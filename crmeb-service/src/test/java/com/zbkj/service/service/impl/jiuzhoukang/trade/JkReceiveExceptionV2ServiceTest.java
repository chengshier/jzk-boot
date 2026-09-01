package com.zbkj.service.service.impl.jiuzhoukang.trade;

import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.model.jiuzhoukang.JkStockBatchFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveExceptionItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JkReceiveExceptionV2ServiceTest {

    @Test
    public void transferPartialReceiveUsesActualQtyAndStableCanonicalLedgerKeys() {
        JkReceiveExceptionV2Service service = new JkReceiveExceptionV2Service();

        JkTradeReceiveException exception = new JkTradeReceiveException()
                .setId(900L)
                .setExceptionNo("REX900")
                .setBusinessType("STOCK_TRANSFER")
                .setBusinessId(100L)
                .setResolutionSnapshotJson("{\"normalReceivedQty\":8,\"exceptionQty\":2}")
                .setIsDeleted(false);
        JkTradeReceiveExceptionItem exceptionItem = new JkTradeReceiveExceptionItem()
                .setId(901L)
                .setExceptionId(900L)
                .setBusinessItemId(200L)
                .setExpectedQty(10)
                .setReceivedQty(8)
                .setDamagedQty(0)
                .setShortageQty(2)
                .setIsDeleted(false);
        JkStockTransfer transfer = new JkStockTransfer()
                .setId(100L)
                .setTransferNo("ST100")
                .setUserId(20L)
                .setRoleCode("maker")
                .setCountyAgentId(10L)
                .setRegionCode("4104")
                .setStatus("RECEIVE_EXCEPTION")
                .setIsDeleted(false);
        JkStockTransferItem item = new JkStockTransferItem()
                .setId(200L)
                .setTransferId(100L)
                .setProductId(1)
                .setSkuId(2)
                .setSkuCode("SKU-2")
                .setQuantity(10)
                .setUnitPrice(new BigDecimal("10.00"))
                .setTotalAmount(new BigDecimal("100.00"))
                .setToStockAccountId(30L)
                .setIsDeleted(false);
        JkStockBatchFlow sourceFlow = new JkStockBatchFlow()
                .setId(400L)
                .setBatchId(300L)
                .setBusinessType("STOCK_TRANSFER")
                .setBusinessId(100L)
                .setProductId(1)
                .setSkuId(2)
                .setFlowType("OUTBOUND")
                .setChangeQty(10)
                .setIsDeleted(false);
        JkStockBatch sourceBatch = new JkStockBatch()
                .setId(300L)
                .setUnitCost(new BigDecimal("6.00"))
                .setIsDeleted(false);

        List<JkStockActionRequest> stockActions = new ArrayList<JkStockActionRequest>();
        List<JkPerformanceRecord> performances = new ArrayList<JkPerformanceRecord>();
        List<JkOperationProfitRecord> profits = new ArrayList<JkOperationProfitRecord>();
        List<JkCommissionRuleTrialRequest> commissionScenarios = new ArrayList<JkCommissionRuleTrialRequest>();
        List<String> commissionKeys = new ArrayList<String>();

        JkStockTransferDao transferDao = proxy(JkStockTransferDao.class, (method, args) -> {
            if ("selectById".equals(method.getName())) return transfer;
            if ("update".equals(method.getName())) return 1;
            return defaultValue(method.getReturnType());
        });
        JkStockTransferItemDao transferItemDao = proxy(JkStockTransferItemDao.class, (method, args) -> {
            if ("selectById".equals(method.getName())) return item;
            if ("updateById".equals(method.getName())) return 1;
            return defaultValue(method.getReturnType());
        });
        JkStockBatchFlowDao batchFlowDao = proxy(JkStockBatchFlowDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) return Collections.singletonList(sourceFlow);
            return defaultValue(method.getReturnType());
        });
        JkStockBatchDao batchDao = proxy(JkStockBatchDao.class, (method, args) -> {
            if ("selectById".equals(method.getName())) return sourceBatch;
            return defaultValue(method.getReturnType());
        });
        StockFlowService stockFlowService = proxy(StockFlowService.class, (method, args) -> {
            if ("inboundStock".equals(method.getName())) stockActions.add((JkStockActionRequest) args[0]);
            return defaultValue(method.getReturnType());
        });
        JkPerformanceService performanceService = proxy(JkPerformanceService.class, (method, args) -> {
            if ("record".equals(method.getName())) {
                JkPerformanceRecord record = (JkPerformanceRecord) args[0];
                performances.add(record);
                return record;
            }
            return defaultValue(method.getReturnType());
        });
        JkOperationProfitService profitService = proxy(JkOperationProfitService.class, (method, args) -> {
            if ("record".equals(method.getName())) {
                JkOperationProfitRecord record = (JkOperationProfitRecord) args[0];
                profits.add(record);
                return record;
            }
            return defaultValue(method.getReturnType());
        });
        CommissionScenarioService commissionService = proxy(CommissionScenarioService.class, (method, args) -> {
            if ("dispatch".equals(method.getName())) {
                commissionScenarios.add((JkCommissionRuleTrialRequest) args[0]);
                commissionKeys.add((String) args[1]);
            }
            return defaultValue(method.getReturnType());
        });

        ReflectionTestUtils.setField(service, "transferDao", transferDao);
        ReflectionTestUtils.setField(service, "transferItemDao", transferItemDao);
        ReflectionTestUtils.setField(service, "stockFlowService", stockFlowService);
        ReflectionTestUtils.setField(service, "batchFlowDao", batchFlowDao);
        ReflectionTestUtils.setField(service, "batchDao", batchDao);
        ReflectionTestUtils.setField(service, "performanceService", performanceService);
        ReflectionTestUtils.setField(service, "profitService", profitService);
        ReflectionTestUtils.setField(service, "commissionService", commissionService);

        invokeFinalizeTransfer(service, exception, exceptionItem);

        Assert.assertEquals(1, stockActions.size());
        Assert.assertEquals(Integer.valueOf(8), stockActions.get(0).getQuantity());
        Assert.assertEquals("RECEIVE_EXCEPTION_RESOLUTION", stockActions.get(0).getBusinessType());
        Assert.assertEquals(Long.valueOf(900L), stockActions.get(0).getBusinessId());
        Assert.assertEquals("RECV-EX-REX900", stockActions.get(0).getBatchNo());

        Assert.assertEquals(Integer.valueOf(8), item.getReceivedQty());
        Assert.assertEquals(new BigDecimal("48.00"), item.getCostAmount());
        Assert.assertEquals(new BigDecimal("32.00"), item.getSpreadAmount());

        Assert.assertEquals(2, performances.size());
        JkPerformanceRecord receiverPerformance = performances.get(0);
        Assert.assertEquals("STOCK_TRANSFER", receiverPerformance.getPerformanceType());
        Assert.assertEquals(Long.valueOf(20L), receiverPerformance.getOwnerUserId());
        Assert.assertEquals(Long.valueOf(10L), receiverPerformance.getSourceUserId());
        Assert.assertEquals(Integer.valueOf(8), receiverPerformance.getQuantity());
        Assert.assertEquals(new BigDecimal("80.00"), receiverPerformance.getPerformanceAmount());
        Assert.assertEquals("PERFORMANCE:RECEIVE_EXCEPTION:900:200:RECEIVER:20", receiverPerformance.getActionKey());

        JkPerformanceRecord senderPerformance = performances.get(1);
        Assert.assertEquals("INVENTORY_TURNOVER", senderPerformance.getPerformanceType());
        Assert.assertEquals(Long.valueOf(10L), senderPerformance.getOwnerUserId());
        Assert.assertEquals(Long.valueOf(20L), senderPerformance.getSourceUserId());
        Assert.assertEquals(Integer.valueOf(8), senderPerformance.getQuantity());
        Assert.assertEquals(new BigDecimal("80.00"), senderPerformance.getPerformanceAmount());
        Assert.assertEquals("PERFORMANCE:RECEIVE_EXCEPTION:900:200", senderPerformance.getActionKey());

        Assert.assertEquals(1, profits.size());
        Assert.assertEquals(Long.valueOf(10L), profits.get(0).getUserId());
        Assert.assertEquals(Integer.valueOf(8), profits.get(0).getQuantity());
        Assert.assertEquals(new BigDecimal("80.00"), profits.get(0).getRevenueAmount());
        Assert.assertEquals(new BigDecimal("48.00"), profits.get(0).getCostAmount());
        Assert.assertEquals(new BigDecimal("32.00"), profits.get(0).getProfitAmount());
        Assert.assertEquals("PROFIT:RECEIVE_EXCEPTION:900:200", profits.get(0).getActionKey());

        Assert.assertEquals(1, commissionScenarios.size());
        Assert.assertEquals(Integer.valueOf(8), commissionScenarios.get(0).getQuantity());
        Assert.assertEquals(new BigDecimal("80.00"), commissionScenarios.get(0).getBaseAmount());
        Assert.assertEquals(new BigDecimal("48.00"), commissionScenarios.get(0).getCostAmount());
        Assert.assertEquals(new BigDecimal("32.00"), commissionScenarios.get(0).getRealGrossProfit());
        Assert.assertEquals("COMMISSION:RECEIVE_EXCEPTION:900:200", commissionKeys.get(0));

        invokeFinalizeTransfer(service, exception, exceptionItem);

        Assert.assertEquals(2, stockActions.size());
        Assert.assertEquals(stockActions.get(0).getBusinessType(), stockActions.get(1).getBusinessType());
        Assert.assertEquals(stockActions.get(0).getBusinessId(), stockActions.get(1).getBusinessId());
        Assert.assertEquals(stockActions.get(0).getBatchNo(), stockActions.get(1).getBatchNo());
        Assert.assertEquals(stockActions.get(0).getQuantity(), stockActions.get(1).getQuantity());
        Assert.assertEquals(4, performances.size());
        Assert.assertEquals(performances.get(0).getActionKey(), performances.get(2).getActionKey());
        Assert.assertEquals(performances.get(1).getActionKey(), performances.get(3).getActionKey());
        Assert.assertEquals(2, profits.size());
        Assert.assertEquals(profits.get(0).getActionKey(), profits.get(1).getActionKey());
        Assert.assertEquals(2, commissionKeys.size());
        Assert.assertEquals(commissionKeys.get(0), commissionKeys.get(1));
    }

    private void invokeFinalizeTransfer(JkReceiveExceptionV2Service service, JkTradeReceiveException exception,
                                        JkTradeReceiveExceptionItem exceptionItem) {
        ReflectionTestUtils.invokeMethod(service, "finalizeTransfer", exception,
                Collections.singletonList(exceptionItem), 99L);
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

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }

    private interface Invocation {
        Object apply(Method method, Object[] args) throws Throwable;
    }
}
