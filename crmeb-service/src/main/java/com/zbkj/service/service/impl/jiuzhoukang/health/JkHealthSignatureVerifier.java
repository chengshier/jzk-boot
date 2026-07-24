package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.request.jiuzhoukang.JkHealthDeviceCallbackRequest;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 第三方设备回调验签。
 * <p>优先读取厂商级 callbackSecret；未迁移的旧接入可暂时回退全局密钥。</p>
 */
@Component
public class JkHealthSignatureVerifier {
    @Value("${jk.health.callback-secret:}") private String legacySecret;
    @Value("${jk.health.callback-enabled:false}") private boolean enabled;
    @Autowired private JkHealthProviderService providerService;

    public void verify(JkHealthDeviceCallbackRequest request) {
        if (!enabled) throw new CrmebException("健康设备回调尚未启用");
        String secret = providerService.callbackSecret(request.getProviderCode());
        if (secret == null || secret.trim().isEmpty()) secret = legacySecret;
        if (secret == null || secret.trim().isEmpty()) throw new CrmebException("健康设备回调密钥未配置");
        String raw = request.getProviderCode() + "|" + request.getDeviceSn() + "|" + request.getExternalNo()
                + "|" + request.getTimestamp() + "|" + request.getValue().toPlainString();
        String expected = hmac(raw, secret);
        if (!constantEquals(expected, request.getSign())) throw new CrmebException("健康设备回调签名无效");
        long timestamp;
        try { timestamp = Long.parseLong(request.getTimestamp()); } catch (Exception e) { throw new CrmebException("回调时间戳无效"); }
        if (Math.abs(System.currentTimeMillis() - timestamp) > 5 * 60 * 1000L) throw new CrmebException("健康设备回调已过期");
    }
    private String hmac(String text, String key) {
        try { Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));byte[] bytes=mac.doFinal(text.getBytes(StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();for(byte b:bytes)sb.append(String.format("%02x",b&0xff));return sb.toString(); }
        catch(Exception e){throw new CrmebException("健康设备回调验签失败");}
    }
    private boolean constantEquals(String a,String b){if(a==null||b==null||a.length()!=b.length())return false;int diff=0;for(int i=0;i<a.length();i++)diff|=a.charAt(i)^b.charAt(i);return diff==0;}
}
