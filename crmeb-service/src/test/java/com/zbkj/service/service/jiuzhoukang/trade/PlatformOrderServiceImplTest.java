package com.zbkj.service.service.jiuzhoukang.trade;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkOfflinePaymentVoucher;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrderItem;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.request.jiuzhoukang.JkPaymentAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentVoucherRequest;
import com.zbkj.service.dao.jiuzhoukang.JkOfflinePaymentVoucherDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.service.impl.jiuzhoukang.trade.PlatformOrderServiceImpl;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PlatformOrderServiceImplTest {

    @Test
    public void rejectsDuplicateVoucherSubmitBeforePaymentRejection() {
        JkPlatformOrder order = new JkPlatformOrder()
                .setId(1L)
                .setUserId(46L)
                .setStatus("CREATED")
                .setPayStatus("UNPAID")
                .setAuditStatus("NONE");
        JkOfflinePaymentVoucher currentVoucher = new JkOfflinePaymentVoucher()
                .setId(11L)
                .setBusinessType("PLATFORM_ORDER")
                .setBusinessId(1L)
                .setAuditStatus("PENDING")
                .setIsCurrent(true);
        AtomicInteger insertCount = new AtomicInteger();
        PlatformOrderServiceImpl service = new PlatformOrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderDao", proxy(JkPlatformOrderDao.class, (method, args) -> {
            if ("selectById".equals(method.getName())) return order;
            if ("updateById".equals(method.getName())) return 1;
            return null;
        }));
        ReflectionTestUtils.setField(service, "voucherDao", proxy(JkOfflinePaymentVoucherDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return currentVoucher;
            if ("update".equals(method.getName())) return 1;
            if ("insert".equals(method.getName())) {
                insertCount.incrementAndGet();
                return 1;
            }
            return null;
        }));
        ReflectionTestUtils.setField(service, "auditLogService", proxy(JkAuditLogService.class, (method, args) -> null));

        JkPaymentVoucherRequest voucherRequest = new JkPaymentVoucherRequest();
        voucherRequest.setVoucherUrl("https://example.com/a.jpg");
        try {
            service.submitVoucher(46L, 1L, voucherRequest);
            Assert.fail("expected duplicate voucher rejection");
        } catch (CrmebException e) {
            Assert.assertTrue(e.getMessage().contains("付款凭证"));
        }
        Assert.assertEquals(0, insertCount.get());
    }

    @Test
    public void convertsInventoryShortageIntoRejectedOrderInsteadOfThrowing() {
        JkPlatformOrder order = new JkPlatformOrder()
                .setId(2L)
                .setPlatformOrderNo("PO-2")
                .setRequestNo("REQ-2")
                .setStatus("PAYMENT_SUBMITTED")
                .setPayStatus("PAYMENT_SUBMITTED")
                .setAuditStatus("PENDING");
        JkOfflinePaymentVoucher currentVoucher = new JkOfflinePaymentVoucher()
                .setId(22L)
                .setBusinessType("PLATFORM_ORDER")
                .setBusinessId(2L)
                .setAuditStatus("PENDING")
                .setIsCurrent(true);
        JkPlatformOrderItem item = new JkPlatformOrderItem()
                .setId(201L)
                .setPlatformOrderId(2L)
                .setProductId(94)
                .setSkuId(1296)
                .setSkuCode("SKU-1296")
                .setQuantity(3);
        JkStockAccount platformAccount = new JkStockAccount()
                .setId(7L)
                .setAccountType("PLATFORM")
                .setStatus(true)
                .setIsDeleted(false);
        AtomicReference<JkPlatformOrder> updatedOrder = new AtomicReference<>();
        AtomicReference<JkOfflinePaymentVoucher> updatedVoucher = new AtomicReference<>();

        PlatformOrderServiceImpl service = new PlatformOrderServiceImpl();
        ReflectionTestUtils.setField(service, "orderDao", proxy(JkPlatformOrderDao.class, (method, args) -> {
            if ("selectById".equals(method.getName())) return order;
            if ("updateById".equals(method.getName())) {
                updatedOrder.set((JkPlatformOrder) args[0]);
                return 1;
            }
            return null;
        }));
        ReflectionTestUtils.setField(service, "voucherDao", proxy(JkOfflinePaymentVoucherDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return currentVoucher;
            if ("updateById".equals(method.getName())) {
                updatedVoucher.set((JkOfflinePaymentVoucher) args[0]);
                return 1;
            }
            return null;
        }));
        ReflectionTestUtils.setField(service, "stockAccountDao", proxy(JkStockAccountDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return platformAccount;
            return null;
        }));
        ReflectionTestUtils.setField(service, "stockFlowService", proxy(StockFlowService.class, (method, args) -> {
            if ("freezeStock".equals(method.getName())) throw new CrmebException("库存不足");
            return null;
        }));
        ReflectionTestUtils.setField(service, "auditLogService", proxy(JkAuditLogService.class, (method, args) -> null));
        ReflectionTestUtils.setField(service, "itemDao", proxy(JkPlatformOrderItemDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) return Collections.singletonList(item);
            return null;
        }));

        JkPaymentAuditRequest request = new JkPaymentAuditRequest();
        request.setBusinessId(2L);
        request.setApproved(true);
        request.setRemark("phase3 test");
        JkPlatformOrder result = service.auditPayment(41L, request);

        Assert.assertEquals("PAYMENT_REJECTED", result.getStatus());
        Assert.assertEquals("REJECTED", result.getPayStatus());
        Assert.assertEquals("库存不足；phase3 test", result.getRejectReason());
        Assert.assertEquals("REJECTED", updatedVoucher.get().getAuditStatus());
        Assert.assertEquals("PAYMENT_REJECTED", updatedOrder.get().getStatus());
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
