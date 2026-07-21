package com.zbkj.service.service.jiuzhoukang.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionReverse;
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
import com.zbkj.common.model.jiuzhoukang.JkWithdrawApply;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyResponse;
import com.zbkj.common.response.jiuzhoukang.JkPriceRuleResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockAccountResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockFlowResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.common.response.jiuzhoukang.JkUserBusinessRoleResponse;
import com.zbkj.service.dao.StoreProductAttrValueDao;
import com.zbkj.service.dao.StoreProductDao;
import com.zbkj.service.dao.UserDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JkDisplayEnrichmentSupport {
    private static final String PRODUCT_DELETED = "商品已删除";
    private static final String SKU_DELETED = "SKU 已删除";
    private static final String USER_MISSING = "用户不存在";
    private static final String REGION_MISSING = "区域未配置";

    @Autowired
    private StoreProductDao productDao;
    @Autowired
    private StoreProductAttrValueDao skuDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private JkRegionDao regionDao;
    @Autowired
    private JkStockAccountDao stockAccountDao;
    @Autowired
    private JkPlatformOrderItemDao platformOrderItemDao;
    @Autowired
    private JkStockTransferItemDao stockTransferItemDao;
    @Autowired
    private JkBusinessRoleService businessRoleService;

    public void enrichStockAccounts(List<JkStockAccountResponse> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(list.stream().map(JkStockAccountResponse::getOwnerUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkStockAccountResponse::getRoleCode).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkStockAccountResponse::getRegionCode).collect(Collectors.toList())));
        for (JkStockAccountResponse item : list) {
            UserView user = context.userMap.get(item.getOwnerUserId());
            if (StrUtil.isBlank(item.getOwnerName())) {
                item.setOwnerName(user == null ? USER_MISSING : user.displayName);
            }
            item.setAccountTypeText(labelStockAccountType(item.getAccountType()));
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setRoleName(resolveRoleName(item.getRoleCode(), context));
            item.setRegionName(resolveRegionName(item.getRegionCode(), context));
            item.setStatusText(Boolean.TRUE.equals(item.getStatus()) ? "启用" : "停用");
            item.setStatusTag(Boolean.TRUE.equals(item.getStatus()) ? "success" : "info");
        }
    }

    public void enrichStockItems(List<JkStockItemResponse> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, JkStockAccount> accountMap = loadStockAccountMap(collectLongs(list.stream().map(JkStockItemResponse::getStockAccountId).collect(Collectors.toList())));
        DisplayContext context = loadContext(
                collectIntegers(list.stream().map(JkStockItemResponse::getProductId).collect(Collectors.toList())),
                collectIntegers(list.stream().map(JkStockItemResponse::getSkuId).collect(Collectors.toList())),
                collectLongs(accountMap.values().stream().map(JkStockAccount::getOwnerUserId).collect(Collectors.toList())),
                collectStrings(accountMap.values().stream().map(JkStockAccount::getRoleCode).collect(Collectors.toList())),
                collectStrings(accountMap.values().stream().map(JkStockAccount::getRegionCode).collect(Collectors.toList())));
        for (JkStockItemResponse item : list) {
            JkStockAccount account = accountMap.get(item.getStockAccountId());
            fillStockAccountDisplay(item, account, context);
            item.setProductName(resolveProductName(item.getProductId(), context));
            item.setSkuName(resolveSkuName(item.getSkuId(), context));
            item.setSkuText(resolveSkuText(item.getSkuId(), context));
        }
    }

    public void enrichStockFlows(List<JkStockFlowResponse> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, JkStockAccount> accountMap = loadStockAccountMap(collectLongs(list.stream().map(JkStockFlowResponse::getStockAccountId).collect(Collectors.toList())));
        DisplayContext context = loadContext(
                collectIntegers(list.stream().map(JkStockFlowResponse::getProductId).collect(Collectors.toList())),
                collectIntegers(list.stream().map(JkStockFlowResponse::getSkuId).collect(Collectors.toList())),
                collectLongs(accountMap.values().stream().map(JkStockAccount::getOwnerUserId).collect(Collectors.toList())),
                collectStrings(accountMap.values().stream().map(JkStockAccount::getRoleCode).collect(Collectors.toList())),
                collectStrings(accountMap.values().stream().map(JkStockAccount::getRegionCode).collect(Collectors.toList())));
        for (JkStockFlowResponse item : list) {
            JkStockAccount account = accountMap.get(item.getStockAccountId());
            fillStockAccountDisplay(item, account, context);
            item.setProductName(resolveProductName(item.getProductId(), context));
            item.setSkuName(resolveSkuName(item.getSkuId(), context));
            item.setSkuText(resolveSkuText(item.getSkuId(), context));
            item.setBusinessTypeText(labelSourceType(item.getBusinessType()));
            item.setFlowTypeText(JkDictLabelHelper.label("stock_flow_type", item.getFlowType()));
            item.setStatusTag(tagOf(item.getFlowType()));
        }
    }

    public void enrichIdentityApplies(List<JkIdentityApplyResponse> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(list.stream().map(JkIdentityApplyResponse::getUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkIdentityApplyResponse::getApplyRoleCode).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkIdentityApplyResponse::getRegionCode).collect(Collectors.toList())));
        for (JkIdentityApplyResponse item : list) {
            UserView user = context.userMap.get(item.getUserId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setApplyRoleName(resolveRoleName(item.getApplyRoleCode(), context));
            item.setRegionName(resolveRegionName(item.getRegionCode(), context));
            item.setAuditStatusText(JkDictLabelHelper.label("audit_status", item.getAuditStatus()));
            item.setStatusText(item.getAuditStatusText());
            item.setStatusTag(tagOf(item.getAuditStatus()));
        }
    }

    public void enrichPlatformOrders(List<JkPlatformOrder> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, ItemSummary> summaryMap = loadPlatformOrderItemSummaries(list);
        DisplayContext context = loadContext(
                collectIntegers(summaryMap.values().stream().map(ItemSummary::getProductId).collect(Collectors.toList())),
                collectIntegers(summaryMap.values().stream().map(ItemSummary::getSkuId).collect(Collectors.toList())),
                collectLongs(list.stream().map(JkPlatformOrder::getUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkPlatformOrder::getRoleCode).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkPlatformOrder::getRegionCode).collect(Collectors.toList())));
        for (JkPlatformOrder item : list) {
            UserView user = context.userMap.get(item.getUserId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setRoleName(resolveRoleName(item.getRoleCode(), context));
            item.setRegionName(resolveRegionName(item.getRegionCode(), context));
            item.setStatusText(JkDictLabelHelper.label("platform_order_status", item.getStatus()));
            item.setStatusTag(tagOf(item.getStatus()));
            item.setAuditStatusText(JkDictLabelHelper.label("audit_status", item.getAuditStatus()));
            item.setPayStatusText(labelPayStatus(item.getPayStatus()));
            item.setReceiveStatusText(labelReceiveStatus(item.getReceiveStatus()));
            fillItemSummary(item, summaryMap.get(item.getId()), context);
        }
    }

    public void enrichPriceRules(List<JkPriceRuleResponse> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(
                collectIntegers(list.stream().map(JkPriceRuleResponse::getProductId).collect(Collectors.toList())),
                collectIntegers(list.stream().map(JkPriceRuleResponse::getSkuId).collect(Collectors.toList())),
                collectLongs(list.stream().map(JkPriceRuleResponse::getUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkPriceRuleResponse::getRoleCode).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkPriceRuleResponse::getRegionCode).collect(Collectors.toList())));
        for (JkPriceRuleResponse item : list) {
            UserView user = context.userMap.get(item.getUserId());
            item.setProductName(resolveProductName(item.getProductId(), context));
            item.setSkuName(resolveSkuName(item.getSkuId(), context));
            item.setSkuText(resolveSkuText(item.getSkuId(), context));
            item.setRoleName(resolveRoleName(item.getRoleCode(), context));
            item.setRegionName(resolveRegionName(item.getRegionCode(), context));
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setStatusText(Boolean.TRUE.equals(item.getStatus()) ? "启用" : "禁用");
            item.setStatusTag(Boolean.TRUE.equals(item.getStatus()) ? "success" : "info");
        }
    }

    public void enrichStockTransfers(List<JkStockTransfer> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, ItemSummary> summaryMap = loadStockTransferItemSummaries(list);
        DisplayContext context = loadContext(
                collectIntegers(summaryMap.values().stream().map(ItemSummary::getProductId).collect(Collectors.toList())),
                collectIntegers(summaryMap.values().stream().map(ItemSummary::getSkuId).collect(Collectors.toList())),
                collectLongs(list.stream().map(JkStockTransfer::getUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkStockTransfer::getRoleCode).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkStockTransfer::getRegionCode).collect(Collectors.toList())));
        for (JkStockTransfer item : list) {
            UserView user = context.userMap.get(item.getUserId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setRoleName(resolveRoleName(item.getRoleCode(), context));
            item.setRegionName(resolveRegionName(item.getRegionCode(), context));
            item.setStatusText(JkDictLabelHelper.label("stock_transfer_status", item.getStatus()));
            item.setStatusTag(tagOf(item.getStatus()));
            item.setAuditStatusText(JkDictLabelHelper.label("audit_status", item.getAuditStatus()));
            item.setPayStatusText(labelPayStatus(item.getPayStatus()));
            item.setReceiveStatusText(labelReceiveStatus(item.getReceiveStatus()));
            fillItemSummary(item, summaryMap.get(item.getId()), context);
        }
    }

    private Map<Long, ItemSummary> loadPlatformOrderItemSummaries(List<JkPlatformOrder> list) {
        Set<Long> ids = collectLongs(list.stream().map(JkPlatformOrder::getId).collect(Collectors.toList()));
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ItemSummary> result = new HashMap<>();
        List<JkPlatformOrderItem> items = platformOrderItemDao.selectList(new LambdaQueryWrapper<JkPlatformOrderItem>()
                .in(JkPlatformOrderItem::getPlatformOrderId, ids)
                .eq(JkPlatformOrderItem::getIsDeleted, false));
        items.sort(Comparator.comparing(JkPlatformOrderItem::getId, Comparator.nullsLast(Long::compareTo)));
        for (JkPlatformOrderItem item : items) {
            result.computeIfAbsent(item.getPlatformOrderId(), key -> new ItemSummary()
                    .setProductId(item.getProductId())
                    .setSkuId(item.getSkuId())
                    .setProductName(item.getProductName())
                    .setSkuName(item.getSkuName())
                    .setSkuText(item.getSkuName()));
        }
        return result;
    }

    private Map<Long, ItemSummary> loadStockTransferItemSummaries(List<JkStockTransfer> list) {
        Set<Long> ids = collectLongs(list.stream().map(JkStockTransfer::getId).collect(Collectors.toList()));
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, ItemSummary> result = new HashMap<>();
        List<JkStockTransferItem> items = stockTransferItemDao.selectList(new LambdaQueryWrapper<JkStockTransferItem>()
                .in(JkStockTransferItem::getTransferId, ids)
                .eq(JkStockTransferItem::getIsDeleted, false));
        items.sort(Comparator.comparing(JkStockTransferItem::getId, Comparator.nullsLast(Long::compareTo)));
        for (JkStockTransferItem item : items) {
            result.computeIfAbsent(item.getTransferId(), key -> new ItemSummary()
                    .setProductId(item.getProductId())
                    .setSkuId(item.getSkuId())
                    .setProductName(item.getProductName())
                    .setSkuName(item.getSkuName())
                    .setSkuText(item.getSkuName()));
        }
        return result;
    }

    private void fillItemSummary(JkPlatformOrder item, ItemSummary summary, DisplayContext context) {
        if (item == null || summary == null) {
            return;
        }
        item.setFirstProductName(StrUtil.blankToDefault(summary.getProductName(), resolveProductName(summary.getProductId(), context)));
        item.setFirstSkuName(StrUtil.blankToDefault(summary.getSkuName(), resolveSkuName(summary.getSkuId(), context)));
        item.setFirstSkuText(StrUtil.blankToDefault(summary.getSkuText(), resolveSkuText(summary.getSkuId(), context)));
    }

    private void fillItemSummary(JkStockTransfer item, ItemSummary summary, DisplayContext context) {
        if (item == null || summary == null) {
            return;
        }
        item.setFirstProductName(StrUtil.blankToDefault(summary.getProductName(), resolveProductName(summary.getProductId(), context)));
        item.setFirstSkuName(StrUtil.blankToDefault(summary.getSkuName(), resolveSkuName(summary.getSkuId(), context)));
        item.setFirstSkuText(StrUtil.blankToDefault(summary.getSkuText(), resolveSkuText(summary.getSkuId(), context)));
    }

    public void enrichUserBusinessRoles(List<JkUserBusinessRoleResponse> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(list.stream().map(JkUserBusinessRoleResponse::getUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkUserBusinessRoleResponse::getRoleCode).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkUserBusinessRoleResponse::getRegionCode).collect(Collectors.toList())));
        for (JkUserBusinessRoleResponse item : list) {
            UserView user = context.userMap.get(item.getUserId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            if (StrUtil.isBlank(item.getRoleName())) {
                item.setRoleName(resolveRoleName(item.getRoleCode(), context));
            }
            item.setRegionName(resolveRegionName(item.getRegionCode(), context));
            item.setAuditStatusText(JkDictLabelHelper.label("audit_status", item.getAuditStatus()));
            item.setStatusTag(tagOf(item.getAuditStatus()));
        }
    }

    public void enrichWithdrawApplies(List<JkWithdrawApply> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(list.stream().map(JkWithdrawApply::getUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkWithdrawApply::getRoleCode).collect(Collectors.toList())),
                Collections.emptySet());
        for (JkWithdrawApply item : list) {
            UserView user = context.userMap.get(item.getUserId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setRoleName(resolveRoleName(item.getRoleCode(), context));
            item.setStatusText(JkDictLabelHelper.label("withdraw_status", item.getStatus()));
            item.setWithdrawStatusText(item.getStatusText());
            item.setStatusTag(tagOf(item.getStatus()));
        }
    }

    public void enrichCommissionRecords(List<JkCommissionRecord> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(list.stream().map(JkCommissionRecord::getReceiverUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkCommissionRecord::getReceiverRoleCode).collect(Collectors.toList())),
                Collections.emptySet());
        for (JkCommissionRecord item : list) {
            UserView user = context.userMap.get(item.getReceiverUserId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setRoleName(resolveRoleName(item.getReceiverRoleCode(), context));
            item.setStatusText(JkDictLabelHelper.label("commission_status", item.getStatus()));
            item.setCommissionStatusText(item.getStatusText());
            item.setSourceTypeText(labelSourceType(item.getSourceType()));
            item.setStatusTag(tagOf(item.getStatus()));
        }
    }

    public void enrichCommissionAccounts(List<JkCommissionAccount> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(list.stream().map(JkCommissionAccount::getUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkCommissionAccount::getRoleCode).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkCommissionAccount::getRegionCode).collect(Collectors.toList())));
        for (JkCommissionAccount item : list) {
            UserView user = context.userMap.get(item.getUserId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setRoleName(resolveRoleName(item.getRoleCode(), context));
            item.setRegionName(resolveRegionName(item.getRegionCode(), context));
            item.setStatusText(Boolean.TRUE.equals(item.getStatus()) ? "启用" : "停用");
            item.setStatusTag(Boolean.TRUE.equals(item.getStatus()) ? "success" : "info");
        }
    }

    public void enrichFundAccounts(List<JkFundAccount> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(list.stream().map(JkFundAccount::getUserId).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkFundAccount::getRoleCode).collect(Collectors.toList())),
                collectStrings(list.stream().map(JkFundAccount::getRegionCode).collect(Collectors.toList())));
        for (JkFundAccount item : list) {
            UserView user = context.userMap.get(item.getUserId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
            item.setUserNickname(user == null ? USER_MISSING : user.nickname);
            item.setRoleName(resolveRoleName(item.getRoleCode(), context));
            item.setRegionName(resolveRegionName(item.getRegionCode(), context));
            item.setStatusText(Boolean.TRUE.equals(item.getStatus()) ? "启用" : "停用");
            item.setStatusTag(Boolean.TRUE.equals(item.getStatus()) ? "success" : "info");
        }
    }

    public void enrichFundFlows(List<JkFundFlow> list, Map<Long, JkFundAccount> accountMap) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, JkFundAccount> accounts = accountMap == null ? Collections.emptyMap() : accountMap;
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(accounts.values().stream().map(JkFundAccount::getUserId).collect(Collectors.toList())),
                collectStrings(accounts.values().stream().map(JkFundAccount::getRoleCode).collect(Collectors.toList())),
                collectStrings(accounts.values().stream().map(JkFundAccount::getRegionCode).collect(Collectors.toList())));
        for (JkFundFlow item : list) {
            JkFundAccount account = accounts.get(item.getAccountId());
            if (account != null) {
                UserView user = context.userMap.get(account.getUserId());
                item.setApplicantName(user == null ? USER_MISSING : user.displayName);
                item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
                item.setUserNickname(user == null ? USER_MISSING : user.nickname);
                item.setRoleName(resolveRoleName(account.getRoleCode(), context));
                item.setRegionName(resolveRegionName(account.getRegionCode(), context));
            }
            item.setFlowTypeText(labelFundFlowType(item.getFlowType()));
            item.setFundFlowTypeText(item.getFlowTypeText());
            item.setSourceTypeText(labelSourceType(item.getSourceType()));
            item.setStatusTag(tagOf(item.getFlowType()));
        }
    }

    public void enrichOfflinePaymentVouchers(List<JkOfflinePaymentVoucher> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (JkOfflinePaymentVoucher item : list) {
            item.setVoucherStatusText(JkDictLabelHelper.label("voucher_audit_status", item.getAuditStatus()));
            item.setVoucherStatusTag(tagOf(item.getAuditStatus()));
        }
    }

    public void enrichCommissionSettleTasks(List<JkCommissionSettleTask> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(list.stream().map(JkCommissionSettleTask::getOperatorId).collect(Collectors.toList())),
                Collections.emptySet(), Collections.emptySet());
        for (JkCommissionSettleTask item : list) {
            UserView user = context.userMap.get(item.getOperatorId());
            item.setApplicantName(user == null ? USER_MISSING : user.displayName);
            item.setStatusText(labelTaskStatus(item.getStatus()));
            item.setStatusTag(tagOf(item.getStatus()));
        }
    }

    public void enrichCommissionReverses(List<JkCommissionReverse> list, Map<Long, JkCommissionRecord> recordMap) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, JkCommissionRecord> records = recordMap == null ? Collections.emptyMap() : recordMap;
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                collectLongs(records.values().stream().map(JkCommissionRecord::getReceiverUserId).collect(Collectors.toList())),
                collectStrings(records.values().stream().map(JkCommissionRecord::getReceiverRoleCode).collect(Collectors.toList())),
                Collections.emptySet());
        for (JkCommissionReverse item : list) {
            JkCommissionRecord record = records.get(item.getOriginalCommissionRecordId());
            if (record != null) {
                UserView user = context.userMap.get(record.getReceiverUserId());
                item.setApplicantName(user == null ? USER_MISSING : user.displayName);
                item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
                item.setUserNickname(user == null ? USER_MISSING : user.nickname);
                item.setRoleName(resolveRoleName(record.getReceiverRoleCode(), context));
                item.setOriginalCommissionNo(record.getCommissionNo());
            }
            item.setStatusText(JkDictLabelHelper.label("commission_status", item.getStatus()));
            item.setCommissionStatusText(item.getStatusText());
            item.setSourceTypeText(labelSourceType(item.getSourceType()));
            item.setReverseTypeText(JkDictLabelHelper.label("commission_reverse_type", item.getReverseType()));
            item.setStatusTag(tagOf(item.getStatus()));
        }
    }

    private DisplayContext loadContext(Set<Integer> productIds, Set<Integer> skuIds, Set<Long> userIds, Set<String> roleCodes, Set<String> regionCodes) {
        DisplayContext context = new DisplayContext();
        if (!productIds.isEmpty()) {
            context.productMap = productDao.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(StoreProduct::getId, value -> value, (a, b) -> a));
        }
        if (!skuIds.isEmpty()) {
            context.skuMap = skuDao.selectBatchIds(skuIds).stream()
                    .collect(Collectors.toMap(StoreProductAttrValue::getId, value -> value, (a, b) -> a));
        }
        if (!userIds.isEmpty()) {
            List<Integer> ids = userIds.stream().filter(Objects::nonNull).map(Long::intValue).collect(Collectors.toList());
            context.userMap = userDao.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(value -> Long.valueOf(value.getUid()), this::toUserView, (a, b) -> a));
        }
        if (!regionCodes.isEmpty()) {
            context.regionMap = regionDao.selectList(new LambdaQueryWrapper<JkRegion>()
                            .in(JkRegion::getRegionCode, regionCodes)
                            .eq(JkRegion::getIsDeleted, false))
                    .stream().collect(Collectors.toMap(JkRegion::getRegionCode, JkRegion::getRegionName, (a, b) -> a));
        }
        if (!roleCodes.isEmpty()) {
            context.roleMap = businessRoleService.list(new LambdaQueryWrapper<JkBusinessRole>()
                            .in(JkBusinessRole::getRoleCode, roleCodes)
                            .eq(JkBusinessRole::getIsDeleted, false))
                    .stream().collect(Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
        }
        return context;
    }

    private UserView toUserView(User user) {
        UserView view = new UserView();
        view.displayName = StrUtil.blankToDefault(user.getRealName(), StrUtil.blankToDefault(user.getNickname(), USER_MISSING));
        view.nickname = StrUtil.blankToDefault(user.getNickname(), view.displayName);
        view.phone = StrUtil.blankToDefault(user.getPhone(), USER_MISSING);
        return view;
    }

    private Map<Long, JkStockAccount> loadStockAccountMap(Set<Long> stockAccountIds) {
        if (stockAccountIds == null || stockAccountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return stockAccountDao.selectBatchIds(stockAccountIds).stream()
                .collect(Collectors.toMap(JkStockAccount::getId, value -> value, (a, b) -> a));
    }

    private void fillStockAccountDisplay(JkStockItemResponse item, JkStockAccount account, DisplayContext context) {
        if (item == null) {
            return;
        }
        if (account == null) {
            item.setApplicantName(USER_MISSING);
            item.setApplicantPhone(USER_MISSING);
            item.setUserNickname(USER_MISSING);
            item.setRoleName("--");
            item.setRegionName(REGION_MISSING);
            return;
        }
        UserView user = context.userMap.get(account.getOwnerUserId());
        item.setApplicantName(user == null ? USER_MISSING : user.displayName);
        item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
        item.setUserNickname(user == null ? USER_MISSING : user.nickname);
        item.setRoleName(resolveRoleName(account.getRoleCode(), context));
        item.setRegionName(resolveRegionName(account.getRegionCode(), context));
    }

    private void fillStockAccountDisplay(JkStockFlowResponse item, JkStockAccount account, DisplayContext context) {
        if (item == null) {
            return;
        }
        if (account == null) {
            item.setApplicantName(USER_MISSING);
            item.setApplicantPhone(USER_MISSING);
            item.setUserNickname(USER_MISSING);
            item.setRoleName("--");
            item.setRegionName(REGION_MISSING);
            return;
        }
        UserView user = context.userMap.get(account.getOwnerUserId());
        item.setApplicantName(user == null ? USER_MISSING : user.displayName);
        item.setApplicantPhone(user == null ? USER_MISSING : user.phone);
        item.setUserNickname(user == null ? USER_MISSING : user.nickname);
        item.setRoleName(resolveRoleName(account.getRoleCode(), context));
        item.setRegionName(resolveRegionName(account.getRegionCode(), context));
    }

    private String resolveProductName(Integer productId, DisplayContext context) {
        if (productId == null) {
            return PRODUCT_DELETED;
        }
        StoreProduct product = context.productMap.get(productId);
        return product == null ? PRODUCT_DELETED : StrUtil.blankToDefault(product.getStoreName(), PRODUCT_DELETED);
    }

    private String resolveSkuName(Integer skuId, DisplayContext context) {
        if (skuId == null) {
            return SKU_DELETED;
        }
        StoreProductAttrValue sku = context.skuMap.get(skuId);
        return sku == null ? SKU_DELETED : StrUtil.blankToDefault(sku.getSuk(), SKU_DELETED);
    }

    private String resolveSkuText(Integer skuId, DisplayContext context) {
        if (skuId == null) {
            return SKU_DELETED;
        }
        StoreProductAttrValue sku = context.skuMap.get(skuId);
        if (sku == null) {
            return SKU_DELETED;
        }
        return StrUtil.blankToDefault(sku.getAttrValue(), StrUtil.blankToDefault(sku.getSuk(), SKU_DELETED));
    }

    private String resolveRoleName(String roleCode, DisplayContext context) {
        if (StrUtil.isBlank(roleCode)) {
            return "--";
        }
        return StrUtil.blankToDefault(context.roleMap.get(roleCode), roleCode);
    }

    private String resolveRegionName(String regionCode, DisplayContext context) {
        if (StrUtil.isBlank(regionCode)) {
            return REGION_MISSING;
        }
        return StrUtil.blankToDefault(context.regionMap.get(regionCode), REGION_MISSING);
    }

    public String resolveRegionNameForDisplay(String regionCode) {
        if (StrUtil.isBlank(regionCode)) {
            return REGION_MISSING;
        }
        DisplayContext context = loadContext(Collections.emptySet(), Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet(), Collections.singleton(regionCode));
        return resolveRegionName(regionCode, context);
    }

    private String tagOf(String code) {
        if (StrUtil.isBlank(code)) {
            return "info";
        }
        if (code.contains("REJECT") || code.contains("CLOSE") || code.contains("CANCEL") || code.contains("FAIL")) {
            return "danger";
        }
        if (code.contains("APPROVED") || code.contains("EFFECTIVE") || code.contains("SUCCESS")
                || code.contains("STOCK_IN") || code.contains("SHIPPED") || code.contains("TRANSFERRED")) {
            return "success";
        }
        if (code.contains("PENDING") || code.contains("SUBMITTED") || code.contains("CREATED")
                || code.contains("FREEZE") || code.contains("FROZEN") || code.contains("RUNNING")) {
            return "warning";
        }
        return "info";
    }

    private String labelFundFlowType(String code) {
        if (StrUtil.isBlank(code)) {
            return "--";
        }
        if ("SETTLE_IN".equals(code)) {
            return "结算转入";
        }
        if ("WITHDRAW_FREEZE".equals(code)) {
            return "提现冻结";
        }
        if ("WITHDRAW_RELEASE".equals(code)) {
            return "提现驳回释放";
        }
        if ("WITHDRAW_PAID".equals(code)) {
            return "线下打款";
        }
        if ("COMMISSION_REVERSE_OUT".equals(code) || "REVERSE_DEDUCT".equals(code)) {
            return "佣金冲正扣减";
        }
        return code;
    }

    private String labelSourceType(String code) {
        if (StrUtil.isBlank(code)) {
            return "--";
        }
        if ("RETAIL_ORDER".equals(code)) {
            return "零售订单";
        }
        if ("PLATFORM_ORDER".equals(code)) {
            return "平台订货";
        }
        if ("STOCK_TRANSFER".equals(code)) {
            return "库存调拨";
        }
        if ("COMMISSION_REVERSE".equals(code)) {
            return "佣金冲正";
        }
        if ("WITHDRAW".equals(code)) {
            return "提现";
        }
        return code;
    }

    private String labelStockAccountType(String code) {
        if (StrUtil.isBlank(code)) {
            return "--";
        }
        if ("PLATFORM".equals(code)) {
            return "平台库存账户";
        }
        if ("RETAIL".equals(code)) {
            return "零售库存账户";
        }
        if ("COUNTY_AGENT".equals(code)) {
            return "区县代库存账户";
        }
        if ("PARTNER".equals(code)) {
            return "合伙人库存账户";
        }
        if ("MAKER".equals(code)) {
            return "创客库存账户";
        }
        return code;
    }

    private String labelTaskStatus(String code) {
        if (StrUtil.isBlank(code)) {
            return "--";
        }
        if ("PENDING".equals(code)) {
            return "待处理";
        }
        if ("RUNNING".equals(code) || "PROCESSING".equals(code)) {
            return "执行中";
        }
        if ("PARTIAL_SUCCESS".equals(code)) {
            return "部分成功";
        }
        if ("SUCCESS".equals(code)) {
            return "已完成";
        }
        if ("FAILED".equals(code)) {
            return "失败";
        }
        return code;
    }

    private String labelPayStatus(String code) {
        return JkDictLabelHelper.label("pay_status", code);
    }

    private String labelReceiveStatus(String code) {
        if (StrUtil.isBlank(code)) {
            return "--";
        }
        if ("WAIT_RECEIVE".equals(code)) {
            return "待收货";
        }
        if ("RECEIVED".equals(code)) {
            return "已收货";
        }
        if ("STOCK_IN".equals(code)) {
            return "已入库";
        }
        return code;
    }

    private Set<Integer> collectIntegers(Collection<Integer> values) {
        return values == null ? Collections.emptySet() : values.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> collectLongs(Collection<Long> values) {
        return values == null ? Collections.emptySet() : values.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> collectStrings(Collection<String> values) {
        return values == null ? Collections.emptySet() : values.stream().filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static class DisplayContext {
        private Map<Integer, StoreProduct> productMap = new HashMap<>();
        private Map<Integer, StoreProductAttrValue> skuMap = new HashMap<>();
        private Map<Long, UserView> userMap = new HashMap<>();
        private Map<String, String> roleMap = new HashMap<>();
        private Map<String, String> regionMap = new HashMap<>();
    }

    private static class UserView {
        private String displayName;
        private String nickname;
        private String phone;
    }

    @lombok.Data
    @lombok.experimental.Accessors(chain = true)
    private static class ItemSummary {
        private Integer productId;
        private Integer skuId;
        private String productName;
        private String skuName;
        private String skuText;
    }
}
