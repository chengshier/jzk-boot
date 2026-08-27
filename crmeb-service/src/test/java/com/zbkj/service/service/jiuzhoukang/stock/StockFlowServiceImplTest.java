package com.zbkj.service.service.jiuzhoukang.stock;

import com.zbkj.common.model.jiuzhoukang.JkStockFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.service.dao.jiuzhoukang.JkStockFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.service.impl.jiuzhoukang.stock.StockFlowServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

public class StockFlowServiceImplTest {

    @Test
    public void writesBusinessTypeAndBusinessIdIntoStockFlow() {
        AtomicReference<JkStockFlow> insertedFlow = new AtomicReference<>();
        JkStockItem stockItem = new JkStockItem()
                .setId(101L)
                .setStockAccountId(20L)
                .setProductId(30)
                .setSkuId(40)
                .setAvailableQty(8)
                .setFrozenQty(2)
                .setIsDeleted(false);
        JkStockItemDao stockItemDao = (JkStockItemDao) Proxy.newProxyInstance(
                JkStockItemDao.class.getClassLoader(),
                new Class[]{JkStockItemDao.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) {
                        return stockItem;
                    }
                    if ("selectById".equals(method.getName())) {
                        return stockItem;
                    }
                    if ("update".equals(method.getName())) {
                        return 1;
                    }
                    return null;
                });
        JkStockFlowDao stockFlowDao = (JkStockFlowDao) Proxy.newProxyInstance(
                JkStockFlowDao.class.getClassLoader(),
                new Class[]{JkStockFlowDao.class},
                (proxy, method, args) -> {
                    if ("insert".equals(method.getName())) {
                        insertedFlow.set((JkStockFlow) args[0]);
                        return 1;
                    }
                    if ("updateById".equals(method.getName())) {
                        return 1;
                    }
                    return null;
                });
        StockFlowServiceImpl service = new StockFlowServiceImpl();
        ReflectionTestUtils.setField(service, "stockItemDao", stockItemDao);
        ReflectionTestUtils.setField(service, "stockFlowDao", stockFlowDao);
        ReflectionTestUtils.setField(service, "stockAccountDao", (JkStockAccountDao) Proxy.newProxyInstance(
                JkStockAccountDao.class.getClassLoader(),
                new Class[]{JkStockAccountDao.class},
                (proxy, method, args) -> "selectById".equals(method.getName())
                        ? new JkStockAccount().setStatus(true).setIsDeleted(false).setAccountType("PARTNER")
                        : null));

        JkStockActionRequest request = new JkStockActionRequest()
                .setBusinessType("PLATFORM_ORDER")
                .setBusinessId(10L)
                .setBusinessNo("PO20260714001")
                .setStockAccountId(20L)
                .setProductId(30)
                .setSkuId(40)
                .setSkuCode("SKU-40")
                .setQuantity(3)
                .setOperatorUserId(1L)
                .setRemark("test");

        service.writeStockFlow(request, "FREEZE");

        JkStockFlow flow = insertedFlow.get();
        Assert.assertEquals("PLATFORM_ORDER", flow.getBusinessType());
        Assert.assertEquals(Long.valueOf(10L), flow.getBusinessId());
    }
}
