package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionSettleTask;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount;
import com.zbkj.common.model.jiuzhoukang.JkFundFlow;
import com.zbkj.common.model.jiuzhoukang.JkOfflinePaymentVoucher;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrderItem;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import com.zbkj.common.response.jiuzhoukang.JkUserBusinessRoleResponse;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockFlowResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.service.dao.StoreProductAttrValueDao;
import com.zbkj.service.dao.StoreProductDao;
import com.zbkj.service.dao.UserDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JkDisplayEnrichmentSupportTest {

    @Test
    public void enrichesStockItemsWithBatchLoadedNamesAndFallbacks() {
        JkDisplayEnrichmentSupport support = new JkDisplayEnrichmentSupport();
        ReflectionTestUtils.setField(support, "productDao", proxy(StoreProductDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new StoreProduct().setId(101).setStoreName("灵芝孢子粉"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "skuDao", proxy(StoreProductAttrValueDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new StoreProductAttrValue().setId(202).setSuk("大瓶装").setAttrValue("规格:大瓶装"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "stockAccountDao", proxy(JkStockAccountDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Arrays.asList(
                        new JkStockAccount().setId(301L).setOwnerUserId(18L).setRoleCode("partner").setRegionCode("CN-3301")
                );
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "userDao", proxy(UserDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new User().setUid(18).setRealName("张三").setNickname("三哥").setPhone("13800000000"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "regionDao", proxy(JkRegionDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Collections.singletonList(new JkRegion().setRegionCode("CN-3301").setRegionName("杭州一区"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "businessRoleService", proxy(JkBusinessRoleService.class, (method, args) -> {
            if ("list".equals(method.getName())) {
                return Collections.singletonList(new JkBusinessRole().setRoleCode("partner").setRoleName("合伙人"));
            }
            return Collections.emptyList();
        }));

        JkStockItemResponse existing = new JkStockItemResponse();
        existing.setStockAccountId(301L);
        existing.setProductId(101);
        existing.setSkuId(202);
        JkStockItemResponse deleted = new JkStockItemResponse();
        deleted.setStockAccountId(999L);
        deleted.setProductId(999);
        deleted.setSkuId(888);

        List<JkStockItemResponse> list = Arrays.asList(existing, deleted);
        support.enrichStockItems(list);

        Assert.assertEquals("灵芝孢子粉", existing.getProductName());
        Assert.assertEquals("大瓶装", existing.getSkuName());
        Assert.assertEquals("规格:大瓶装", existing.getSkuText());
        Assert.assertEquals("张三", existing.getApplicantName());
        Assert.assertEquals("13800000000", existing.getApplicantPhone());
        Assert.assertEquals("三哥", existing.getUserNickname());
        Assert.assertEquals("合伙人", existing.getRoleName());
        Assert.assertEquals("杭州一区", existing.getRegionName());
        Assert.assertEquals("商品已删除", deleted.getProductName());
        Assert.assertEquals("SKU 已删除", deleted.getSkuName());
        Assert.assertEquals("SKU 已删除", deleted.getSkuText());
        Assert.assertEquals("用户不存在", deleted.getApplicantName());
        Assert.assertEquals("用户不存在", deleted.getApplicantPhone());
        Assert.assertEquals("用户不存在", deleted.getUserNickname());
        Assert.assertEquals("--", deleted.getRoleName());
        Assert.assertEquals("区域未配置", deleted.getRegionName());
    }

    @Test
    public void enrichesStockFlowsWithAccountOwnerAndDisplayNames() {
        JkDisplayEnrichmentSupport support = new JkDisplayEnrichmentSupport();
        ReflectionTestUtils.setField(support, "productDao", proxy(StoreProductDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new StoreProduct().setId(501).setStoreName("益生菌"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "skuDao", proxy(StoreProductAttrValueDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new StoreProductAttrValue().setId(601).setSuk("礼盒装").setAttrValue("规格:礼盒装"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "stockAccountDao", proxy(JkStockAccountDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new JkStockAccount().setId(701L).setOwnerUserId(28L).setRoleCode("county_agent").setRegionCode("CN-3302"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "userDao", proxy(UserDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new User().setUid(28).setRealName("李四").setNickname("四姐").setPhone("13900000000"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "regionDao", proxy(JkRegionDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Collections.singletonList(new JkRegion().setRegionCode("CN-3302").setRegionName("宁波二区"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "businessRoleService", proxy(JkBusinessRoleService.class, (method, args) -> {
            if ("list".equals(method.getName())) {
                return Collections.singletonList(new JkBusinessRole().setRoleCode("county_agent").setRoleName("区县代"));
            }
            return Collections.emptyList();
        }));

        JkStockFlowResponse flow = new JkStockFlowResponse();
        flow.setStockAccountId(701L);
        flow.setProductId(501);
        flow.setSkuId(601);
        flow.setBusinessType("PLATFORM_ORDER");
        flow.setFlowType("IN");

        support.enrichStockFlows(Collections.singletonList(flow));

        Assert.assertEquals("李四", flow.getApplicantName());
        Assert.assertEquals("13900000000", flow.getApplicantPhone());
        Assert.assertEquals("四姐", flow.getUserNickname());
        Assert.assertEquals("区县代", flow.getRoleName());
        Assert.assertEquals("宁波二区", flow.getRegionName());
        Assert.assertEquals("益生菌", flow.getProductName());
        Assert.assertEquals("礼盒装", flow.getSkuName());
        Assert.assertEquals("规格:礼盒装", flow.getSkuText());
        Assert.assertEquals("平台订货", flow.getBusinessTypeText());
    }

    @Test
    public void enrichesIdentityApplyWithUserRoleRegionAndStatusText() {
        JkDisplayEnrichmentSupport support = new JkDisplayEnrichmentSupport();
        ReflectionTestUtils.setField(support, "userDao", proxy(UserDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new User().setUid(18).setRealName("张三").setNickname("三哥").setPhone("13800000000"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "regionDao", proxy(JkRegionDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Collections.singletonList(new JkRegion().setRegionCode("CN-3301").setRegionName("杭州一区"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "businessRoleService", proxy(JkBusinessRoleService.class, (method, args) -> {
            if ("list".equals(method.getName())) {
                return Collections.singletonList(new JkBusinessRole().setRoleCode("partner").setRoleName("合伙人"));
            }
            return Collections.emptyList();
        }));

        JkIdentityApplyResponse existing = new JkIdentityApplyResponse();
        existing.setUserId(18L);
        existing.setApplyRoleCode("partner");
        existing.setRegionCode("CN-3301");
        existing.setAuditStatus("PENDING");
        JkIdentityApplyResponse missing = new JkIdentityApplyResponse();
        missing.setUserId(999L);
        missing.setApplyRoleCode("unknown_role");
        missing.setRegionCode("UNKNOWN");
        missing.setAuditStatus("REJECTED");

        List<JkIdentityApplyResponse> list = Arrays.asList(existing, missing);
        support.enrichIdentityApplies(list);

        Assert.assertEquals("张三", existing.getApplicantName());
        Assert.assertEquals("13800000000", existing.getApplicantPhone());
        Assert.assertEquals("三哥", existing.getUserNickname());
        Assert.assertEquals("合伙人", existing.getApplyRoleName());
        Assert.assertEquals("杭州一区", existing.getRegionName());
        Assert.assertEquals("待审核", existing.getAuditStatusText());
        Assert.assertEquals("warning", existing.getStatusTag());

        Assert.assertEquals("用户不存在", missing.getApplicantName());
        Assert.assertEquals("用户不存在", missing.getApplicantPhone());
        Assert.assertEquals("用户不存在", missing.getUserNickname());
        Assert.assertEquals("unknown_role", missing.getApplyRoleName());
        Assert.assertEquals("区域未配置", missing.getRegionName());
        Assert.assertEquals("已驳回", missing.getAuditStatusText());
        Assert.assertEquals("danger", missing.getStatusTag());
    }

    @Test
    public void enrichesPlatformOrderStatusesAndUserBusinessRoleTexts() {
        JkDisplayEnrichmentSupport support = new JkDisplayEnrichmentSupport();
        ReflectionTestUtils.setField(support, "platformOrderItemDao", proxy(JkPlatformOrderItemDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Collections.singletonList(new JkPlatformOrderItem().setPlatformOrderId(901L).setProductId(501).setSkuId(601).setProductName("益生菌").setSkuName("礼盒装"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "userDao", proxy(UserDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new User().setUid(28).setRealName("李四").setNickname("四姐").setPhone("13900000000"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "productDao", proxy(StoreProductDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new StoreProduct().setId(501).setStoreName("益生菌"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "skuDao", proxy(StoreProductAttrValueDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new StoreProductAttrValue().setId(601).setSuk("礼盒装").setAttrValue("规格:礼盒装"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "regionDao", proxy(JkRegionDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Collections.singletonList(new JkRegion().setRegionCode("CN-3302").setRegionName("宁波二区"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "businessRoleService", proxy(JkBusinessRoleService.class, (method, args) -> {
            if ("list".equals(method.getName())) {
                return Collections.singletonList(new JkBusinessRole().setRoleCode("county_agent").setRoleName("区县代"));
            }
            return Collections.emptyList();
        }));

        JkPlatformOrder order = new JkPlatformOrder();
        order.setId(901L);
        order.setUserId(28L);
        order.setRoleCode("county_agent");
        order.setRegionCode("CN-3302");
        order.setStatus("PAYMENT_APPROVED");
        order.setPayStatus("PAYMENT_SUBMITTED");
        order.setAuditStatus("APPROVED");
        order.setReceiveStatus("STOCK_IN");
        support.enrichPlatformOrders(Collections.singletonList(order));

        Assert.assertEquals("李四", order.getApplicantName());
        Assert.assertEquals("区县代", order.getRoleName());
        Assert.assertEquals("宁波二区", order.getRegionName());
        Assert.assertEquals("付款审核通过", order.getStatusText());
        Assert.assertEquals("待确认付款", order.getPayStatusText());
        Assert.assertEquals("审核通过", order.getAuditStatusText());
        Assert.assertEquals("已入库", order.getReceiveStatusText());
        Assert.assertEquals("益生菌", order.getFirstProductName());
        Assert.assertEquals("礼盒装", order.getFirstSkuName());
        Assert.assertEquals("礼盒装", order.getFirstSkuText());

        JkUserBusinessRoleResponse role = new JkUserBusinessRoleResponse();
        role.setUserId(28L);
        role.setRoleCode("county_agent");
        role.setRegionCode("CN-3302");
        role.setAuditStatus("FROZEN");
        support.enrichUserBusinessRoles(Collections.singletonList(role));

        Assert.assertEquals("李四", role.getApplicantName());
        Assert.assertEquals("13900000000", role.getApplicantPhone());
        Assert.assertEquals("四姐", role.getUserNickname());
        Assert.assertEquals("区县代", role.getRoleName());
        Assert.assertEquals("宁波二区", role.getRegionName());
        Assert.assertEquals("已冻结", role.getAuditStatusText());
        Assert.assertEquals("warning", role.getStatusTag());
    }

    @Test
    public void enrichesStockTransferListWithFirstItemSummary() {
        JkDisplayEnrichmentSupport support = new JkDisplayEnrichmentSupport();
        ReflectionTestUtils.setField(support, "stockTransferItemDao", proxy(JkStockTransferItemDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Collections.singletonList(new JkStockTransferItem().setTransferId(1001L).setProductId(701).setSkuId(801).setProductName("鱼油").setSkuName("90粒装"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "userDao", proxy(UserDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new User().setUid(39).setRealName("赵六").setNickname("六姐").setPhone("13600000000"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "productDao", proxy(StoreProductDao.class, (method, args) -> Collections.emptyList()));
        ReflectionTestUtils.setField(support, "skuDao", proxy(StoreProductAttrValueDao.class, (method, args) -> Collections.emptyList()));
        ReflectionTestUtils.setField(support, "regionDao", proxy(JkRegionDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Collections.singletonList(new JkRegion().setRegionCode("CN-3304").setRegionName("嘉兴四区"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "businessRoleService", proxy(JkBusinessRoleService.class, (method, args) -> {
            if ("list".equals(method.getName())) {
                return Collections.singletonList(new JkBusinessRole().setRoleCode("partner").setRoleName("合伙人"));
            }
            return Collections.emptyList();
        }));

        JkStockTransfer transfer = new JkStockTransfer();
        transfer.setId(1001L);
        transfer.setUserId(39L);
        transfer.setRoleCode("partner");
        transfer.setRegionCode("CN-3304");
        transfer.setStatus("PAYMENT_SUBMITTED");

        support.enrichStockTransfers(Collections.singletonList(transfer));

        Assert.assertEquals("鱼油", transfer.getFirstProductName());
        Assert.assertEquals("90粒装", transfer.getFirstSkuName());
        Assert.assertEquals("90粒装", transfer.getFirstSkuText());
        Assert.assertEquals("赵六", transfer.getApplicantName());
    }

    @Test
    public void enrichesFundFlowTypeAliasAndOfflineVoucherStatusText() {
        JkDisplayEnrichmentSupport support = new JkDisplayEnrichmentSupport();
        ReflectionTestUtils.setField(support, "userDao", proxy(UserDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new User().setUid(36).setRealName("王五").setNickname("五哥").setPhone("13700000000"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "regionDao", proxy(JkRegionDao.class, (method, args) -> {
            if ("selectList".equals(method.getName())) {
                return Collections.singletonList(new JkRegion().setRegionCode("CN-3303").setRegionName("温州三区"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "businessRoleService", proxy(JkBusinessRoleService.class, (method, args) -> {
            if ("list".equals(method.getName())) {
                return Collections.singletonList(new JkBusinessRole().setRoleCode("maker").setRoleName("创客"));
            }
            return Collections.emptyList();
        }));

        JkFundFlow flow = new JkFundFlow();
        flow.setAccountId(801L);
        flow.setFlowType("WITHDRAW_FREEZE");
        flow.setSourceType("WITHDRAW");
        Map<Long, JkFundAccount> accountMap = new HashMap<>();
        accountMap.put(801L, new JkFundAccount().setId(801L).setUserId(36L).setRoleCode("maker").setRegionCode("CN-3303"));

        support.enrichFundFlows(Collections.singletonList(flow), accountMap);

        Assert.assertEquals("提现冻结", flow.getFlowTypeText());
        Assert.assertEquals("提现冻结", flow.getFundFlowTypeText());
        Assert.assertEquals("提现", flow.getSourceTypeText());
        Assert.assertEquals("王五", flow.getApplicantName());
        Assert.assertEquals("创客", flow.getRoleName());
        Assert.assertEquals("温州三区", flow.getRegionName());

        JkOfflinePaymentVoucher voucher = new JkOfflinePaymentVoucher();
        voucher.setAuditStatus("APPROVED");
        support.enrichOfflinePaymentVouchers(Collections.singletonList(voucher));
        Assert.assertEquals("审核通过", voucher.getVoucherStatusText());
        Assert.assertEquals("success", voucher.getVoucherStatusTag());
    }

    @Test
    public void enrichesCommissionSourceTypeAndSettleTaskStatusText() {
        JkDisplayEnrichmentSupport support = new JkDisplayEnrichmentSupport();
        ReflectionTestUtils.setField(support, "userDao", proxy(UserDao.class, (method, args) -> {
            if ("selectBatchIds".equals(method.getName())) {
                return Collections.singletonList(new User().setUid(66).setRealName("陈七").setNickname("七哥").setPhone("13500000000"));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(support, "businessRoleService", proxy(JkBusinessRoleService.class, (method, args) -> {
            if ("list".equals(method.getName())) {
                return Collections.singletonList(new JkBusinessRole().setRoleCode("maker").setRoleName("创客"));
            }
            return Collections.emptyList();
        }));

        JkCommissionRecord record = new JkCommissionRecord();
        record.setReceiverUserId(66L);
        record.setReceiverRoleCode("maker");
        record.setSourceType("PLATFORM_ORDER");
        record.setStatus("PENDING_SETTLE");
        support.enrichCommissionRecords(Collections.singletonList(record));

        Assert.assertEquals("平台订货", record.getSourceTypeText());
        Assert.assertEquals("待结算", record.getCommissionStatusText());
        Assert.assertEquals("warning", record.getStatusTag());

        JkCommissionSettleTask running = new JkCommissionSettleTask();
        running.setOperatorId(66L);
        running.setStatus("RUNNING");
        JkCommissionSettleTask partial = new JkCommissionSettleTask();
        partial.setOperatorId(66L);
        partial.setStatus("PARTIAL_SUCCESS");
        support.enrichCommissionSettleTasks(Arrays.asList(running, partial));

        Assert.assertEquals("执行中", running.getStatusText());
        Assert.assertEquals("warning", running.getStatusTag());
        Assert.assertEquals("部分成功", partial.getStatusText());
        Assert.assertEquals("success", partial.getStatusTag());
        Assert.assertEquals("陈七", running.getApplicantName());
    }

    private <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) {
                    return type.getSimpleName() + "Proxy";
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
            }
            return invocation.apply(method, args);
        }));
    }

    private interface Invocation {
        Object apply(Method method, Object[] args) throws Throwable;
    }
}
