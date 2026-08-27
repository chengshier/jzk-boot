package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.zbkj.service.dao.jiuzhoukang.JkSinocareAuthorizationDao;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SinocareAuthorizationServiceImplTest {

    @Test
    public void buildsAuthorizationUrlWithPersistedUniqueIdAndEncodedRedirectUrl() throws Exception {
        SinocareAuthorizationServiceImpl service = new SinocareAuthorizationServiceImpl();
        ReflectionTestUtils.setField(service, "dao", dao());
        ReflectionTestUtils.setField(service, "appId", "test-app-id");
        ReflectionTestUtils.setField(service, "authorizationH5Url", "https://sinocare.example.com/mobile/auth");
        ReflectionTestUtils.setField(service, "redirectUrl", "https://jzkapp.wit.cn/pages/health/index?a=1&b=2");

        Method method;
        try {
            method = SinocareAuthorizationServiceImpl.class.getMethod("buildAuthorizationUrl", Long.class);
        } catch (NoSuchMethodException e) {
            Assert.fail("buildAuthorizationUrl should be available for the front authorization endpoint");
            return;
        }
        Object response = method.invoke(service, 9L);
        String uniqueId = (String) response.getClass().getMethod("getUniqueId").invoke(response);
        String authorizationUrl = (String) response.getClass().getMethod("getAuthorizationUrl").invoke(response);

        Assert.assertEquals(32, uniqueId.length());
        Assert.assertEquals("https://sinocare.example.com/mobile/auth?appId=test-app-id&uniqueId=" + uniqueId
                + "&redirectUrl=https%3A%2F%2Fjzkapp.wit.cn%2Fpages%2Fhealth%2Findex%3Fa%3D1%26b%3D2", authorizationUrl);
    }

    private JkSinocareAuthorizationDao dao() {
        return (JkSinocareAuthorizationDao) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{JkSinocareAuthorizationDao.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) return null;
                    if ("insert".equals(method.getName())) return 1;
                    return null;
                });
    }
}
