package com.zbkj.admin.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.config.CrmebConfig;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.utils.SpringUtil;
import com.zbkj.service.service.SystemAttachmentService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.impl.SystemAttachmentServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Collections;

public class ResponseRouterMinioTest {

    @Test
    public void routesPrefixedMinioObjectInsideJsonWithoutCorruptingPayload() {
        installAttachmentService("6", "https://cdn.example.com/");

        String routed = new ResponseRouter().filter("{\"data\":{\"image\":\"image-assets/crmebimage/public/product/photo.jpg\"}}",
                "api/admin/product/list", config());

        JSONObject parsed = JSON.parseObject(routed);
        Assert.assertEquals("https://cdn.example.com/image-assets/crmebimage/public/product/photo.jpg",
                parsed.getJSONObject("data").getString("image"));
    }

    @Test
    public void keepsNonMinioImageRoutingContract() {
        installAttachmentService("1", "https://local.example.com");

        String routed = new ResponseRouter().filter("{\"data\":{\"image\":\"crmebimage/public/product/photo.jpg\"}}",
                "api/admin/product/list", config());

        Assert.assertEquals("https://local.example.com/crmebimage/public/product/photo.jpg",
                JSON.parseObject(routed).getJSONObject("data").getString("image"));
    }

    @Test
    public void rewritesNestedMinioKeysWithoutTouchingEscapedQuoteText() {
        installAttachmentService("6", "https://cdn.example.com");
        String payload = "{\"data\":{\"items\":[{\"image\":\"image-assets/crmebimage/public/a.jpg\"}],"
                + "\"note\":\"she said \\\"crmebimage/public/a.jpg\\\"\"}}";

        JSONObject data = JSON.parseObject(new ResponseRouter().filter(payload, "api/admin/product/list", config())).getJSONObject("data");

        Assert.assertEquals("https://cdn.example.com/image-assets/crmebimage/public/a.jpg",
                data.getJSONArray("items").getJSONObject(0).getString("image"));
        Assert.assertEquals("she said \"crmebimage/public/a.jpg\"", data.getString("note"));
    }

    private void installAttachmentService(String uploadType, String cdn) {
        SystemAttachmentServiceImpl attachmentService = new SystemAttachmentServiceImpl();
        ReflectionTestUtils.setField(attachmentService, "systemConfigService", configService(uploadType, cdn));
        ApplicationContext context = (ApplicationContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ApplicationContext.class},
                (proxy, method, args) -> "getBean".equals(method.getName()) ? attachmentService : null);
        ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", context);
    }

    private SystemConfigService configService(String uploadType, String cdn) {
        return (SystemConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SystemConfigService.class},
                (proxy, method, args) -> {
                    if (!"getValueByKey".equals(method.getName()) && !"getValueByKeyException".equals(method.getName())) return null;
                    if (SysConfigConstants.CONFIG_UPLOAD_TYPE.equals(args[0])) return uploadType;
                    return cdn;
                });
    }

    private CrmebConfig config() {
        CrmebConfig config = new CrmebConfig();
        config.setIgnored(Collections.<String>emptyList());
        return config;
    }
}
