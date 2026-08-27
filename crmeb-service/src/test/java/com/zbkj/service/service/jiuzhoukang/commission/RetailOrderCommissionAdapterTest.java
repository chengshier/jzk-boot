package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.service.service.jiuzhoukang.order.RetailOrderAttributionService;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class RetailOrderCommissionAdapterTest {

    @Test
    public void locksAttributionEvenWhenNoCommissionReceiverCanBeTriggered() {
        AtomicBoolean locked = new AtomicBoolean(false);
        RetailOrderCommissionAdapter adapter = new RetailOrderCommissionAdapter();
        ReflectionTestUtils.setField(adapter, "attributionService", proxy(RetailOrderAttributionService.class, (method, args) -> {
            if ("lockByOrder".equals(method.getName())) locked.set(true);
            if ("listByOrder".equals(method.getName())) return Collections.emptyList();
            return null;
        }));

        adapter.afterCrmebOrderCompleted(new StoreOrder().setId(100).setOrderId("ORDER-100"));

        assertTrue(locked.get());
    }

    private <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return null;
            return invocation.apply(method, args);
        }));
    }

    private interface Invocation {
        Object apply(Method method, Object[] args) throws Throwable;
    }
}
