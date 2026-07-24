package com.zbkj.service.service.impl.jiuzhoukang.health.provider;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.*;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkHealthProvider;
import com.zbkj.common.request.jiuzhoukang.JkHealthDeviceCallbackRequest;
import com.zbkj.service.service.jiuzhoukang.health.provider.HealthProviderAdapter;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 可配置 REST/JSON 双模式适配器。
 * <p>pull 与 callback 共用字段映射规则，常规 JSON 厂商只需后台配置 URL、认证、JSONPath 和签名方式，
 * 无需修改健康数据核心代码。</p>
 */
@Component
public class GenericRestHealthProviderAdapter implements HealthProviderAdapter {
    @Override public String adapterType() { return "GENERIC_REST"; }

    @Override
    public List<JkHealthDeviceCallbackRequest> pull(JkHealthProvider provider, String credentialJson, String configJson, int limit) {
        JSONObject config = parse(configJson);
        JSONObject credential = parse(credentialJson);
        String url = config.getString("pullUrl");
        if (StrUtil.isBlank(url)) {
            String path = config.getString("pullPath");
            if (StrUtil.isBlank(provider.getBaseUrl()) || StrUtil.isBlank(path)) throw new CrmebException("厂商主动拉取地址未配置");
            url = provider.getBaseUrl().replaceAll("/$", "") + "/" + path.replaceAll("^/", "");
        }
        HttpRequest request = "POST".equalsIgnoreCase(config.getString("method")) ? HttpRequest.post(url) : HttpRequest.get(url);
        request.timeout(config.getIntValue("timeoutMillis") > 0 ? config.getIntValue("timeoutMillis") : 15000);
        applyHeaders(request, config.getJSONObject("headers"));
        applyAuth(request, provider.getAuthType(), credential, config);
        JSONObject params = config.getJSONObject("params");
        if (params != null) for (Map.Entry<String,Object> e : params.entrySet()) request.form(e.getKey(), String.valueOf(e.getValue()));
        String cursorParam = config.getString("cursorParam");
        if (StrUtil.isNotBlank(cursorParam) && StrUtil.isNotBlank(provider.getPullCursor())) request.form(cursorParam, provider.getPullCursor());
        String limitParam = config.getString("limitParam");
        if (StrUtil.isNotBlank(limitParam)) request.form(limitParam, Math.max(1, Math.min(limit, 500)));
        HttpResponse response = request.execute();
        if (!response.isOk()) throw new CrmebException("厂商接口响应失败，HTTP=" + response.getStatus());
        Object body = parseBody(response.body());
        assertBusinessSuccess(body, config);
        List<JkHealthDeviceCallbackRequest> result = convertRows(provider.getProviderCode(), body, config, "dataPath", "fieldMapping", Math.max(1, Math.min(limit, 500)));
        String nextCursorPath = config.getString("nextCursorPath");
        if (StrUtil.isNotBlank(nextCursorPath)) {
            Object next = JSONPath.eval(body, nextCursorPath);
            provider.setPullCursor(next == null ? null : String.valueOf(next));
        }
        return result;
    }

    @Override
    public List<JkHealthDeviceCallbackRequest> parseCallback(JkHealthProvider provider, String credentialJson, String configJson,
                                                              String rawBody, Map<String,String> headers) {
        JSONObject config = parse(configJson);
        JSONObject credential = parse(credentialJson);
        verifyCallback(rawBody, headers, credential, config);
        Object body = parseBody(rawBody);
        assertBusinessSuccess(body, config.getJSONObject("callbackBusiness") == null ? config : config.getJSONObject("callbackBusiness"));
        int limit = config.getIntValue("callbackBatchLimit");
        if (limit <= 0) limit = 500;
        List<JkHealthDeviceCallbackRequest> rows = convertRows(provider.getProviderCode(), body, config,
                "callbackDataPath", "callbackFieldMapping", Math.max(1, Math.min(limit, 1000)));
        String timestamp = firstNonBlank(header(headers, StrUtil.blankToDefault(config.getString("callbackTimestampHeader"), "X-Timestamp")),
                valueAt(body, config.getString("callbackTimestampPath")), String.valueOf(System.currentTimeMillis()));
        for (JkHealthDeviceCallbackRequest row : rows) row.setTimestamp(timestamp).setSign("CALLBACK_NORMALIZED");
        return rows;
    }

    private List<JkHealthDeviceCallbackRequest> convertRows(String providerCode, Object body, JSONObject config,
                                                             String dataPathKey, String mappingKey, int limit) {
        String dataPath = config.getString(dataPathKey);
        if (StrUtil.isBlank(dataPath) && dataPathKey.startsWith("callback")) dataPath = config.getString("dataPath");
        Object data = StrUtil.isBlank(dataPath) ? body : JSONPath.eval(body, dataPath);
        List<?> source = toList(data);
        JSONObject mapping = config.getJSONObject(mappingKey);
        if (mapping == null && mappingKey.startsWith("callback")) mapping = config.getJSONObject("fieldMapping");
        if (mapping == null) throw new CrmebException("厂商字段映射未配置: " + mappingKey);
        List<JkHealthDeviceCallbackRequest> result = new ArrayList<JkHealthDeviceCallbackRequest>();
        for (Object row : source) {
            if (result.size() >= limit) break;
            result.add(convert(providerCode, row, mapping, config));
        }
        return result;
    }

    private void verifyCallback(String rawBody, Map<String,String> headers, JSONObject credential, JSONObject config) {
        String type = StrUtil.blankToDefault(config.getString("callbackSignatureType"), "HMAC_SHA256").toUpperCase();
        if ("NONE".equals(type)) {
            if (!config.getBooleanValue("allowUnsignedCallback")) throw new CrmebException("无签名回调必须显式配置 allowUnsignedCallback=true");
            return;
        }
        if (!"HMAC_SHA256".equals(type)) throw new CrmebException("通用适配器不支持的回调签名类型: " + type);
        String secret = credential.getString("callbackSecret");
        if (StrUtil.isBlank(secret)) throw new CrmebException("厂商 callbackSecret 未配置");
        String signHeader = StrUtil.blankToDefault(config.getString("callbackSignHeader"), "X-Signature");
        String actual = header(headers, signHeader);
        if (StrUtil.isBlank(actual)) throw new CrmebException("厂商回调缺少签名头: " + signHeader);
        String timestampHeader = StrUtil.blankToDefault(config.getString("callbackTimestampHeader"), "X-Timestamp");
        String timestamp = header(headers, timestampHeader);
        String contentMode = StrUtil.blankToDefault(config.getString("callbackSignContent"), "TIMESTAMP_BODY").toUpperCase();
        String raw;
        if ("BODY".equals(contentMode)) raw = rawBody;
        else {
            if (StrUtil.isBlank(timestamp)) throw new CrmebException("厂商回调缺少时间戳头: " + timestampHeader);
            verifyTimestamp(timestamp, config.getIntValue("callbackMaxSkewSeconds"));
            raw = timestamp + "." + rawBody;
        }
        String expected = hmac(raw, secret);
        if (!constantEquals(expected, actual.trim())) throw new CrmebException("厂商回调签名无效");
    }

    private void verifyTimestamp(String value, int maxSkewSeconds) {
        long timestamp;
        try { timestamp = Long.parseLong(value); } catch (Exception e) { throw new CrmebException("厂商回调时间戳无效"); }
        if (value.length() <= 10) timestamp *= 1000L;
        long max = (maxSkewSeconds > 0 ? maxSkewSeconds : 300) * 1000L;
        if (Math.abs(System.currentTimeMillis() - timestamp) > max) throw new CrmebException("厂商回调已过期");
    }

    private void assertBusinessSuccess(Object body, JSONObject config) {
        if (config == null) return;
        String successPath = config.getString("successPath");
        if (StrUtil.isNotBlank(successPath)) {
            Object success = JSONPath.eval(body, successPath);
            String expected = config.getString("successValue");
            if (expected != null && !expected.equals(String.valueOf(success))) throw new CrmebException("厂商接口业务状态失败: " + success);
        }
    }

    private void applyHeaders(HttpRequest request, JSONObject headers) { if (headers != null) for (Map.Entry<String,Object> e : headers.entrySet()) request.header(e.getKey(), String.valueOf(e.getValue())); }
    private void applyAuth(HttpRequest request, String authType, JSONObject credential, JSONObject config) {
        String type = StrUtil.isBlank(authType) ? "NONE" : authType.toUpperCase();
        if ("API_KEY".equals(type)) { String header = StrUtil.blankToDefault(credential.getString("headerName"), "X-API-Key"); request.header(header, credential.getString("apiKey")); }
        else if ("BEARER_STATIC".equals(type)) request.header("Authorization", "Bearer " + credential.getString("token"));
        else if ("OAUTH2_CLIENT_CREDENTIALS".equals(type)) {
            String tokenUrl = StrUtil.blankToDefault(credential.getString("tokenUrl"), config.getString("tokenUrl"));
            HttpResponse tokenResponse = HttpRequest.post(tokenUrl).form("grant_type", "client_credentials")
                    .form("client_id", credential.getString("clientId")).form("client_secret", credential.getString("clientSecret"))
                    .form("scope", credential.getString("scope")).timeout(10000).execute();
            if (!tokenResponse.isOk()) throw new CrmebException("厂商 OAuth2 token 获取失败");
            JSONObject tokenJson = JSON.parseObject(tokenResponse.body());
            String token = tokenJson.getString(StrUtil.blankToDefault(credential.getString("tokenField"), "access_token"));
            if (StrUtil.isBlank(token)) throw new CrmebException("厂商 OAuth2 响应缺少 access_token");
            request.header("Authorization", "Bearer " + token);
        }
    }

    private JkHealthDeviceCallbackRequest convert(String providerCode, Object row, JSONObject mapping, JSONObject config) {
        String deviceSn = text(row, mapping.getString("deviceSn"));
        String externalNo = text(row, mapping.getString("externalNo"));
        String valueText = text(row, mapping.getString("value"));
        String measuredText = text(row, mapping.getString("measuredAt"));
        if (StrUtil.hasBlank(deviceSn, externalNo, valueText, measuredText)) throw new CrmebException("厂商数据缺少设备号、外部号、数值或测量时间");
        return new JkHealthDeviceCallbackRequest().setProviderCode(providerCode).setDeviceSn(deviceSn).setExternalNo(externalNo)
                .setMeasuredAt(parseDate(measuredText, config.getString("timeFormat"))).setValue(new BigDecimal(valueText))
                .setUnit(text(row, mapping.getString("unit"))).setPeriod(text(row, mapping.getString("period")))
                .setTimestamp(String.valueOf(System.currentTimeMillis())).setSign("PROVIDER_NORMALIZED");
    }

    private Object parseBody(String value) { try { return JSON.parse(value); } catch (Exception e) { throw new CrmebException("厂商接口返回不是有效 JSON"); } }
    private JSONObject parse(String value) { if (StrUtil.isBlank(value)) return new JSONObject(); try { return JSON.parseObject(value); } catch (Exception e) { throw new CrmebException("厂商接入配置不是有效 JSON"); } }
    private List<?> toList(Object data) { if (data == null) return Collections.emptyList(); return data instanceof List ? (List<?>) data : Collections.singletonList(data); }
    private String text(Object row, String path) { if (StrUtil.isBlank(path)) return null; Object value=JSONPath.eval(row,path);return value==null?null:String.valueOf(value); }
    private String valueAt(Object body,String path){if(StrUtil.isBlank(path))return null;Object v=JSONPath.eval(body,path);return v==null?null:String.valueOf(v);}
    private Date parseDate(String value, String format) { try { if(value.matches("^\\d{13}$"))return new Date(Long.parseLong(value));if(value.matches("^\\d{10}$"))return new Date(Long.parseLong(value)*1000L);return StrUtil.isBlank(format)?DateUtil.parse(value):DateUtil.parse(value,format); } catch(Exception e){throw new CrmebException("厂商测量时间格式无法解析: "+value);} }
    private String header(Map<String,String> headers,String name){if(headers==null||name==null)return null;for(Map.Entry<String,String> e:headers.entrySet())if(name.equalsIgnoreCase(e.getKey()))return e.getValue();return null;}
    private String firstNonBlank(String... values){for(String v:values)if(StrUtil.isNotBlank(v))return v;return null;}
    private String hmac(String text,String key){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));byte[] out=mac.doFinal(text.getBytes(StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();for(byte b:out)sb.append(String.format("%02x",b&0xff));return sb.toString();}catch(Exception e){throw new CrmebException("厂商回调验签失败");}}
    private boolean constantEquals(String a,String b){if(a==null||b==null||a.length()!=b.length())return false;int diff=0;for(int i=0;i<a.length();i++)diff|=a.charAt(i)^b.charAt(i);return diff==0;}
}
