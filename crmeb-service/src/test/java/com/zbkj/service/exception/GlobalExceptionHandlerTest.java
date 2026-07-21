package com.zbkj.service.exception;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.result.CommonResultCode;
import com.zbkj.service.service.ExceptionLogService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GlobalExceptionHandlerTest {

    @Test
    public void shouldKeepCrmebExceptionCodeWhenHandlingForbidden() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "exceptionLogService", proxy(ExceptionLogService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/jk/stock/my");

        CommonResult<?> result = handler.defaultExceptionHandler(
                request,
                new CrmebException(CommonResultCode.FORBIDDEN, "无业务权限")
        );

        Assert.assertEquals(403L, result.getCode());
        Assert.assertEquals("无业务权限", result.getMessage());
    }

    private <T> T proxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return defaultObjectMethod(type, proxy, method, args);
            }
            return defaultValue(method.getReturnType());
        }));
    }

    private Object defaultObjectMethod(Class<?> type, Object proxy, Method method, Object[] args) {
        if ("toString".equals(method.getName())) {
            return type.getSimpleName() + "Proxy";
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(method.getName())) {
            return proxy == args[0];
        }
        return null;
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        return null;
    }
}
