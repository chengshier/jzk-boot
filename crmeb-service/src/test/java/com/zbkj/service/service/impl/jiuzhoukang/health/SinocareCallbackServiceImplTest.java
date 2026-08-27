package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.zbkj.common.model.jiuzhoukang.JkSinocareCallbackLog;
import com.zbkj.service.dao.jiuzhoukang.JkSinocareCallbackLogDao;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SinocareCallbackServiceImplTest {
    @Test
    public void retryRejectsCallbackThatIsNotFailed() throws Exception {
        SinocareCallbackServiceImpl service = new SinocareCallbackServiceImpl();
        ReflectionTestUtils.setField(service, "callbackLogDao", daoWithStatus("SUCCESS"));

        Method method;
        try {
            method = SinocareCallbackServiceImpl.class.getMethod("retry", Long.class);
        } catch (NoSuchMethodException e) {
            Assert.fail("retry should be available for failed Sinocare callbacks");
            return;
        }
        try {
            method.invoke(service, 10L);
            Assert.fail("only FAILED callbacks may be retried");
        } catch (InvocationTargetException e) {
            Assert.assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    private JkSinocareCallbackLogDao daoWithStatus(String status) {
        return (JkSinocareCallbackLogDao) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{JkSinocareCallbackLogDao.class},
                (proxy, method, args) -> {
                    if ("selectById".equals(method.getName())) return new JkSinocareCallbackLog().setId((Long) args[0]).setProcessStatus(status);
                    if ("toString".equals(method.getName())) return "callbackLogDao";
                    return 1;
                });
    }
}
