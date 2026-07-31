package com.zbkj.service.service.impl.jiuzhoukang.storage;

import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.service.service.SystemConfigService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Map;

public class JkFileStorageServiceImplMinioConfigTest {

    @Test
    public void reportsMinioReadyFromSharedSystemConfig() {
        JkFileStorageServiceImpl service = new JkFileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "provider", "MINIO");
        ReflectionTestUtils.setField(service, "systemConfigService", configService());

        Map<String, Object> status = service.status();

        Assert.assertEquals(Boolean.TRUE, status.get("minioConfigured"));
        Assert.assertEquals(Boolean.TRUE, status.get("ready"));
    }

    private SystemConfigService configService() {
        return (SystemConfigService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SystemConfigService.class},
                (proxy, method, args) -> {
                    if (!"getValueByKey".equals(method.getName())) return null;
                    String key = (String) args[0];
                    if (SysConfigConstants.CONFIG_MINIO_REGION.equals(key)) return "";
                    return "configured";
                });
    }
}
