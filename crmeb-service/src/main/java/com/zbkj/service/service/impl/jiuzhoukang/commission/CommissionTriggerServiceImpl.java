package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionReverse;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturn;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessEventDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionReverseDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferReturnDao;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkOperationProfitLedgerService;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkPerformanceLedgerService;
import com.zbkj.service.service.impl.jiuzhoukang.trade.JkTradeLedgerClosureService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionCalculateService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionFreezeService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionTriggerService;
import com.zbkj.service.service.jiuzhoukang.commission.FundAccountService;
import com.zbkj.service.service.jiuzhoukang.order.RetailOrderAttributionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * V3.1 业务事件适配层。
 * 业务事实、业绩、经营收益和平台佣金分开处理；新佣金规则未发布时不产生可提现金额。
 */
@Service
public class CommissionTriggerServiceImpl implements CommissionTriggerService {
    @Autowired private CommissionCalculateService calculateService;
    @Autowired private CommissionReverseService reverseService;
    @Autowired private CommissionFreezeService freezeService;
    @Autowired private FundAccountService fundAccountService;
    @Autowired private RetailOrderAttributionService attributionService;
    @Autowired private JkCommissionV31Service commissionV31Service;
    @Autowired private JkPerformanceLedgerService performanceService;
    @Autowired private JkOperationProfitLedgerService profitService;
    @Autowired private JkTradeLedgerClosureService tradeLedgerClosureService;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private JkCommissionReverseDao reverseDao;
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkFundAccountDao fundAccountDao;
    @Autowired private JkBusinessEventDao businessEventDao;
    @Autowired private JkStockTransferReturnDao transferReturnDao;
    @Autowired private JkStockTransferDao stockTransferDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onRetailOrderCompleted(Long orderId, String orderNo, Long orderInfoId, Long receiverUserId,
                                       String role, BigDecimal amount, String requestNo) {
        List<JkRetailOrderAttribution> attributions = attributionService.listByOrder(orderId, orderNo);
        boolean handledSnapshot = false;
        for (JkRetailOrderAttribution attribution : attributions) {
            if (orderInfoId != null && !orderInfoId.equals(attribution.getOrderInfoId())) continue;
            handledSnapshot = true;
            BigDecimal paid = safe(attribution.getItemPaidAmount());
            Long owner = attribution.getReceiverUserId() == null ? receiverUserId : attribution.getReceiverUserId();
            String ownerRole = attribution.getReceiverRoleCode() == null ? role : attribution.getReceiverRoleCode();
            performanceService.record(new JkPerformanceRecord().setSourceType("RETAIL_ORDER").setSourceId(orderId)
                    .setSourceNo(orderNo).setSourceItemId(attribution.getOrderInfoId()).setPerformanceType("RETAIL_ONLINE")
                    .setOwnerUserId(owner).setOwnerRoleCode(ownerRole).setSourceUserId(attribution.getBuyerUserId())
                    .setDirectParentUserId(attribution.getDirectParentUserId()).setCountyAgentUserId(attribution.getCountyAgentUserId())
                    .setRegionCode(attribution.getRegionCode()).setBaseAmount(paid).setPerformanceAmount(paid)
                    .setRequestNo("PERFORMANCE:RETAIL_ORDER:" + orderId + ":" + attribution.getOrderInfoId())
                    .setRelationSnapshotJson(attribution.getSnapshotJson()).setSourceSnapshotJson(attribution.getSnapshotJson()));
            commissionV31Service.createForScenario(new JkCommissionRuleTrialRequest()
                    .setSourceType("RETAIL_ORDER").setSourceId(orderId).setSourceItemId(attribution.getOrderInfoId()).setSourceNo(orderNo)
                    .setOwnerUserId(owner).setOwnerRoleCode(ownerRole).setDirectParentUserId(attribution.getDirectParentUserId())
                    .setCountyAgentUserId(attribution.getCountyAgentUserId()).setSellerUserId(owner)
                    .setPurchaserUserId(attribution.getBuyerUserId()).setRegionCode(attribution.getRegionCode())
                    .setBaseAmount(paid).setRegisteredCustomer(true).setVoucherPresent(true).setAudited(true)
                    .setRelationSnapshotJson(attribution.getSnapshotJson()).setSourceSnapshotJson(attribution.getSnapshotJson()),
                    "COMMISSION:RETAIL_ORDER:" + orderId + ":" + attribution.getOrderInfoId());
        }
        if (!handledSnapshot) {
            // 没有下单快照时禁止用当前关系生成 V3.1 佣金，仅保留旧规则兼容调用和异常事件。
            recordPerformanceEvent("RETAIL_ORDER_ATTRIBUTION_MISSING", orderId, orderNo, requestNo,
                    "订单完成但未读取到下单时归属快照，V3.1 不生成关系类佣金");
        }
        calculateService.calculateRetailOrder(orderId, orderNo, orderInfoId, receiverUserId, role, amount, requestNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPlatformOrderStockIn(Long id, String no, String requestNo) {
        tradeLedgerClosureService.platformOrderReceived(id, requestNo);
        recordPerformanceEvent("PLATFORM_ORDER_STOCK_IN", id, no, requestNo,
                "平台订货入库完成：已生成订货业绩；仅命中已发布规则时生成平台补贴");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onStockTransferCompleted(Long id, String no, String requestNo) {
        tradeLedgerClosureService.stockTransferReceived(id, requestNo);
        recordPerformanceEvent("STOCK_TRANSFER_COMPLETED", id, no, requestNo,
                "库存调拨完成：已固化批次成本、调拨业绩和线下经营价差；平台补贴规则默认关闭");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onRefundCompleted(Long id, String no, BigDecimal refundAmount, String requestNo) {
        List<RetailOrderAttributionService.RefundAllocation> allocations = attributionService.allocateRefund(no, refundAmount, requestNo);
        BigDecimal totalPaid = BigDecimal.ZERO;
        Long orderId = null;
        for (RetailOrderAttributionService.RefundAllocation allocation : allocations) {
            JkRetailOrderAttribution attribution = allocation.getAttribution();
            if (orderId == null) orderId = attribution.getOrderId();
            totalPaid = totalPaid.add(safe(attribution.getItemPaidAmount()));
            BigDecimal remainingBaseBefore = safe(attribution.getItemPaidAmount())
                    .subtract(safe(allocation.getBeforeRefundedAmount())).max(BigDecimal.ZERO);
            if (remainingBaseBefore.signum() <= 0) continue;
            List<JkCommissionRecord> records = recordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                    .eq(JkCommissionRecord::getSourceType, "RETAIL_ORDER")
                    .and(q -> q.eq(JkCommissionRecord::getSourceId, attribution.getOrderInfoId())
                            .or().eq(JkCommissionRecord::getSourceItemId, attribution.getOrderInfoId()))
                    .eq(JkCommissionRecord::getIsDeleted, false));
            for (JkCommissionRecord record : records) {
                BigDecimal remainingCommission = safe(record.getCommissionAmount()).subtract(priorReversed(record.getId())).max(BigDecimal.ZERO);
                if (remainingCommission.signum() <= 0) continue;
                BigDecimal reverseAmount = allocation.getRefundBaseAmount().compareTo(remainingBaseBefore) >= 0
                        ? remainingCommission
                        : remainingCommission.multiply(allocation.getRefundBaseAmount())
                        .divide(remainingBaseBefore, 2, RoundingMode.HALF_UP).min(remainingCommission);
                if (reverseAmount.signum() <= 0) continue;
                reverseService.reverse(record.getId(), "RETAIL_ORDER", id, no, "REFUND", reverseAmount,
                        requestNo + ":" + record.getId(), null, "按原订单实付分摊快照冲正");
            }
        }
        if (orderId != null && totalPaid.signum() > 0) {
            BigDecimal ratio = safe(refundAmount).divide(totalPaid, 8, RoundingMode.HALF_UP).min(BigDecimal.ONE);
            performanceService.reverseBySource("RETAIL_ORDER", orderId, ratio, "零售退款 " + no);
        }
        recordPerformanceEvent("RETAIL_ORDER_REFUND", id, no, requestNo,
                "零售订单退款已按下单实付快照执行佣金和业绩冲正，refundAmount=" + refundAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onTransferReturnCompleted(Long id, String no, String requestNo) {
        JkStockTransferReturn returnOrder = transferReturnDao.selectById(id);
        if (returnOrder == null || !"COMPLETED".equals(returnOrder.getStatus())) {
            throw new IllegalArgumentException("调拨退回单不存在或尚未完成");
        }
        JkStockTransfer transfer = stockTransferDao.selectById(returnOrder.getOriginalTransferId());
        BigDecimal originalAmount = transfer == null ? BigDecimal.ZERO : safe(transfer.getTotalAmount());
        BigDecimal returnAmount = safe(returnOrder.getReturnAmount());
        List<JkCommissionRecord> records = recordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                .eq(JkCommissionRecord::getSourceType, "STOCK_TRANSFER")
                .and(q -> q.eq(JkCommissionRecord::getSourceId, returnOrder.getOriginalTransferId())
                        .or().eq(JkCommissionRecord::getSourceNo, returnOrder.getOriginalTransferNo()))
                .eq(JkCommissionRecord::getIsDeleted, false));
        for (JkCommissionRecord record : records) {
            BigDecimal remaining = safe(record.getCommissionAmount()).subtract(priorReversed(record.getId())).max(BigDecimal.ZERO);
            if (remaining.signum() <= 0) continue;
            BigDecimal reverseAmount = originalAmount.signum() <= 0 || returnAmount.compareTo(originalAmount) >= 0
                    ? remaining : remaining.multiply(returnAmount).divide(originalAmount, 2, RoundingMode.HALF_UP).min(remaining);
            if (reverseAmount.signum() > 0) {
                reverseService.reverse(record.getId(), "STOCK_TRANSFER_RETURN", id, no, "TRANSFER_RETURN", reverseAmount,
                        requestNo + ":" + record.getId(), null, "按调拨退回金额比例冲正原调拨平台补贴");
            }
        }
        BigDecimal ratio = originalAmount.signum() <= 0 ? BigDecimal.ONE : returnAmount.divide(originalAmount, 8, RoundingMode.HALF_UP).min(BigDecimal.ONE);
        performanceService.reverseBySource("STOCK_TRANSFER", returnOrder.getOriginalTransferId(), ratio, "调拨退回 " + no);
        profitService.reverseBySource("STOCK_TRANSFER", returnOrder.getOriginalTransferId(), ratio, "调拨退回 " + no);
        recordPerformanceEvent("STOCK_TRANSFER_RETURN", id, no, requestNo,
                records.isEmpty() ? "调拨退回已冲减业绩和经营收益；未发现平台补贴" : "调拨退回已按比例冲正平台补贴、业绩和经营收益");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onIdentityFrozen(Long userId, String requestNo) {
        for (JkCommissionAccount account : commissionAccountDao.selectList(new LambdaQueryWrapper<JkCommissionAccount>()
                .eq(JkCommissionAccount::getUserId, userId).eq(JkCommissionAccount::getIsDeleted, false))) {
            BigDecimal settled = safe(account.getSettledAmount());
            if (settled.signum() > 0) {
                freezeService.freezeCommission(userId, account.getRoleCode(), settled, "IDENTITY_FROZEN", "IDENTITY", userId,
                        requestNo, "IDENTITY_COMMISSION_FREEZE:" + userId + ":" + account.getRoleCode() + ":" + requestNo, "身份冻结收益");
            }
        }
        for (JkFundAccount account : fundAccountDao.selectList(new LambdaQueryWrapper<JkFundAccount>()
                .eq(JkFundAccount::getUserId, userId).eq(JkFundAccount::getIsDeleted, false))) {
            BigDecimal available = safe(account.getAvailableAmount());
            if (available.signum() > 0) {
                fundAccountService.freezeAvailable(userId, account.getRoleCode(), available, "IDENTITY", userId,
                        requestNo, "IDENTITY_FUND_FREEZE:" + userId + ":" + account.getRoleCode() + ":" + requestNo, "身份冻结可提现资金");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onIdentityUnfrozen(Long userId, String requestNo) {
        for (JkCommissionAccount account : commissionAccountDao.selectList(new LambdaQueryWrapper<JkCommissionAccount>()
                .eq(JkCommissionAccount::getUserId, userId).eq(JkCommissionAccount::getIsDeleted, false))) {
            BigDecimal frozen = safe(account.getFrozenCommissionAmount());
            if (frozen.signum() > 0) {
                freezeService.releaseCommission(userId, account.getRoleCode(), frozen, "IDENTITY_UNFROZEN", "IDENTITY", userId,
                        requestNo, "IDENTITY_COMMISSION_UNFREEZE:" + userId + ":" + account.getRoleCode() + ":" + requestNo, "身份解冻收益");
            }
        }
        for (JkFundAccount account : fundAccountDao.selectList(new LambdaQueryWrapper<JkFundAccount>()
                .eq(JkFundAccount::getUserId, userId).eq(JkFundAccount::getIsDeleted, false))) {
            BigDecimal identityFrozen = safe(account.getFrozenAmount()).subtract(safe(account.getWithdrawingAmount())).max(BigDecimal.ZERO);
            if (identityFrozen.signum() > 0) {
                fundAccountService.releaseIdentityFrozen(userId, account.getRoleCode(), identityFrozen, "IDENTITY", userId,
                        requestNo, "IDENTITY_FUND_UNFREEZE:" + userId + ":" + account.getRoleCode() + ":" + requestNo, "身份解冻可提现资金");
            }
        }
    }

    private void recordPerformanceEvent(String type, Long id, String no, String requestNo, String remark) {
        String eventKey = type + ":" + id;
        JkBusinessEvent existing = businessEventDao.selectOne(new LambdaQueryWrapper<JkBusinessEvent>()
                .eq(JkBusinessEvent::getEventKey, eventKey).last("limit 1"));
        if (existing != null) return;
        Date now = new Date();
        try {
            businessEventDao.insert(new JkBusinessEvent().setEventKey(eventKey).setEventType(type).setBusinessId(id)
                    .setBusinessNo(no).setPayloadJson(remark).setEventStatus("SUCCESS").setRetryCount(0).setMaxRetryCount(8)
                    .setOccurredTime(now).setProcessedTime(now).setCreateTime(now).setUpdateTime(now));
        } catch (DuplicateKeyException ignored) { }
    }

    private BigDecimal priorReversed(Long recordId) {
        BigDecimal total = BigDecimal.ZERO;
        for (JkCommissionReverse reverse : reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>()
                .eq(JkCommissionReverse::getOriginalCommissionRecordId, recordId)
                .eq(JkCommissionReverse::getStatus, "SUCCESS"))) {
            total = total.add(safe(reverse.getReverseAmount()));
        }
        return total;
    }

    private BigDecimal safe(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }
}
