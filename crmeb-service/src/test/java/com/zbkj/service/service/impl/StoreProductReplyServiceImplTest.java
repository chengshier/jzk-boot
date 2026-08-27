package com.zbkj.service.service.impl;

import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.product.StoreProductReply;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.service.dao.StoreProductReplyDao;
import com.zbkj.service.service.StoreOrderService;
import com.zbkj.service.service.jiuzhoukang.commission.RetailOrderCommissionAdapter;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class StoreProductReplyServiceImplTest {

    @Test
    public void triggersRetailCommissionWhenTheLastOrderItemIsReviewed() {
        StoreProductReplyServiceImpl service = new StoreProductReplyServiceImpl();
        AtomicBoolean triggered = new AtomicBoolean(false);
        ReflectionTestUtils.setField(service, "dao", proxy(StoreProductReplyDao.class, 1));
        ReflectionTestUtils.setField(service, "storeOrderService", proxy(StoreOrderService.class, true));
        ReflectionTestUtils.setField(service, "redisUtil", new RedisUtil(null, null) {
            @Override
            public boolean lPush(String key, Object value) {
                return true;
            }
        });
        ReflectionTestUtils.setField(service, "retailOrderCommissionAdapter", new RecordingAdapter(triggered));

        ReflectionTestUtils.invokeMethod(service, "completeOrder", new StoreProductReply(), 1,
                new StoreOrder().setId(100).setOrderId("ORDER-100"));

        assertTrue(triggered.get());
    }

    private <T> T proxy(Class<T> type, Object result) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) return type.getSimpleName() + " proxy";
                if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                if ("equals".equals(method.getName())) return proxy == args[0];
            }
            return result;
        }));
    }

    private static class RecordingAdapter extends RetailOrderCommissionAdapter {
        private final AtomicBoolean triggered;

        private RecordingAdapter(AtomicBoolean triggered) { this.triggered = triggered; }

        @Override
        public void afterCrmebOrderCompleted(StoreOrder order) { triggered.set(true); }
    }
}
