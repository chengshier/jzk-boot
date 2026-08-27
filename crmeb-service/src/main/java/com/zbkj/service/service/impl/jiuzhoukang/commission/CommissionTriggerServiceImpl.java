package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionReverse;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrderItem;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.model.jiuzhoukang.JkStockBatchReservation;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturn;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessEventDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionReverseDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchReservationDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferReturnDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionFreezeService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionTriggerService;
import com.zbkj.service.service.jiuzhoukang.commission.FundAccountService;
import com.zbkj.service.service.jiuzhoukang.order.RetailOrderAttributionService;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import com.zbkj.service.service.jiuzhoukang.promotion.JkPromotionEffectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V3.1 统一业务事件适配层。
 * 业务完成固定生成业绩和线下经营收益；只有已发布生效规则才生成 PLATFORM_PAYABLE 佣金。
 */
@Service
public class CommissionTriggerServiceImpl implements CommissionTriggerService {
    @Autowired private CommissionReverseService reverseService;
    @Autowired private CommissionFreezeService freezeService;
    @Autowired private FundAccountService fundAccountService;
    @Autowired private RetailOrderAttributionService attributionService;
    @Autowired private CommissionScenarioService scenarioService;
    @Autowired private JkPerformanceService performanceService;
    @Autowired private JkOperationProfitService profitService;
    @Autowired private JkPromotionEffectService promotionEffectService;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private JkCommissionReverseDao reverseDao;
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkFundAccountDao fundAccountDao;
    @Autowired private JkBusinessEventDao businessEventDao;
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkPlatformOrderItemDao platformOrderItemDao;
    @Autowired private JkStockTransferDao stockTransferDao;
    @Autowired private JkStockTransferItemDao stockTransferItemDao;
    @Autowired private JkStockTransferReturnDao transferReturnDao;
    @Autowired private JkStockBatchReservationDao reservationDao;
    @Autowired private JkStockBatchDao batchDao;

    @Override
    public void onRetailOrderCompleted(Long orderId, String orderNo, Long orderInfoId, Long receiverUserId,
                                       String role, BigDecimal amount, String requestNo) {
        JkRetailOrderAttribution attribution = findAttribution(orderId, orderNo, orderInfoId);
        if (attribution == null) {
            recordBusinessEvent("RETAIL_ONLINE_COMPLETED", orderId, orderNo, requestNo, "FAILED",
                    "缺少下单时归属快照，不读取当前关系兜底，不生成业绩和佣金");
            return;
        }
        BigDecimal paidAmount = safe(attribution.getItemPaidAmount()).signum() > 0
                ? attribution.getItemPaidAmount() : safe(amount);
        Long ownerUserId = attribution.getReceiverUserId() == null ? receiverUserId : attribution.getReceiverUserId();
        String ownerRole = attribution.getReceiverRoleCode() == null ? role : attribution.getReceiverRoleCode();
        performanceService.record(new JkPerformanceRecord().setSourceType("RETAIL_ORDER").setSourceId(orderId)
                .setSourceNo(orderNo).setSourceItemId(orderInfoId).setPerformanceType("RETAIL_ONLINE")
                .setOwnerUserId(ownerUserId).setOwnerRoleCode(ownerRole).setSourceUserId(attribution.getBuyerUserId())
                .setDirectParentUserId(attribution.getDirectParentUserId()).setCountyAgentUserId(attribution.getCountyAgentUserId())
                .setRegionCode(attribution.getRegionCode()).setQuantity(0).setBaseAmount(paidAmount).setPerformanceAmount(paidAmount)
                .setRequestNo(requestNo).setRelationSnapshotJson(attribution.getSnapshotJson())
                .setSourceSnapshotJson(retailSourceSnapshot(attribution))
                .setActionKey("PERF:RETAIL_ORDER:" + orderInfoId + ":" + ownerUserId));
        JkCommissionRuleTrialRequest scenario = new JkCommissionRuleTrialRequest();
        scenario.setScenario("RETAIL_ONLINE_COMPLETED");
        scenario.setSourceType("RETAIL_ORDER");
        scenario.setSourceId(orderId);
        scenario.setSourceItemId(orderInfoId);
        scenario.setBuyerUserId(attribution.getBuyerUserId());
        scenario.setSellerUserId(ownerUserId);
        scenario.setDirectParentUserId(attribution.getDirectParentUserId());
        scenario.setCountyAgentUserId(attribution.getCountyAgentUserId());
        scenario.setRegionCode(attribution.getRegionCode());
        scenario.setBaseAmount(paidAmount);
        scenario.setRegisteredCustomer(true);
        scenario.setVoucherPresent(true);
        scenario.setAudited(true);
        dispatchSafely(scenario, "COMMISSION:RETAIL_ORDER:" + orderInfoId, orderNo, requestNo,
                "RETAIL_ONLINE_COMPLETED", orderId);
        recordBusinessEvent("RETAIL_ONLINE_COMPLETED", orderId, orderNo, requestNo, "SUCCESS",
                "线上零售业绩已按下单归属快照生成；佣金仅按已发布规则处理");
    }

    @Override
    public void onPlatformOrderStockIn(Long id, String no, String requestNo) {
        JkPlatformOrder order = platformOrderDao.selectById(id);
        if (order == null || Boolean.TRUE.equals(order.getIsDeleted())) {
            recordBusinessEvent("PLATFORM_ORDER_RECEIVED", id, no, requestNo, "FAILED", "平台订货单不存在");
            return;
        }
        List<JkPlatformOrderItem> items = platformOrderItemDao.selectList(new LambdaQueryWrapper<JkPlatformOrderItem>()
                .eq(JkPlatformOrderItem::getPlatformOrderId, id).eq(JkPlatformOrderItem::getIsDeleted, false));
        for (JkPlatformOrderItem item : items) {
            performanceService.record(new JkPerformanceRecord().setSourceType("PLATFORM_ORDER").setSourceId(id)
                    .setSourceNo(no).setSourceItemId(item.getId()).setPerformanceType("PLATFORM_PURCHASE")
                    .setOwnerUserId(order.getUserId()).setOwnerRoleCode(order.getRoleCode()).setSourceUserId(order.getUserId())
                    .setCountyAgentUserId(order.getCountyAgentId() == null ? order.getUserId() : order.getCountyAgentId())
                    .setRegionCode(order.getRegionCode()).setProductId(item.getProductId()).setSkuId(item.getSkuId())
                    .setQuantity(item.getQuantity()).setBaseAmount(item.getTotalAmount()).setPerformanceAmount(item.getTotalAmount())
                    .setRequestNo(requestNo).setRelationSnapshotJson(platformSnapshot(order)).setSourceSnapshotJson(JSONUtil.toJsonStr(item))
                    .setActionKey("PERF:PLATFORM_ORDER:" + item.getId() + ":" + order.getUserId()));
            JkCommissionRuleTrialRequest scenario = businessScenario("PLATFORM_ORDER_RECEIVED", "PLATFORM_ORDER", id, item.getId(),
                    order.getUserId(), order.getUserId(), null, order.getCountyAgentId() == null ? order.getUserId() : order.getCountyAgentId(),
                    order.getRegionCode(), item.getProductId(), item.getSkuId(), item.getQuantity(), item.getTotalAmount(), null);
            scenario.setRegisteredCustomer(true); scenario.setVoucherPresent(true); scenario.setAudited(true);
            dispatchSafely(scenario, "COMMISSION:PLATFORM_ORDER:" + item.getId(), no, requestNo,
                    "PLATFORM_ORDER_RECEIVED", id);
        }
        recordBusinessEvent("PLATFORM_ORDER_RECEIVED", id, no, requestNo, "SUCCESS",
                "平台订货业绩已生成；订货补贴模板未发布时不增加可提现佣金");
    }

    @Override
    public void onStockTransferCompleted(Long id, String no, String requestNo) {
        JkStockTransfer transfer = stockTransferDao.selectById(id);
        if (transfer == null || Boolean.TRUE.equals(transfer.getIsDeleted())) {
            recordBusinessEvent("STOCK_TRANSFER_RECEIVED", id, no, requestNo, "FAILED", "库存调拨单不存在");
            return;
        }
        List<JkStockTransferItem> items = stockTransferItemDao.selectList(new LambdaQueryWrapper<JkStockTransferItem>()
                .eq(JkStockTransferItem::getTransferId, id).eq(JkStockTransferItem::getIsDeleted, false));
        for (JkStockTransferItem item : items) {
            try {
                TransferCost cost = transferCost(transfer, item);
                BigDecimal revenue = safe(item.getTotalAmount());
                BigDecimal profit = revenue.subtract(cost.costAmount).setScale(2, RoundingMode.HALF_UP);
                item.setReceivedQty(item.getQuantity()).setSourceUnitCost(cost.unitCost).setCostAmount(cost.costAmount)
                        .setUnitSpread(item.getUnitPrice().subtract(cost.unitCost).setScale(2, RoundingMode.HALF_UP))
                        .setSpreadAmount(profit).setCostMethod("RESERVED_BATCH_WEIGHTED")
                        .setCostSnapshotJson(cost.snapshotJson).setProfitStatus("CONFIRMED").setUpdateTime(new Date());
                stockTransferItemDao.updateById(item);
                Long sender = transfer.getCountyAgentId();
                Long receiver = transfer.getUserId();
                performanceService.record(new JkPerformanceRecord().setSourceType("STOCK_TRANSFER").setSourceId(id)
                        .setSourceNo(no).setSourceItemId(item.getId()).setPerformanceType("STOCK_TRANSFER")
                        .setOwnerUserId(receiver).setOwnerRoleCode(transfer.getRoleCode()).setSourceUserId(sender)
                        .setCountyAgentUserId(sender).setRegionCode(transfer.getRegionCode()).setProductId(item.getProductId())
                        .setSkuId(item.getSkuId()).setQuantity(item.getQuantity()).setBaseAmount(revenue).setPerformanceAmount(revenue)
                        .setRequestNo(requestNo).setRelationSnapshotJson(transferSnapshot(transfer)).setSourceSnapshotJson(JSONUtil.toJsonStr(item))
                        .setActionKey("PERF:STOCK_TRANSFER:" + item.getId() + ":RECEIVER:" + receiver));
                if (sender != null) {
                    performanceService.record(new JkPerformanceRecord().setSourceType("STOCK_TRANSFER").setSourceId(id)
                            .setSourceNo(no).setSourceItemId(item.getId()).setPerformanceType("INVENTORY_TURNOVER")
                            .setOwnerUserId(sender).setOwnerRoleCode("county_agent").setSourceUserId(receiver)
                            .setCountyAgentUserId(sender).setRegionCode(transfer.getRegionCode()).setProductId(item.getProductId())
                            .setSkuId(item.getSkuId()).setQuantity(item.getQuantity()).setBaseAmount(revenue).setPerformanceAmount(revenue)
                            .setRequestNo(requestNo).setRelationSnapshotJson(transferSnapshot(transfer)).setSourceSnapshotJson(JSONUtil.toJsonStr(item))
                            .setActionKey("PERF:STOCK_TRANSFER:" + item.getId() + ":SENDER:" + sender));
                    profitService.record(new JkOperationProfitRecord().setUserId(sender).setRoleCode("county_agent")
                            .setSourceType("STOCK_TRANSFER").setSourceId(id).setSourceNo(no).setSourceItemId(item.getId())
                            .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                            .setRevenueAmount(revenue).setCostAmount(cost.costAmount).setProfitAmount(profit)
                            .setCostSnapshotJson(cost.snapshotJson).setRelationSnapshotJson(transferSnapshot(transfer))
                            .setRequestNo(requestNo).setActionKey("PROFIT:STOCK_TRANSFER:" + item.getId() + ":" + sender));
                }
                JkCommissionRuleTrialRequest scenario = businessScenario("STOCK_TRANSFER_RECEIVED", "STOCK_TRANSFER", id, item.getId(),
                        receiver, sender, null, sender, transfer.getRegionCode(), item.getProductId(), item.getSkuId(),
                        item.getQuantity(), revenue, profit);
                scenario.setRegisteredCustomer(true); scenario.setVoucherPresent(true); scenario.setAudited(true);
                dispatchSafely(scenario, "COMMISSION:STOCK_TRANSFER:" + item.getId(), no, requestNo,
                        "STOCK_TRANSFER_RECEIVED", id);
            } catch (Exception error) {
                item.setProfitStatus("COST_EXCEPTION").setUpdateTime(new Date());
                stockTransferItemDao.updateById(item);
                recordBusinessEvent("STOCK_TRANSFER_COST_EXCEPTION", id, no, requestNo + ":" + item.getId(), "FAILED",
                        "调拨成本缺失或不一致，未生成经营毛利和平台补贴：" + safeMessage(error));
            }
        }
        recordBusinessEvent("STOCK_TRANSFER_RECEIVED", id, no, requestNo, "SUCCESS",
                "调拨业绩与线下经营毛利已分账；平台补贴仅按已发布规则生成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onRefundCompleted(Long id, String no, BigDecimal refundAmount, String requestNo) {
        List<RetailOrderAttributionService.RefundAllocation> allocations = attributionService.allocateRefund(no, refundAmount, requestNo);
        for (RetailOrderAttributionService.RefundAllocation allocation : allocations) {
            JkRetailOrderAttribution attribution = allocation.getAttribution();
            promotionEffectService.recordRetailRefund(attribution,
                    allocation.getRefundBaseAmount(), allocation.getBeforeRefundedAmount(),
                    requestNo, new Date());
            BigDecimal remainingBaseBefore = safe(attribution.getItemPaidAmount())
                    .subtract(safe(allocation.getBeforeRefundedAmount())).max(BigDecimal.ZERO);
            if (remainingBaseBefore.signum() <= 0) continue;
            performanceService.reverse("RETAIL_ORDER", attribution.getOrderId(), attribution.getOrderInfoId(),
                    allocation.getRefundBaseAmount(), requestNo + ":PERF:" + attribution.getOrderInfoId(), "普通零售退款");
            List<JkCommissionRecord> records = recordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                    .eq(JkCommissionRecord::getSourceType, "RETAIL_ORDER")
                    .and(q -> q.eq(JkCommissionRecord::getSourceItemId, attribution.getOrderInfoId())
                            .or().eq(JkCommissionRecord::getSourceId, attribution.getOrderInfoId()))
                    .eq(JkCommissionRecord::getIsDeleted, false));
            for (JkCommissionRecord record : records) {
                BigDecimal remainingCommission = safe(record.getCommissionAmount()).subtract(priorReversed(record.getId())).max(BigDecimal.ZERO);
                if (remainingCommission.signum() <= 0) continue;
                BigDecimal reverseAmount = allocation.getRefundBaseAmount().compareTo(remainingBaseBefore) >= 0
                        ? remainingCommission
                        : remainingCommission.multiply(allocation.getRefundBaseAmount()).divide(remainingBaseBefore, 2, RoundingMode.HALF_UP).min(remainingCommission);
                if (reverseAmount.signum() > 0) reverseService.reverse(record.getId(), "RETAIL_ORDER", id, no, "REFUND", reverseAmount,
                        requestNo + ":" + record.getId(), null, "按原订单实付和规则快照冲正");
            }
        }
        recordBusinessEvent("RETAIL_ORDER_REFUNDED", id, no, requestNo, "SUCCESS",
                "退款已按下单快照冲正业绩、佣金和推广净效果，refundAmount=" + refundAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onTransferReturnCompleted(Long id, String no, String requestNo) {
        JkStockTransferReturn returnOrder = transferReturnDao.selectById(id);
        if (returnOrder == null || !"COMPLETED".equals(returnOrder.getStatus())) throw new IllegalArgumentException("调拨退回单不存在或尚未完成");
        JkStockTransfer transfer = stockTransferDao.selectById(returnOrder.getOriginalTransferId());
        BigDecimal originalAmount = transfer == null ? BigDecimal.ZERO : safe(transfer.getTotalAmount());
        BigDecimal returnAmount = safe(returnOrder.getReturnAmount());
        performanceService.reverse("STOCK_TRANSFER", returnOrder.getOriginalTransferId(), null, returnAmount,
                requestNo + ":PERFORMANCE", "调拨退回");
        profitService.reverse("STOCK_TRANSFER", returnOrder.getOriginalTransferId(), null, returnAmount,
                requestNo + ":PROFIT", "调拨退回");
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
            if (reverseAmount.signum() > 0) reverseService.reverse(record.getId(), "STOCK_TRANSFER_RETURN", id, no,
                    "TRANSFER_RETURN", reverseAmount, requestNo + ":" + record.getId(), null, "按原调拨规则快照比例冲正");
        }
        recordBusinessEvent("STOCK_TRANSFER_RETURNED", id, no, requestNo, "SUCCESS",
                "调拨退回已分别冲正业绩、经营收益和平台佣金");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onIdentityFrozen(Long userId, String requestNo) {
        for (JkCommissionAccount account : commissionAccountDao.selectList(new LambdaQueryWrapper<JkCommissionAccount>()
                .eq(JkCommissionAccount::getUserId, userId).eq(JkCommissionAccount::getIsDeleted, false))) {
            BigDecimal settled = safe(account.getSettledAmount());
            if (settled.signum() > 0) freezeService.freezeCommission(userId, account.getRoleCode(), settled, "IDENTITY_FROZEN", "IDENTITY", userId,
                    requestNo, "IDENTITY_COMMISSION_FREEZE:" + userId + ":" + account.getRoleCode() + ":" + requestNo, "身份冻结收益");
        }
        for (JkFundAccount account : fundAccountDao.selectList(new LambdaQueryWrapper<JkFundAccount>()
                .eq(JkFundAccount::getUserId, userId).eq(JkFundAccount::getIsDeleted, false))) {
            BigDecimal available = safe(account.getAvailableAmount());
            if (available.signum() > 0) fundAccountService.freezeAvailable(userId, account.getRoleCode(), available, "IDENTITY", userId,
                    requestNo, "IDENTITY_FUND_FREEZE:" + userId + ":" + account.getRoleCode() + ":" + requestNo, "身份冻结可提现资金");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onIdentityUnfrozen(Long userId, String requestNo) {
        for (JkCommissionAccount account : commissionAccountDao.selectList(new LambdaQueryWrapper<JkCommissionAccount>()
                .eq(JkCommissionAccount::getUserId, userId).eq(JkCommissionAccount::getIsDeleted, false))) {
            BigDecimal frozen = safe(account.getFrozenCommissionAmount());
            if (frozen.signum() > 0) freezeService.releaseCommission(userId, account.getRoleCode(), frozen, "IDENTITY_UNFROZEN", "IDENTITY", userId,
                    requestNo, "IDENTITY_COMMISSION_UNFREEZE:" + userId + ":" + account.getRoleCode() + ":" + requestNo, "身份解冻收益");
        }
        for (JkFundAccount account : fundAccountDao.selectList(new LambdaQueryWrapper<JkFundAccount>()
                .eq(JkFundAccount::getUserId, userId).eq(JkFundAccount::getIsDeleted, false))) {
            BigDecimal identityFrozen = safe(account.getFrozenAmount()).subtract(safe(account.getWithdrawingAmount())).max(BigDecimal.ZERO);
            if (identityFrozen.signum() > 0) fundAccountService.releaseIdentityFrozen(userId, account.getRoleCode(), identityFrozen, "IDENTITY", userId,
                    requestNo, "IDENTITY_FUND_UNFREEZE:" + userId + ":" + account.getRoleCode() + ":" + requestNo, "身份解冻可提现资金");
        }
    }

    private JkRetailOrderAttribution findAttribution(Long orderId, String orderNo, Long orderInfoId) {
        for (JkRetailOrderAttribution row : attributionService.listByOrder(orderId, orderNo)) {
            if (orderInfoId == null || orderInfoId.equals(row.getOrderInfoId())) return row;
        }
        return null;
    }

    private TransferCost transferCost(JkStockTransfer transfer, JkStockTransferItem item) {
        LambdaQueryWrapper<JkStockBatchReservation> query = new LambdaQueryWrapper<JkStockBatchReservation>()
                .eq(JkStockBatchReservation::getBusinessType, "STOCK_TRANSFER").eq(JkStockBatchReservation::getBusinessId, transfer.getId())
                .eq(JkStockBatchReservation::getStockAccountId, item.getFromStockAccountId()).eq(JkStockBatchReservation::getProductId, item.getProductId())
                .eq(JkStockBatchReservation::getSkuId, item.getSkuId()).eq(JkStockBatchReservation::getIsDeleted, false);
        List<JkStockBatchReservation> reservations = reservationDao.selectList(query);
        if (reservations.isEmpty()) throw new IllegalArgumentException("调拨批次预留不存在");
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalQty = 0;
        List<Map<String, Object>> snapshots = new ArrayList<Map<String, Object>>();
        for (JkStockBatchReservation reservation : reservations) {
            JkStockBatch batch = batchDao.selectById(reservation.getBatchId());
            if (batch == null || batch.getUnitCost() == null) throw new IllegalArgumentException("调拨来源批次成本缺失");
            int qty = reservation.getOutboundQty() != null && reservation.getOutboundQty() > 0
                    ? reservation.getOutboundQty() : (reservation.getFrozenQty() == null ? 0 : reservation.getFrozenQty());
            totalQty += qty;
            totalCost = totalCost.add(batch.getUnitCost().multiply(new BigDecimal(qty)));
            Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
            snapshot.put("batchId", batch.getId()); snapshot.put("batchNo", batch.getBatchNo());
            snapshot.put("quantity", qty); snapshot.put("unitCost", batch.getUnitCost()); snapshots.add(snapshot);
        }
        if (totalQty <= 0 || totalQty != item.getQuantity()) throw new IllegalArgumentException("调拨批次出库数量不一致");
        return new TransferCost(totalCost.divide(new BigDecimal(totalQty), 6, RoundingMode.HALF_UP),
                totalCost.setScale(2, RoundingMode.HALF_UP), JSONUtil.toJsonStr(snapshots));
    }

    private JkCommissionRuleTrialRequest businessScenario(String scenario, String sourceType, Long sourceId, Long sourceItemId,
                                                           Long buyer, Long seller, Long parent, Long county, String region,
                                                           Integer productId, Integer skuId, Integer qty, BigDecimal amount, BigDecimal grossProfit) {
        JkCommissionRuleTrialRequest request = new JkCommissionRuleTrialRequest();
        request.setScenario(scenario); request.setSourceType(sourceType); request.setSourceId(sourceId); request.setSourceItemId(sourceItemId);
        request.setBuyerUserId(buyer); request.setSellerUserId(seller); request.setDirectParentUserId(parent); request.setCountyAgentUserId(county);
        request.setRegionCode(region); request.setProductId(productId); request.setSkuId(skuId); request.setQuantity(qty);
        request.setBaseAmount(safe(amount)); request.setRealGrossProfit(grossProfit); return request;
    }

    private void dispatchSafely(JkCommissionRuleTrialRequest scenario, String eventKey, String sourceNo, String requestNo,
                                String eventType, Long businessId) {
        try { scenarioService.dispatch(scenario, eventKey, sourceNo, requestNo); }
        catch (Exception error) {
            recordBusinessEvent(eventType + "_COMMISSION", businessId, sourceNo, requestNo, "FAILED",
                    "佣金计算失败，业务和业绩不回滚，进入事件补偿：" + safeMessage(error));
        }
    }

    private void recordBusinessEvent(String type, Long id, String no, String requestNo, String status, String remark) {
        String eventKey = type + ":" + id + ":" + (requestNo == null ? "" : requestNo);
        if (businessEventDao.selectOne(new LambdaQueryWrapper<JkBusinessEvent>().eq(JkBusinessEvent::getEventKey, eventKey).last("limit 1")) != null) return;
        Date now = new Date();
        try {
            businessEventDao.insert(new JkBusinessEvent().setEventKey(eventKey).setEventType(type).setBusinessId(id)
                    .setBusinessNo(no).setPayloadJson(remark).setEventStatus(status).setRetryCount(0).setMaxRetryCount(8)
                    .setOccurredTime(now).setProcessedTime("SUCCESS".equals(status) ? now : null).setCreateTime(now).setUpdateTime(now));
        } catch (DuplicateKeyException ignored) { }
    }

    private String retailSourceSnapshot(JkRetailOrderAttribution attribution) {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("orderId", attribution.getOrderId()); value.put("orderInfoId", attribution.getOrderInfoId());
        value.put("buyerUserId", attribution.getBuyerUserId()); value.put("itemPaidAmount", attribution.getItemPaidAmount());
        value.put("refundedAmount", attribution.getRefundedAmount()); return JSONUtil.toJsonStr(value);
    }
    private String platformSnapshot(JkPlatformOrder order) { return JSONUtil.toJsonStr(order); }
    private String transferSnapshot(JkStockTransfer transfer) { return JSONUtil.toJsonStr(transfer); }
    private BigDecimal priorReversed(Long recordId) { BigDecimal total = BigDecimal.ZERO; for (JkCommissionReverse reverse : reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>().eq(JkCommissionReverse::getOriginalCommissionRecordId, recordId).eq(JkCommissionReverse::getStatus, "SUCCESS"))) total = total.add(safe(reverse.getReverseAmount())); return total; }
    private BigDecimal safe(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }
    private String safeMessage(Exception error) { String value = error.getMessage(); return value == null ? error.getClass().getSimpleName() : value.replace('\n', ' ').replace('\r', ' '); }

    private static class TransferCost {
        private final BigDecimal unitCost; private final BigDecimal costAmount; private final String snapshotJson;
        private TransferCost(BigDecimal unitCost, BigDecimal costAmount, String snapshotJson) { this.unitCost = unitCost; this.costAmount = costAmount; this.snapshotJson = snapshotJson; }
    }
}
