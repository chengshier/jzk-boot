package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturn;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturnItem;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessEventDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferReturnDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferReturnItemDao;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class CommissionTriggerServiceImplTransferReturnTest {

    @Test
    public void transferReturnReversesPerformanceAndProfitByOriginalItemQuantityRatio() {
        CommissionTriggerServiceImpl service = new CommissionTriggerServiceImpl();
        JkStockTransferReturn returnOrder = new JkStockTransferReturn()
                .setId(500L).setReturnNo("SR500").setOriginalTransferId(200L).setOriginalTransferNo("ST200")
                .setReturnAmount(new BigDecimal("200.00")).setStatus("COMPLETED").setIsDeleted(false);
        JkStockTransfer transfer = new JkStockTransfer()
                .setId(200L).setTransferNo("ST200").setTotalAmount(new BigDecimal("1000.00")).setIsDeleted(false);
        JkStockTransferReturnItem returnItem = new JkStockTransferReturnItem()
                .setId(501L).setReturnId(500L).setOriginalTransferItemId(300L).setReturnQuantity(2).setIsDeleted(false);
        JkStockTransferItem originalItem = new JkStockTransferItem()
                .setId(300L).setTransferId(200L).setQuantity(10).setIsDeleted(false);

        ReflectionTestUtils.setField(service, "transferReturnDao", proxy(JkStockTransferReturnDao.class, (method, args) ->
                "selectById".equals(method.getName()) ? returnOrder : null));
        ReflectionTestUtils.setField(service, "stockTransferDao", proxy(JkStockTransferDao.class, (method, args) ->
                "selectById".equals(method.getName()) ? transfer : null));
        ReflectionTestUtils.setField(service, "transferReturnItemDao", proxy(JkStockTransferReturnItemDao.class, (method, args) ->
                "selectList".equals(method.getName()) ? Collections.singletonList(returnItem) : null));
        ReflectionTestUtils.setField(service, "stockTransferItemDao", proxy(JkStockTransferItemDao.class, (method, args) ->
                "selectById".equals(method.getName()) ? originalItem : null));
        ReflectionTestUtils.setField(service, "recordDao", proxy(JkCommissionRecordDao.class, (method, args) ->
                "selectList".equals(method.getName()) ? Collections.emptyList() : null));
        ReflectionTestUtils.setField(service, "businessEventDao", proxy(JkBusinessEventDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return null;
            if ("insert".equals(method.getName())) return 1;
            return null;
        }));

        AtomicReference<BigDecimal> performanceRatio = new AtomicReference<BigDecimal>();
        AtomicReference<Long> performanceItemId = new AtomicReference<Long>();
        ReflectionTestUtils.setField(service, "performanceService", proxy(JkPerformanceService.class, (method, args) -> {
            if ("reverseByRatio".equals(method.getName())) {
                performanceItemId.set((Long) args[2]);
                performanceRatio.set((BigDecimal) args[3]);
                return new BigDecimal("40.00");
            }
            return null;
        }));

        AtomicReference<BigDecimal> profitRatio = new AtomicReference<BigDecimal>();
        AtomicReference<Long> profitItemId = new AtomicReference<Long>();
        ReflectionTestUtils.setField(service, "profitService", proxy(JkOperationProfitService.class, (method, args) -> {
            if ("reverseByRatio".equals(method.getName())) {
                profitItemId.set((Long) args[2]);
                profitRatio.set((BigDecimal) args[3]);
                return new BigDecimal("10.00");
            }
            return null;
        }));

        service.onTransferReturnCompleted(500L, "SR500", "STOCK_TRANSFER_RETURN_COMPLETED:500");

        Assert.assertEquals(Long.valueOf(300L), performanceItemId.get());
        Assert.assertEquals(0, new BigDecimal("0.20000000").compareTo(performanceRatio.get()));
        Assert.assertEquals(Long.valueOf(300L), profitItemId.get());
        Assert.assertEquals(0, new BigDecimal("0.20000000").compareTo(profitRatio.get()));
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
