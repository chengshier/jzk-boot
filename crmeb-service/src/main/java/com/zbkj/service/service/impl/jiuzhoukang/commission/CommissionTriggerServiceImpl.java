package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.jiuzhoukang.commission.*;
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
 * 前四阶段业务事件适配层。
 * 零售完成生成佣金；平台订货/调拨完成只写可靠业绩事件，不生成可提现佣金。
 */
@Service
public class CommissionTriggerServiceImpl implements CommissionTriggerService {
    @Autowired private CommissionCalculateService calculateService;
    @Autowired private CommissionReverseService reverseService;
    @Autowired private CommissionFreezeService freezeService;
    @Autowired private FundAccountService fundAccountService;
    @Autowired private RetailOrderAttributionService attributionService;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private JkCommissionReverseDao reverseDao;
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkFundAccountDao fundAccountDao;
    @Autowired private JkBusinessEventDao businessEventDao;
    @Autowired private JkStockTransferReturnDao transferReturnDao;
    @Autowired private JkStockTransferDao stockTransferDao;

    @Override
    public void onRetailOrderCompleted(Long orderId, String orderNo, Long orderInfoId, Long receiverUserId,
                                       String role, BigDecimal amount, String requestNo) {
        calculateService.calculateRetailOrder(orderId, orderNo, orderInfoId, receiverUserId, role, amount, requestNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPlatformOrderStockIn(Long id, String no, String requestNo) {
        recordPerformanceEvent("PLATFORM_ORDER_STOCK_IN", id, no, requestNo,
                "平台订货入库完成：前四阶段只记录业绩事件，不生成可提现佣金");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onStockTransferCompleted(Long id, String no, String requestNo) {
        recordPerformanceEvent("STOCK_TRANSFER_COMPLETED", id, no, requestNo,
                "库存调拨完成：前四阶段只记录业绩事件，不写死上级佣金、差价或团队奖励");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onRefundCompleted(Long id, String no, BigDecimal refundAmount, String requestNo) {
        List<RetailOrderAttributionService.RefundAllocation> allocations = attributionService.allocateRefund(no, refundAmount, requestNo);
        for (RetailOrderAttributionService.RefundAllocation allocation : allocations) {
            JkRetailOrderAttribution attribution = allocation.getAttribution();
            BigDecimal remainingBaseBefore = safe(attribution.getItemPaidAmount())
                    .subtract(safe(allocation.getBeforeRefundedAmount())).max(BigDecimal.ZERO);
            if (remainingBaseBefore.signum() <= 0) continue;
            List<JkCommissionRecord> records = recordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                    .eq(JkCommissionRecord::getSourceType, "RETAIL_ORDER")
                    .eq(JkCommissionRecord::getSourceId, attribution.getOrderInfoId())
                    .eq(JkCommissionRecord::getIsDeleted, false));
            for (JkCommissionRecord record : records) {
                BigDecimal remainingCommission = safe(record.getCommissionAmount()).subtract(priorReversed(record.getId())).max(BigDecimal.ZERO);
                if (remainingCommission.signum() <= 0) continue;
                BigDecimal reverseAmount;
                if (allocation.getRefundBaseAmount().compareTo(remainingBaseBefore) >= 0) {
                    reverseAmount = remainingCommission;
                } else {
                    reverseAmount = remainingCommission.multiply(allocation.getRefundBaseAmount())
                            .divide(remainingBaseBefore, 2, RoundingMode.HALF_UP).min(remainingCommission);
                }
                if (reverseAmount.signum() <= 0) continue;
                reverseService.reverse(record.getId(), "RETAIL_ORDER", id, no, "REFUND", reverseAmount,
                        requestNo + ":" + record.getId(), null, "按原订单实付分摊快照冲正");
            }
        }
        recordPerformanceEvent("RETAIL_ORDER_REFUND", id, no, requestNo,
                "零售订单退款已按下单实付快照执行佣金冲正，refundAmount=" + refundAmount);
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
                    ? remaining
                    : remaining.multiply(returnAmount).divide(originalAmount, 2, RoundingMode.HALF_UP).min(remaining);
            if (reverseAmount.signum() > 0) {
                reverseService.reverse(record.getId(), "STOCK_TRANSFER_RETURN", id, no, "TRANSFER_RETURN", reverseAmount,
                        requestNo + ":" + record.getId(), null, "按调拨退回金额比例冲正原调拨佣金");
            }
        }
        recordPerformanceEvent("STOCK_TRANSFER_RETURN", id, no, requestNo,
                records.isEmpty() ? "调拨退回已完成；当前前四阶段调拨只记录业绩，未生成可提现佣金" : "调拨退回已按退回金额比例冲正原调拨佣金");
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
        } catch (DuplicateKeyException ignored) {
            // 并发重复事件由 event_key 唯一约束收口。
        }
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
