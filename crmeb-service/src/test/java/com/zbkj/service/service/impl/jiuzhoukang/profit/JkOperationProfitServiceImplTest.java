package com.zbkj.service.service.impl.jiuzhoukang.profit;

import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.service.dao.jiuzhoukang.JkOperationProfitRecordDao;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class JkOperationProfitServiceImplTest {

    @Test
    public void summarySubtractsReversalOnlyOnce() {
        JkOperationProfitServiceImpl service = new JkOperationProfitServiceImpl();
        JkOperationProfitRecordDao dao = proxy(JkOperationProfitRecordDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Arrays.asList(
                        new JkOperationProfitRecord()
                                .setUserId(100L)
                                .setProfitAmount(new BigDecimal("100.00"))
                                .setReversedAmount(new BigDecimal("30.00"))
                                .setStatus("PARTIALLY_REVERSED")
                                .setIsDeleted(false),
                        new JkOperationProfitRecord()
                                .setUserId(100L)
                                .setProfitAmount(new BigDecimal("-30.00"))
                                .setReversedAmount(BigDecimal.ZERO)
                                .setStatus("REVERSAL")
                                .setIsDeleted(false)
                );
            }
            return null;
        });
        ReflectionTestUtils.setField(service, "recordDao", dao);

        Assert.assertEquals(new BigDecimal("70.00"), service.summary(100L));
    }

    @Test
    public void reverseByRatioUsesOriginalProfitAcrossRepeatedPartialReturns() {
        Assert.assertEquals(new BigDecimal("20.00"), reverseRatioTarget(new BigDecimal("20.00")));
        Assert.assertEquals(new BigDecimal("20.00"), reverseRatioTarget(new BigDecimal("40.00")));
    }

    @Test
    public void missingCostDoesNotTurnRevenueIntoProfit() {
        JkOperationProfitServiceImpl service = new JkOperationProfitServiceImpl();
        JkOperationProfitRecordDao dao = proxy(JkOperationProfitRecordDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return null;
            if ("insert".equals(method.getName())) return 1;
            return null;
        });
        ReflectionTestUtils.setField(service, "recordDao", dao);

        JkOperationProfitRecord result = service.record(new JkOperationProfitRecord()
                .setUserId(100L)
                .setSourceType("STOCK_TRANSFER")
                .setSourceId(200L)
                .setRevenueAmount(new BigDecimal("100.00"))
                .setActionKey("PROFIT:TEST:200"));

        Assert.assertEquals(new BigDecimal("100.00"), result.getRevenueAmount());
        Assert.assertEquals(BigDecimal.ZERO, result.getCostAmount());
        Assert.assertEquals(BigDecimal.ZERO, result.getProfitAmount());
    }

    @Test
    public void canonicalRecordUsesActionKeyAsRequestNoAndIsIdempotent() {
        JkOperationProfitServiceImpl service = new JkOperationProfitServiceImpl();
        AtomicReference<JkOperationProfitRecord> stored = new AtomicReference<JkOperationProfitRecord>();
        AtomicInteger inserts = new AtomicInteger();
        JkOperationProfitRecordDao dao = proxy(JkOperationProfitRecordDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return stored.get();
            if ("insert".equals(method.getName())) {
                inserts.incrementAndGet();
                stored.set((JkOperationProfitRecord) args[0]);
                return 1;
            }
            return null;
        });
        ReflectionTestUtils.setField(service, "recordDao", dao);

        JkOperationProfitRecord first = service.record(new JkOperationProfitRecord()
                .setUserId(100L)
                .setSourceType("STOCK_TRANSFER")
                .setSourceId(200L)
                .setRevenueAmount(new BigDecimal("100.00"))
                .setCostAmount(new BigDecimal("30.00"))
                .setProfitAmount(new BigDecimal("70.00"))
                .setActionKey("PROFIT:TEST:200"));
        JkOperationProfitRecord second = service.record(new JkOperationProfitRecord()
                .setUserId(100L)
                .setSourceType("STOCK_TRANSFER")
                .setSourceId(200L)
                .setRevenueAmount(new BigDecimal("999.00"))
                .setCostAmount(BigDecimal.ZERO)
                .setProfitAmount(new BigDecimal("999.00"))
                .setActionKey("PROFIT:TEST:200"));

        Assert.assertEquals("PROFIT:TEST:200", first.getRequestNo());
        Assert.assertSame(first, second);
        Assert.assertEquals(1, inserts.get());
        Assert.assertEquals(new BigDecimal("70.00"), second.getProfitAmount());
    }

    private BigDecimal reverseRatioTarget(BigDecimal alreadyReversed) {
        JkOperationProfitServiceImpl service = new JkOperationProfitServiceImpl();
        JkOperationProfitRecord original = new JkOperationProfitRecord()
                .setId(1L).setUserId(100L).setSourceType("STOCK_TRANSFER").setSourceId(200L).setSourceItemId(300L)
                .setProfitAmount(new BigDecimal("100.00")).setReversedAmount(alreadyReversed)
                .setStatus("PARTIALLY_REVERSED").setIsDeleted(false);
        JkOperationProfitRecordDao dao = proxy(JkOperationProfitRecordDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) return Collections.singletonList(original);
            if ("update".equals(method.getName())) return 1;
            if ("selectOne".equals(method.getName())) return null;
            if ("insert".equals(method.getName())) return 1;
            return null;
        });
        ReflectionTestUtils.setField(service, "recordDao", dao);
        return service.reverseByRatio("STOCK_TRANSFER", 200L, 300L, new BigDecimal("0.20"),
                "RETURN:1:" + alreadyReversed, "部分退回");
    }

    private <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) return type.getSimpleName() + "Proxy";
                if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                if ("equals".equals(method.getName())) return proxy == args[0];
            }
            return invocation.apply(method, args);
        }));
    }

    private interface Invocation {
        Object apply(Method method, Object[] args) throws Throwable;
    }
}
