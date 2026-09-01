package com.zbkj.service.service.impl.jiuzhoukang.trade;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrderItem;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.model.jiuzhoukang.JkStockBatchFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveExceptionItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.request.jiuzhoukang.JkReceiveExceptionConfirmRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeReceiveExceptionHandleRequest;
import com.zbkj.common.response.jiuzhoukang.JkTradeReceiveExceptionDetailResponse;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessEventDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkTradeReceiveExceptionDao;
import com.zbkj.service.dao.jiuzhoukang.JkTradeReceiveExceptionItemDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 异常收货 V2：方案、双向确认、部分入库和账本同步。
 * 退款/赔付属于线下履约事实，只记录金额和待办，不直接变更平台资金账户。
 */
@Service
public class JkReceiveExceptionV2Service {
    private static final String PLATFORM_ORDER = "PLATFORM_ORDER";
    private static final String STOCK_TRANSFER = "STOCK_TRANSFER";
    private static final List<String> RESOLUTIONS = Arrays.asList(
            "RETRY_RECEIVE", "PARTIAL_RECEIVE", "REFUND", "COMPENSATION", "REFUND_AND_COMPENSATION", "REJECT_ALL");

    @Autowired private JkTradeReceiveExceptionDao exceptionDao;
    @Autowired private JkTradeReceiveExceptionItemDao exceptionItemDao;
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkPlatformOrderItemDao platformOrderItemDao;
    @Autowired private JkStockTransferDao transferDao;
    @Autowired private JkStockTransferItemDao transferItemDao;
    @Autowired private StockFlowService stockFlowService;
    @Autowired private JkStockBatchFlowDao batchFlowDao;
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private JkPerformanceService performanceService;
    @Autowired private JkOperationProfitService profitService;
    @Autowired private CommissionScenarioService commissionService;
    @Autowired private JkBusinessEventDao eventDao;

    @Transactional(rollbackFor = Exception.class)
    public JkTradeReceiveExceptionDetailResponse propose(Long operatorId, JkTradeReceiveExceptionHandleRequest request) {
        JkTradeReceiveException entity = require(request.getExceptionId());
        if (!("PENDING".equals(entity.getStatus()) || "PROCESSING".equals(entity.getStatus()))) {
            throw new CrmebException("当前异常状态不能提出处理方案");
        }
        String resolution = StrUtil.blankToDefault(request.getResolutionType(), "").trim().toUpperCase();
        if (!RESOLUTIONS.contains(resolution)) throw new CrmebException("不支持的异常处理方案");
        BigDecimal refund = money(request.getRefundAmount());
        BigDecimal compensation = money(request.getCompensationAmount());
        if (refund.signum() < 0 || compensation.signum() < 0) throw new CrmebException("退款和赔付金额不能为负数");
        if ("REFUND".equals(resolution) && refund.signum() <= 0) throw new CrmebException("退款方案必须填写退款金额");
        if ("COMPENSATION".equals(resolution) && compensation.signum() <= 0) throw new CrmebException("赔付方案必须填写赔付金额");
        if ("REFUND_AND_COMPENSATION".equals(resolution) && refund.add(compensation).signum() <= 0) {
            throw new CrmebException("退款并赔付方案必须填写金额");
        }
        int normalQty = normalReceivedQty(entity.getId());
        int exceptionQty = Math.max(0, nvl(entity.getExpectedTotalQty()) - normalQty);
        Date now = new Date();
        if ("RETRY_RECEIVE".equals(resolution) || "REJECT_ALL".equals(resolution)) {
            restoreForRetry(entity, operatorId);
            entity.setStatus("RESOLVED").setResolutionType(resolution).setNormalReceivedQty(0)
                    .setExceptionQty(nvl(entity.getExpectedTotalQty())).setRefundAmount(refund).setCompensationAmount(compensation)
                    .setReceiverConfirmStatus("NOT_REQUIRED").setSenderConfirmStatus("CONFIRMED")
                    .setResolutionSnapshotJson(snapshot(resolution, 0, exceptionQty, refund, compensation, request.getRemark()))
                    .setHandleAction("RESOLUTION_APPLIED").setHandleRemark(request.getRemark()).setHandleUserId(operatorId)
                    .setHandleTime(now).setUpdateUserId(operatorId).setUpdateTime(now);
            exceptionDao.updateById(entity);
            event(entity, "RECEIVE_EXCEPTION_RETRY", "已恢复原待收货状态，未产生库存或资金变更");
            return detail(entity);
        }
        entity.setStatus("WAITING_CONFIRM").setResolutionType(resolution).setNormalReceivedQty(normalQty)
                .setExceptionQty(exceptionQty).setRefundAmount(refund).setCompensationAmount(compensation)
                .setReceiverConfirmStatus("PENDING")
                .setSenderConfirmStatus(PLATFORM_ORDER.equals(entity.getBusinessType()) ? "CONFIRMED" : "PENDING")
                .setResolutionSnapshotJson(snapshot(resolution, normalQty, exceptionQty, refund, compensation, request.getRemark()))
                .setHandleAction("PROPOSE_RESOLUTION").setHandleRemark(request.getRemark()).setHandleUserId(operatorId)
                .setHandleTime(now).setUpdateUserId(operatorId).setUpdateTime(now);
        exceptionDao.updateById(entity);
        event(entity, "RECEIVE_EXCEPTION_RESOLUTION_PROPOSED", "等待收货方和发货方确认");
        return detail(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public JkTradeReceiveExceptionDetailResponse confirmReceiver(Long userId, JkReceiveExceptionConfirmRequest request) {
        JkTradeReceiveException entity = require(request.getExceptionId());
        if (!userId.equals(entity.getReceiverUserId())) throw new CrmebException("无权确认该处理方案");
        ensureWaiting(entity);
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            entity.setStatus("PROCESSING").setReceiverConfirmStatus("REJECTED")
                    .setHandleRemark(request.getRemark()).setUpdateUserId(userId).setUpdateTime(new Date());
            exceptionDao.updateById(entity);
            event(entity, "RECEIVE_EXCEPTION_RESOLUTION_REJECTED", "收货方拒绝方案：" + request.getRemark());
            return detail(entity);
        }
        entity.setReceiverConfirmStatus("CONFIRMED").setUpdateUserId(userId).setUpdateTime(new Date());
        exceptionDao.updateById(entity);
        return tryFinalize(entity, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public JkTradeReceiveExceptionDetailResponse confirmSender(Long userId, JkReceiveExceptionConfirmRequest request) {
        JkTradeReceiveException entity = require(request.getExceptionId());
        ensureWaiting(entity);
        if (PLATFORM_ORDER.equals(entity.getBusinessType())) throw new CrmebException("平台订货发货方由后台提出方案时已确认");
        JkStockTransfer transfer = transferDao.selectById(entity.getBusinessId());
        if (transfer == null || !userId.equals(transfer.getCountyAgentId())) throw new CrmebException("无权代表发货方确认方案");
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            entity.setStatus("PROCESSING").setSenderConfirmStatus("REJECTED")
                    .setHandleRemark(request.getRemark()).setUpdateUserId(userId).setUpdateTime(new Date());
            exceptionDao.updateById(entity);
            event(entity, "RECEIVE_EXCEPTION_RESOLUTION_REJECTED", "发货方拒绝方案：" + request.getRemark());
            return detail(entity);
        }
        entity.setSenderConfirmStatus("CONFIRMED").setUpdateUserId(userId).setUpdateTime(new Date());
        exceptionDao.updateById(entity);
        return tryFinalize(entity, userId);
    }

    private JkTradeReceiveExceptionDetailResponse tryFinalize(JkTradeReceiveException entity, Long operatorId) {
        entity = require(entity.getId());
        if (!("CONFIRMED".equals(entity.getReceiverConfirmStatus()) && "CONFIRMED".equals(entity.getSenderConfirmStatus()))) {
            return detail(entity);
        }
        List<JkTradeReceiveExceptionItem> exceptionItems = exceptionItemDao.selectList(new LambdaQueryWrapper<JkTradeReceiveExceptionItem>()
                .eq(JkTradeReceiveExceptionItem::getExceptionId, entity.getId())
                .eq(JkTradeReceiveExceptionItem::getIsDeleted, false).orderByAsc(JkTradeReceiveExceptionItem::getId));
        if (PLATFORM_ORDER.equals(entity.getBusinessType())) finalizePlatformOrder(entity, exceptionItems, operatorId);
        else finalizeTransfer(entity, exceptionItems, operatorId);
        Date now = new Date();
        entity.setStatus("RESOLVED").setHandleAction("RESOLUTION_APPLIED").setHandleTime(now)
                .setUpdateUserId(operatorId).setUpdateTime(now);
        exceptionDao.updateById(entity);
        event(entity, "RECEIVE_EXCEPTION_RESOLVED_V2",
                "双方已确认，正常实收数量已入库；退款=" + money(entity.getRefundAmount()) + "，赔付=" + money(entity.getCompensationAmount()) + "，待线下履约核销");
        return detail(entity);
    }

    private void finalizePlatformOrder(JkTradeReceiveException entity, List<JkTradeReceiveExceptionItem> rows, Long operatorId) {
        JkPlatformOrder order = platformOrderDao.selectById(entity.getBusinessId());
        if (order == null || !"RECEIVE_EXCEPTION".equals(order.getStatus())) throw new CrmebException("原订货单状态已变化");
        for (JkTradeReceiveExceptionItem row : rows) {
            int qty = normalQty(row);
            if (qty <= 0) continue;
            JkPlatformOrderItem item = platformOrderItemDao.selectById(row.getBusinessItemId());
            if (item == null || !order.getId().equals(item.getPlatformOrderId())) throw new CrmebException("订货商品明细已变化");
            stockFlowService.inboundStock(action(entity, item.getToStockAccountId(), item.getProductId(), item.getSkuId(), item.getSkuCode(), qty, operatorId));
            BigDecimal amount = money(item.getUnitPrice()).multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            String performanceRequestNo = "PERFORMANCE:RECEIVE_EXCEPTION:" + entity.getId() + ":" + item.getId();
            performanceService.record(new JkPerformanceRecord().setSourceType("PLATFORM_ORDER").setSourceId(order.getId())
                    .setSourceNo(order.getPlatformOrderNo()).setSourceItemId(item.getId()).setPerformanceType("PLATFORM_PURCHASE")
                    .setOwnerUserId(order.getUserId()).setOwnerRoleCode(order.getRoleCode()).setCountyAgentUserId(order.getCountyAgentId())
                    .setRegionCode(order.getRegionCode()).setProductId(item.getProductId()).setSkuId(item.getSkuId())
                    .setQuantity(qty).setBaseAmount(amount).setPerformanceAmount(amount)
                    .setRequestNo(performanceRequestNo).setActionKey(performanceRequestNo)
                    .setSourceSnapshotJson(entity.getResolutionSnapshotJson()));
            String commissionRequestNo = "COMMISSION:RECEIVE_EXCEPTION:" + entity.getId() + ":" + item.getId();
            JkCommissionRuleTrialRequest scenario = new JkCommissionRuleTrialRequest().setScenario("PLATFORM_ORDER_RECEIVED")
                    .setSourceType("PLATFORM_ORDER").setSourceId(order.getId()).setSourceItemId(item.getId()).setSourceNo(order.getPlatformOrderNo())
                    .setBuyerUserId(order.getUserId()).setPurchaserUserId(order.getUserId()).setSellerUserId(order.getUserId())
                    .setOwnerUserId(order.getUserId()).setCountyAgentUserId(order.getCountyAgentId()).setRegionCode(order.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(qty).setBaseAmount(amount)
                    .setRegisteredCustomer(true).setVoucherPresent(true).setAudited(true)
                    .setRelationSnapshotJson(entity.getResolutionSnapshotJson()).setSourceSnapshotJson(entity.getResolutionSnapshotJson())
                    .setBusinessTime(new Date());
            commissionService.dispatch(scenario, commissionRequestNo, order.getPlatformOrderNo(), commissionRequestNo);
        }
        int updated = platformOrderDao.update(null, new UpdateWrapper<JkPlatformOrder>()
                .eq("id", order.getId()).eq("status", "RECEIVE_EXCEPTION").eq("is_deleted", false)
                .set("status", "STOCK_IN").set("receive_status", "PARTIAL_RESOLVED")
                .set("receive_time", new Date()).set("update_user_id", operatorId).set("update_time", new Date()));
        if (updated != 1) throw new CrmebException("订货单异常处理并发冲突");
    }

    private void finalizeTransfer(JkTradeReceiveException entity, List<JkTradeReceiveExceptionItem> rows, Long operatorId) {
        JkStockTransfer transfer = transferDao.selectById(entity.getBusinessId());
        if (transfer == null || !"RECEIVE_EXCEPTION".equals(transfer.getStatus())) throw new CrmebException("原调拨单状态已变化");
        for (JkTradeReceiveExceptionItem row : rows) {
            int qty = normalQty(row);
            if (qty <= 0) continue;
            JkStockTransferItem item = transferItemDao.selectById(row.getBusinessItemId());
            if (item == null || !transfer.getId().equals(item.getTransferId())) throw new CrmebException("调拨商品明细已变化");
            stockFlowService.inboundStock(action(entity, item.getToStockAccountId(), item.getProductId(), item.getSkuId(), item.getSkuCode(), qty, operatorId));
            CostSnapshot cost = sourceCost(transfer.getId(), item.getProductId(), item.getSkuId(), qty);
            BigDecimal revenue = money(item.getUnitPrice()).multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal costAmount = cost.unitCost == null ? BigDecimal.ZERO : cost.unitCost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal spread = cost.unitCost == null ? BigDecimal.ZERO : revenue.subtract(costAmount).setScale(2, RoundingMode.HALF_UP);
            item.setReceivedQty(qty).setSourceUnitCost(cost.unitCost).setCostAmount(costAmount)
                    .setUnitSpread(cost.unitCost == null ? null : item.getUnitPrice().subtract(cost.unitCost).setScale(2, RoundingMode.HALF_UP))
                    .setSpreadAmount(spread).setCostMethod("FIFO_BATCH").setCostSnapshotJson(cost.json)
                    .setProfitStatus(cost.unitCost == null ? "COST_MISSING" : "CONFIRMED").setUpdateTime(new Date());
            transferItemDao.updateById(item);
            String performanceRequestNo = "PERFORMANCE:RECEIVE_EXCEPTION:" + entity.getId() + ":" + item.getId();
            performanceService.record(new JkPerformanceRecord().setSourceType("STOCK_TRANSFER").setSourceId(transfer.getId())
                    .setSourceNo(transfer.getTransferNo()).setSourceItemId(item.getId()).setPerformanceType("STOCK_TRANSFER")
                    .setOwnerUserId(transfer.getCountyAgentId()).setOwnerRoleCode("county_agent").setSourceUserId(transfer.getUserId())
                    .setSourceRoleCode(transfer.getRoleCode()).setCountyAgentUserId(transfer.getCountyAgentId())
                    .setRegionCode(transfer.getRegionCode()).setProductId(item.getProductId()).setSkuId(item.getSkuId())
                    .setQuantity(qty).setBaseAmount(revenue).setPerformanceAmount(revenue)
                    .setRequestNo(performanceRequestNo).setActionKey(performanceRequestNo)
                    .setSourceSnapshotJson(entity.getResolutionSnapshotJson()));
            if (cost.unitCost != null) {
                String profitRequestNo = "PROFIT:RECEIVE_EXCEPTION:" + entity.getId() + ":" + item.getId();
                profitService.record(new JkOperationProfitRecord().setUserId(transfer.getCountyAgentId()).setRoleCode("county_agent")
                        .setIncomeNature("OFFLINE_REALIZED").setSourceType("STOCK_TRANSFER").setSourceId(transfer.getId())
                        .setSourceNo(transfer.getTransferNo()).setSourceItemId(item.getId()).setProductId(item.getProductId())
                        .setSkuId(item.getSkuId()).setQuantity(qty).setRevenueAmount(revenue).setCostAmount(costAmount)
                        .setProfitAmount(spread).setCostSnapshotJson(cost.json).setRelationSnapshotJson(entity.getResolutionSnapshotJson())
                        .setRequestNo(profitRequestNo).setActionKey(profitRequestNo));
                String commissionRequestNo = "COMMISSION:RECEIVE_EXCEPTION:" + entity.getId() + ":" + item.getId();
                JkCommissionRuleTrialRequest scenario = new JkCommissionRuleTrialRequest().setScenario("STOCK_TRANSFER_RECEIVED")
                        .setSourceType("STOCK_TRANSFER").setSourceId(transfer.getId()).setSourceItemId(item.getId()).setSourceNo(transfer.getTransferNo())
                        .setBuyerUserId(transfer.getUserId()).setPurchaserUserId(transfer.getUserId()).setOwnerUserId(transfer.getUserId())
                        .setSellerUserId(transfer.getCountyAgentId()).setCountyAgentUserId(transfer.getCountyAgentId()).setRegionCode(transfer.getRegionCode())
                        .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(qty)
                        .setBaseAmount(revenue).setCostAmount(costAmount).setRealGrossProfit(spread)
                        .setRegisteredCustomer(true).setVoucherPresent(true).setAudited(true)
                        .setRelationSnapshotJson(entity.getResolutionSnapshotJson()).setSourceSnapshotJson(entity.getResolutionSnapshotJson())
                        .setBusinessTime(new Date());
                commissionService.dispatch(scenario, commissionRequestNo, transfer.getTransferNo(), commissionRequestNo);
            }
        }
        int updated = transferDao.update(null, new UpdateWrapper<JkStockTransfer>()
                .eq("id", transfer.getId()).eq("status", "RECEIVE_EXCEPTION").eq("is_deleted", false)
                .set("status", "STOCK_IN").set("receive_status", "PARTIAL_RESOLVED")
                .set("receive_time", new Date()).set("update_user_id", operatorId).set("update_time", new Date()));
        if (updated != 1) throw new CrmebException("调拨单异常处理并发冲突");
    }

    private void restoreForRetry(JkTradeReceiveException entity, Long operatorId) {
        String original = PLATFORM_ORDER.equals(entity.getBusinessType()) ? "SHIPPED" : "TRANSFERRED";
        int updated = PLATFORM_ORDER.equals(entity.getBusinessType())
                ? platformOrderDao.update(null, new UpdateWrapper<JkPlatformOrder>().eq("id", entity.getBusinessId())
                    .eq("status", "RECEIVE_EXCEPTION").eq("is_deleted", false).set("status", original)
                    .set("receive_status", "UNRECEIVED").set("update_user_id", operatorId).set("update_time", new Date()))
                : transferDao.update(null, new UpdateWrapper<JkStockTransfer>().eq("id", entity.getBusinessId())
                    .eq("status", "RECEIVE_EXCEPTION").eq("is_deleted", false).set("status", original)
                    .set("receive_status", "UNRECEIVED").set("update_user_id", operatorId).set("update_time", new Date()));
        if (updated != 1) throw new CrmebException("原业务单状态已变化，不能恢复收货");
    }

    private JkStockActionRequest action(JkTradeReceiveException entity, Long accountId, Integer productId, Integer skuId,
                                         String skuCode, int qty, Long operatorId) {
        if (accountId == null) throw new CrmebException("目标库存账户缺失");
        return new JkStockActionRequest().setBusinessType("RECEIVE_EXCEPTION_RESOLUTION").setBusinessId(entity.getId())
                .setBusinessNo(entity.getExceptionNo()).setStockAccountId(accountId).setProductId(productId).setSkuId(skuId)
                .setSkuCode(skuCode).setQuantity(qty).setOperatorUserId(operatorId).setRemark("异常收货 V2 正常实收部分入库")
                .setBatchNo("RECV-EX-" + entity.getExceptionNo());
    }

    private CostSnapshot sourceCost(Long transferId, Integer productId, Integer skuId, int requiredQty) {
        LambdaQueryWrapper<JkStockBatchFlow> query = new LambdaQueryWrapper<JkStockBatchFlow>()
                .eq(JkStockBatchFlow::getBusinessType, STOCK_TRANSFER).eq(JkStockBatchFlow::getBusinessId, transferId)
                .eq(JkStockBatchFlow::getProductId, productId).eq(JkStockBatchFlow::getFlowType, "OUTBOUND")
                .eq(JkStockBatchFlow::getIsDeleted, false).orderByAsc(JkStockBatchFlow::getId);
        if (skuId == null) query.isNull(JkStockBatchFlow::getSkuId); else query.eq(JkStockBatchFlow::getSkuId, skuId);
        int qty = 0; BigDecimal total = BigDecimal.ZERO; StringBuilder json = new StringBuilder("["); boolean first = true;
        for (JkStockBatchFlow flow : batchFlowDao.selectList(query)) {
            if (qty >= requiredQty) break;
            JkStockBatch batch = batchDao.selectById(flow.getBatchId());
            if (batch == null || batch.getUnitCost() == null) return new CostSnapshot(null, "{\"error\":\"COST_MISSING\"}");
            int consume = Math.min(flow.getChangeQty(), requiredQty - qty);
            qty += consume; total = total.add(batch.getUnitCost().multiply(BigDecimal.valueOf(consume)));
            if (!first) json.append(','); first = false;
            json.append("{\"batchId\":").append(batch.getId()).append(",\"qty\":").append(consume)
                    .append(",\"unitCost\":").append(batch.getUnitCost()).append('}');
        }
        json.append(']');
        if (qty < requiredQty) return new CostSnapshot(null, "{\"error\":\"COST_QUANTITY_INSUFFICIENT\"}");
        return new CostSnapshot(total.divide(BigDecimal.valueOf(qty), 6, RoundingMode.HALF_UP), json.toString());
    }

    private void ensureWaiting(JkTradeReceiveException entity) {
        if (!"WAITING_CONFIRM".equals(entity.getStatus())) throw new CrmebException("当前异常没有待确认处理方案");
    }
    private int normalReceivedQty(Long exceptionId) { int total = 0; for (JkTradeReceiveExceptionItem row : exceptionItemDao.selectList(new LambdaQueryWrapper<JkTradeReceiveExceptionItem>().eq(JkTradeReceiveExceptionItem::getExceptionId, exceptionId).eq(JkTradeReceiveExceptionItem::getIsDeleted, false))) total += normalQty(row); return total; }
    private int normalQty(JkTradeReceiveExceptionItem row) { return Math.max(0, nvl(row.getReceivedQty()) - nvl(row.getDamagedQty())); }
    private JkTradeReceiveException require(Long id) { JkTradeReceiveException entity = exceptionDao.selectById(id); if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) throw new CrmebException("收货异常记录不存在"); return entity; }
    private JkTradeReceiveExceptionDetailResponse detail(JkTradeReceiveException entity) { return new JkTradeReceiveExceptionDetailResponse().setException(entity).setItems(exceptionItemDao.selectList(new LambdaQueryWrapper<JkTradeReceiveExceptionItem>().eq(JkTradeReceiveExceptionItem::getExceptionId, entity.getId()).eq(JkTradeReceiveExceptionItem::getIsDeleted, false).orderByAsc(JkTradeReceiveExceptionItem::getId))); }
    private void event(JkTradeReceiveException entity, String type, String message) { String key = type + ":" + entity.getId(); if (eventDao.selectOne(new LambdaQueryWrapper<JkBusinessEvent>().eq(JkBusinessEvent::getEventKey, key).last("limit 1")) != null) return; Date now = new Date(); try { eventDao.insert(new JkBusinessEvent().setEventKey(key).setEventType(type).setBusinessId(entity.getId()).setBusinessNo(entity.getExceptionNo()).setPayloadJson(message).setEventStatus("SUCCESS").setRetryCount(0).setMaxRetryCount(8).setOccurredTime(now).setProcessedTime(now).setCreateTime(now).setUpdateTime(now)); } catch (DuplicateKeyException ignored) { } }
    private String snapshot(String type, int normalQty, int exceptionQty, BigDecimal refund, BigDecimal compensation, String remark) { return "{\"resolutionType\":\"" + escape(type) + "\",\"normalReceivedQty\":" + normalQty + ",\"exceptionQty\":" + exceptionQty + ",\"refundAmount\":" + refund + ",\"compensationAmount\":" + compensation + ",\"remark\":\"" + escape(remark) + "\"}"; }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private static final class CostSnapshot { private final BigDecimal unitCost; private final String json; private CostSnapshot(BigDecimal unitCost, String json) { this.unitCost = unitCost; this.json = json; } }
}
