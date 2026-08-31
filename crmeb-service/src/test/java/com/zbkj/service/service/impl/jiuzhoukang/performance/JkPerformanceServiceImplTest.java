package com.zbkj.service.service.impl.jiuzhoukang.performance;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class JkPerformanceServiceImplTest {

    @Test
    public void summarySubtractsReversedAmountOnlyOnce() {
        JkPerformanceServiceImpl service = new JkPerformanceServiceImpl();
        AtomicReference<String> sqlSegment = new AtomicReference<String>();
        JkPerformanceRecordDao dao = proxy(JkPerformanceRecordDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                Wrapper<?> wrapper = (Wrapper<?>) args[0];
                sqlSegment.set(wrapper.getSqlSegment());
                return Collections.singletonList(new JkPerformanceRecord()
                        .setOwnerUserId(100L)
                        .setPerformanceType("RETAIL_ONLINE")
                        .setPerformanceAmount(new BigDecimal("100.00"))
                        .setReversedAmount(new BigDecimal("30.00"))
                        .setStatus("PARTIALLY_REVERSED")
                        .setIsDeleted(false));
            }
            return null;
        });
        ReflectionTestUtils.setField(service, "recordDao", dao);

        BigDecimal result = service.summary(100L, null);

        Assert.assertEquals(new BigDecimal("70.00"), result);
        Assert.assertNotNull(sqlSegment.get());
        Assert.assertTrue("汇总查询必须排除负数冲正审计行", sqlSegment.get().toUpperCase().contains("NOT LIKE"));
    }

    @Test
    public void legacyLedgerDelegatesToCanonicalServiceWithRequestNoAsActionKey() {
        JkPerformanceLedgerService legacy = new JkPerformanceLedgerService();
        AtomicReference<JkPerformanceRecord> captured = new AtomicReference<JkPerformanceRecord>();
        JkPerformanceService canonical = proxy(JkPerformanceService.class, (method, args) -> {
            if ("record".equals(method.getName())) {
                JkPerformanceRecord value = (JkPerformanceRecord) args[0];
                captured.set(value);
                return value;
            }
            if ("summary".equals(method.getName())) {
                return new BigDecimal("70.00");
            }
            return null;
        });
        ReflectionTestUtils.setField(legacy, "performanceService", canonical);

        JkPerformanceRecord value = new JkPerformanceRecord()
                .setOwnerUserId(100L)
                .setSourceType("PLATFORM_ORDER")
                .setSourceId(200L)
                .setPerformanceType("PLATFORM_PURCHASE")
                .setPerformanceAmount(new BigDecimal("100.00"))
                .setRequestNo("PERFORMANCE:TEST:200");

        legacy.record(value);

        Assert.assertNotNull(captured.get());
        Assert.assertEquals("PERFORMANCE:TEST:200", captured.get().getActionKey());
        Assert.assertEquals(new BigDecimal("70.00"), legacy.validAmount(100L));
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
