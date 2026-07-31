package com.zbkj.service.service.impl;

import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.service.service.SystemConfigService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;

public class SystemAttachmentServiceImplMinioTest {

    @Test
    public void selectsConfiguredPublicUrlForMinioProvider() {
        SystemAttachmentServiceImpl service = new SystemAttachmentServiceImpl();
        ReflectionTestUtils.setField(service, "systemConfigService", configService());

        Assert.assertEquals("https://cdn.example.com", service.getCdnUrl());
    }

    private SystemConfigService configService() {
        return (SystemConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SystemConfigService.class},
                (proxy, method, args) -> {
                    if ("getValueByKeyException".equals(method.getName())) return "6";
                    if ("getValueByKey".equals(method.getName()) && SysConfigConstants.CONFIG_MINIO_UPLOAD_URL.equals(args[0])) {
                        return "https://cdn.example.com";
                    }
                    return null;
                });
    }
}
