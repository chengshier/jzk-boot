package com.zbkj.service.service.impl.jiuzhoukang.health;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class JkHealthAdminServiceImplSinocareStatusTest {
    @Test
    public void exposesOnlySinocareConfigurationReadiness() throws Exception {
        JkHealthAdminServiceImpl service = new JkHealthAdminServiceImpl();
        ReflectionTestUtils.setField(service, "sinocareAppId", "app-id");
        ReflectionTestUtils.setField(service, "sinocareAuthorizationH5Url", "https://sinocare.example.com/auth");
        ReflectionTestUtils.setField(service, "sinocarePublicKey", "public-key");

        Object result = service.integrationStatus();
        Assert.assertTrue((Boolean) result.getClass().getMethod("getSinocareAppIdConfigured").invoke(result));
        Assert.assertTrue((Boolean) result.getClass().getMethod("getSinocareAuthorizationH5UrlConfigured").invoke(result));
        Assert.assertTrue((Boolean) result.getClass().getMethod("getSinocarePublicKeyConfigured").invoke(result));
        Assert.assertFalse(result.toString().contains("app-id"));
        Assert.assertFalse(result.toString().contains("public-key"));
    }
}
