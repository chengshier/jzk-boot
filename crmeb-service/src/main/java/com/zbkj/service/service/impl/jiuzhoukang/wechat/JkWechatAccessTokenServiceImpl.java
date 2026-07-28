package com.zbkj.service.service.impl.jiuzhoukang.wechat;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.service.service.jiuzhoukang.wechat.JkWechatAccessTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JkWechatAccessTokenServiceImpl implements JkWechatAccessTokenService {
    @Value("${jk.wechat.enabled:false}") private boolean enabled;
    @Value("${jk.wechat.appid:}") private String appid;
    @Value("${jk.wechat.secret:}") private String secret;
    private volatile String cachedToken;
    private volatile long expireAt;

    @Override
    public String token() {
        if (!enabled) throw new CrmebException("微信能力总开关尚未启用");
        if (blank(appid) || blank(secret)) throw new CrmebException("微信 appid 或 secret 未配置");
        long now = System.currentTimeMillis();
        if (!blank(cachedToken) && expireAt > now + 120000L) return cachedToken;
        synchronized (this) {
            now = System.currentTimeMillis();
            if (!blank(cachedToken) && expireAt > now + 120000L) return cachedToken;
            try {
                String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                        + URLEncoder.encode(appid, "UTF-8") + "&secret=" + URLEncoder.encode(secret, "UTF-8");
                HttpResponse response = HttpRequest.get(url).timeout(10000).execute();
                JSONObject json = JSONUtil.parseObj(response.body());
                String token = json.getStr("access_token");
                if (blank(token)) throw new CrmebException("获取微信 access_token 失败：" + safe(json.getStr("errmsg")));
                int expires = json.getInt("expires_in", 7200);
                cachedToken = token;
                expireAt = now + Math.max(300, expires) * 1000L;
                return token;
            } catch (CrmebException e) {
                throw e;
            } catch (Exception e) {
                throw new CrmebException("获取微信 access_token 失败：" + safe(e.getMessage()));
            }
        }
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("enabled", enabled);
        result.put("appidConfigured", !blank(appid));
        result.put("secretConfigured", !blank(secret));
        result.put("ready", enabled && !blank(appid) && !blank(secret));
        result.put("tokenCached", !blank(cachedToken) && expireAt > System.currentTimeMillis());
        return result;
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String safe(String value) { return value == null ? "未知错误" : value.replace('\r', ' ').replace('\n', ' '); }
}
