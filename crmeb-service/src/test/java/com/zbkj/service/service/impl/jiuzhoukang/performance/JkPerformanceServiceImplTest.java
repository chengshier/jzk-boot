package com.zbkj.service.service.impl.jiuzhoukang.performance;

import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
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

public class JkPerformanceServiceImplTest {

    @Test
    public void summarySubtractsReversedAmountOnlyOnce() {
        JkPerformanceServiceImpl service = new JkPerformanceServiceImpl();
        JkPerformanceRecordDao dao = proxy(JkPerformanceRecordDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                /*
                 * 单测故意同时返回原始业绩与负数冲正审计行，不依赖 MyBatis-Plus Lambda 元数据解析。
                 * 生产查询本身会过滤 *_REVERSE；这里仍把审计行喂给 service，是为了锁住第二层防线：
                 * 原记录的 reversedAmount 已经扣过一次，负数审计行绝不能再参与净业绩汇总。
                 */
                return Arrays.asList(
                        new JkPerformanceRecord()
                                .setOwnerUserId(100L)
                                .setPerformanceType("RETAIL_ONLINE")
                                .setPerformanceAmount(new BigDecimal("100.00"))
                                .setReversedAmount(new BigDecimal("30.00"))
                                .setStatus("PARTIALLY_REVERSED")
                                .setIsDeleted(false),
                        new JkPerformanceRecord()
                                .setOwnerUserId(100L)
                                .setPerformanceType("RETAIL_ONLINE_REVERSE")
                                .setPerformanceAmount(new BigDecimal("-30.00"))
                                .setReversedAmount(BigDecimal.ZERO)
                                .setStatus("VALID")
                                .setIsDeleted(false)
                );
            }
            return null;
        });
        ReflectionTestUtils.setField(service, "recordDao", dao);

        BigDecimal result = service.summary(100L, null);

        Assert.assertEquals(new BigDecimal("70.00"), result);
    }

    @Test
    public void reverseByRatioUsesOriginalAmountAcrossRepeatedPartialReturns() {
        Assert.assertEquals(new BigDecimal("20.00"), reverseRatioTarget(new BigDecimal("20.00")));
        Assert.assertEquals(new BigDecimal("20.00"), reverseRatioTarget(new BigDecimal("40.00")));
    }

    @Test
    public void canonicalRecordUsesActionKeyAsRequestNoAndIsIdempotent() {
        JkPerformanceServiceImpl service = new JkPerformanceServiceImpl();
        AtomicReference<JkPerformanceRecord> stored = new AtomicReference<JkPerformanceRecord>();
        AtomicInteger inserts = new AtomicInteger();
        JkPerformanceRecordDao dao = proxy(JkPerformanceRecordDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return stored.get();
            if ("insert".equals(method.getName())) {
                inserts.incrementAndGet();
                stored.set((JkPerformanceRecord) args[0]);
                return 1;
            }
            return null;
        });
        ReflectionTestUtils.setField(service, "recordDao", dao);

        JkPerformanceRecord first = service.record(new JkPerformanceRecord()
                .setOwnerUserId(100L)
                .setSourceType("PLATFORM_ORDER")
                .setSourceId(200L)
                .setPerformanceType("PLATFORM_PURCHASE")
                .setBaseAmount(new BigDecimal("100.00"))
                .setPerformanceAmount(new BigDecimal("100.00"))
                .setActionKey("PERFORMANCE:TEST:200"));
        JkPerformanceRecord second = service.record(new JkPerformanceRecord()
                .setOwnerUserId(100L)
                .setSourceType("PLATFORM_ORDER")
                .setSourceId(200L)
                .setPerformanceType("PLATFORM_PURCHASE")
                .setBaseAmount(new BigDecimal("999.00"))
                .setPerformanceAmount(new BigDecimal("999.00"))
                .setActionKey("PERFORMANCE:TEST:200"));

        Assert.assertEquals("PERFORMANCE:TEST:200", first.getRequestNo());
        Assert.assertSame(first, second);
        Assert.assertEquals(1, inserts.get());
        Assert.assertEquals(new BigDecimal("100.00"), second.getPerformanceAmount());
    }

    private BigDecimal reverseRatioTarget(BigDecimal alreadyReversed) {
        JkPerformanceServiceImpl service = new JkPerformanceServiceImpl();
        JkPerformanceRecord original = new JkPerformanceRecord()
                .setId(1L).setOwnerUserId(100L).setSourceType("STOCK_TRANSFER").setSourceId(200L).setSourceItemId(300L)
                .setPerformanceType("STOCK_TRANSFER").setPerformanceAmount(new BigDecimal("100.00"))
                .setReversedAmount(alreadyReversed).setIsDeleted(false);
        JkPerformanceRecordDao dao = proxy(JkPerformanceRecordDao.class, (method, args) -> {
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
