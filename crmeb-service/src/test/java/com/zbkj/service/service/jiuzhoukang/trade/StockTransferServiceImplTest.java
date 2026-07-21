package com.zbkj.service.service.jiuzhoukang.trade;

import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentAuditRequest;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.service.impl.jiuzhoukang.trade.StockTransferServiceImpl;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class StockTransferServiceImplTest {

    @Test
    public void receiveFailsWhenStatusTransitionWasAlreadyConsumed() {
        JkStockTransfer transfer = new JkStockTransfer()
                .setId(2L)
                .setUserId(44L)
                .setRoleCode("partner")
                .setStatus("TRANSFERRED")
                .setReceiveStatus("UNRECEIVED");
        JkStockTransferItem item = new JkStockTransferItem()
                .setId(1L)
                .setTransferId(2L)
                .setProductId(94)
                .setSkuId(1296)
                .setQuantity(2);
        JkStockAccount partnerAccount = new JkStockAccount()
                .setId(6L)
                .setAccountType("PARTNER")
                .setOwnerUserId(44L)
                .setStatus(true);
        partnerAccount.setIsDeleted(false);
        AtomicInteger logCount = new AtomicInteger();
        AtomicInteger inboundCount = new AtomicInteger();

        StockTransferServiceImpl service = new StockTransferServiceImpl();
        ReflectionTestUtils.setField(service, "transferDao", proxy(JkStockTransferDao.class, (method, args) -> {
            if ("selectById".equals(method.getName())) return transfer;
            if ("updateById".equals(method.getName())) return 0;
            if ("update".equals(method.getName())) return 0;
            return null;
        }));
        ReflectionTestUtils.setField(service, "itemDao", proxy(JkStockTransferItemDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) return Collections.singletonList(item);
            return null;
        }));
        ReflectionTestUtils.setField(service, "accountDao", proxy(JkStockAccountDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return partnerAccount;
            return null;
        }));
        ReflectionTestUtils.setField(service, "stockFlowService", proxy(StockFlowService.class, (method, args) -> {
            if ("inboundStock".equals(method.getName())) inboundCount.incrementAndGet();
            return null;
        }));
        ReflectionTestUtils.setField(service, "auditLogService", proxy(JkAuditLogService.class, (method, args) -> {
            if ("saveAuditLog".equals(method.getName())) logCount.incrementAndGet();
            return null;
        }));

        JkBusinessActionRequest request = new JkBusinessActionRequest();
        request.setBusinessId(2L);
        request.setRemark("dup receive");
        try {
            service.receive(44L, request);
            Assert.fail("expected duplicate receive rejection");
        } catch (CrmebException e) {
            Assert.assertTrue(e.getMessage().contains("当前状态不能确认收货"));
        }
        Assert.assertEquals(0, inboundCount.get());
        Assert.assertEquals(0, logCount.get());
    }

    @Test
    public void auditApprovesTransferForCountyHandler() {
        JkStockTransfer transfer = new JkStockTransfer()
                .setId(14L)
                .setUserId(43L)
                .setCountyAgentId(41L)
                .setRegionCode("UAT-COUNTY-001")
                .setStatus("SUBMITTED")
                .setAuditStatus("PENDING");
        JkStockTransferItem item = new JkStockTransferItem()
                .setId(2L)
                .setTransferId(14L)
                .setProductId(94)
                .setSkuId(1296)
                .setQuantity(1)
                .setFromStockAccountId(1L);
        JkStockAccount countyAccount = new JkStockAccount()
                .setId(1L)
                .setAccountType(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT)
                .setOwnerUserId(41L)
                .setRegionCode("UAT-COUNTY-001")
                .setStatus(true);
        countyAccount.setIsDeleted(false);
        JkUserContext countyContext = new JkUserContext();
        countyContext.setPrimaryRoleCode(JkBizConstants.ROLE_COUNTY_AGENT);
        countyContext.setAuditStatus(JkBizConstants.AUDIT_STATUS_EFFECTIVE);
        countyContext.setFreezeStatus(false);
        countyContext.setRegionCode("UAT-COUNTY-001");
        AtomicInteger freezeCount = new AtomicInteger();

        StockTransferServiceImpl service = new StockTransferServiceImpl();
        ReflectionTestUtils.setField(service, "transferDao", proxy(JkStockTransferDao.class, (method, args) -> {
            if ("selectById".equals(method.getName())) return transfer;
            if ("updateById".equals(method.getName())) return 1;
            return null;
        }));
        ReflectionTestUtils.setField(service, "itemDao", proxy(JkStockTransferItemDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) return Collections.singletonList(item);
            return null;
        }));
        ReflectionTestUtils.setField(service, "accountDao", proxy(JkStockAccountDao.class, (method, args) -> {
            if ("selectOne".equals(method.getName())) return countyAccount;
            return null;
        }));
        ReflectionTestUtils.setField(service, "contextService", proxy(JkUserContextService.class, (method, args) -> {
            if ("getFrontContext".equals(method.getName())) return countyContext;
            return null;
        }));
        ReflectionTestUtils.setField(service, "stockFlowService", proxy(StockFlowService.class, (method, args) -> {
            if ("freezeStock".equals(method.getName())) freezeCount.incrementAndGet();
            return null;
        }));
        ReflectionTestUtils.setField(service, "auditLogService", proxy(JkAuditLogService.class, (method, args) -> null));

        JkPaymentAuditRequest request = new JkPaymentAuditRequest();
        request.setBusinessId(14L);
        request.setApproved(true);
        request.setRemark("approve");

        JkStockTransfer result = service.audit(41L, request);

        Assert.assertEquals("AUDIT_APPROVED", result.getStatus());
        Assert.assertEquals("APPROVED", result.getAuditStatus());
        Assert.assertEquals(1, freezeCount.get());
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