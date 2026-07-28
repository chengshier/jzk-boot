package com.zbkj.service.service.impl.jiuzhoukang.trade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrderItem;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.model.jiuzhoukang.JkStockBatchFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferItemDao;
import com.zbkj.service.service.impl.jiuzhoukang.commission.JkCommissionV31Service;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkOperationProfitLedgerService;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkPerformanceLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/** 订货、调拨完成后的第二批账本编排。 */
@Service
public class JkTradeLedgerClosureService {
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkPlatformOrderItemDao platformOrderItemDao;
    @Autowired private JkStockTransferDao transferDao;
    @Autowired private JkStockTransferItemDao transferItemDao;
    @Autowired private JkStockBatchFlowDao batchFlowDao;
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private JkPerformanceLedgerService performanceService;
    @Autowired private JkOperationProfitLedgerService profitService;
    @Autowired private JkCommissionV31Service commissionService;

    @Transactional(rollbackFor = Exception.class)
    public void platformOrderReceived(Long orderId, String requestNo) {
        JkPlatformOrder order = platformOrderDao.selectById(orderId);
        if (order == null || !"STOCK_IN".equals(order.getStatus())) throw new CrmebException("平台订货尚未完成入库");
        List<JkPlatformOrderItem> items = platformOrderItemDao.selectList(new LambdaQueryWrapper<JkPlatformOrderItem>()
                .eq(JkPlatformOrderItem::getPlatformOrderId, orderId).eq(JkPlatformOrderItem::getIsDeleted, false));
        for (JkPlatformOrderItem item : items) {
            String snapshot = sourceSnapshot(order.getPlatformOrderNo(), item.getProductId(), item.getSkuId(), item.getQuantity(), item.getTotalAmount());
            performanceService.record(new JkPerformanceRecord().setSourceType("PLATFORM_ORDER").setSourceId(orderId)
                    .setSourceNo(order.getPlatformOrderNo()).setSourceItemId(item.getId()).setPerformanceType("PLATFORM_PURCHASE")
                    .setOwnerUserId(order.getUserId()).setOwnerRoleCode(order.getRoleCode()).setSourceUserId(order.getUserId())
                    .setCountyAgentUserId(order.getCountyAgentId()).setRegionCode(order.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setBaseAmount(item.getTotalAmount()).setPerformanceAmount(item.getTotalAmount())
                    .setRequestNo("PERFORMANCE:PLATFORM_ORDER:" + orderId + ":" + item.getId())
                    .setSourceSnapshotJson(snapshot));
            commissionService.createForScenario(new JkCommissionRuleTrialRequest()
                    .setSourceType("PLATFORM_ORDER").setSourceId(orderId).setSourceItemId(item.getId()).setSourceNo(order.getPlatformOrderNo())
                    .setOwnerUserId(order.getUserId()).setOwnerRoleCode(order.getRoleCode()).setPurchaserUserId(order.getUserId())
                    .setCountyAgentUserId(order.getCountyAgentId()).setRegionCode(order.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setBaseAmount(item.getTotalAmount()).setRegisteredCustomer(true).setVoucherPresent(true).setAudited(true)
                    .setSourceSnapshotJson(snapshot), requestNo + ":ITEM:" + item.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockTransferReceived(Long transferId, String requestNo) {
        JkStockTransfer transfer = transferDao.selectById(transferId);
        if (transfer == null || !"STOCK_IN".equals(transfer.getStatus())) throw new CrmebException("库存调拨尚未完成入库");
        List<JkStockTransferItem> items = transferItemDao.selectList(new LambdaQueryWrapper<JkStockTransferItem>()
                .eq(JkStockTransferItem::getTransferId, transferId).eq(JkStockTransferItem::getIsDeleted, false));
        for (JkStockTransferItem item : items) {
            CostSnapshot cost = sourceCost("STOCK_TRANSFER", transferId, item.getProductId(), item.getSkuId());
            if (cost.quantity < item.getQuantity() || cost.unitCost == null) {
                item.setReceivedQty(item.getQuantity()).setProfitStatus("COST_MISSING")
                        .setCostMethod("FIFO_BATCH").setCostSnapshotJson(cost.json).setUpdateTime(new Date());
                transferItemDao.updateById(item);
                throw new CrmebException("调拨来源批次成本缺失，已阻止经营毛利生成");
            }
            BigDecimal costAmount = cost.unitCost.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal spread = item.getTotalAmount().subtract(costAmount).setScale(2, RoundingMode.HALF_UP);
            item.setReceivedQty(item.getQuantity()).setSourceUnitCost(cost.unitCost).setCostAmount(costAmount)
                    .setUnitSpread(item.getUnitPrice().subtract(cost.unitCost).setScale(2, RoundingMode.HALF_UP))
                    .setSpreadAmount(spread).setCostMethod("FIFO_BATCH").setCostSnapshotJson(cost.json)
                    .setProfitStatus("CONFIRMED").setUpdateTime(new Date());
            transferItemDao.updateById(item);
            String snapshot = sourceSnapshot(transfer.getTransferNo(), item.getProductId(), item.getSkuId(), item.getQuantity(), item.getTotalAmount());
            performanceService.record(new JkPerformanceRecord().setSourceType("STOCK_TRANSFER").setSourceId(transferId)
                    .setSourceNo(transfer.getTransferNo()).setSourceItemId(item.getId()).setPerformanceType("STOCK_TRANSFER")
                    .setOwnerUserId(transfer.getCountyAgentId()).setOwnerRoleCode("county_agent").setSourceUserId(transfer.getUserId())
                    .setSourceRoleCode(transfer.getRoleCode()).setCountyAgentUserId(transfer.getCountyAgentId()).setRegionCode(transfer.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setBaseAmount(item.getTotalAmount()).setPerformanceAmount(item.getTotalAmount())
                    .setRequestNo("PERFORMANCE:STOCK_TRANSFER:" + transferId + ":" + item.getId()).setSourceSnapshotJson(snapshot));
            performanceService.record(new JkPerformanceRecord().setSourceType("STOCK_TRANSFER").setSourceId(transferId)
                    .setSourceNo(transfer.getTransferNo()).setSourceItemId(item.getId()).setPerformanceType("INVENTORY_TURNOVER")
                    .setOwnerUserId(transfer.getUserId()).setOwnerRoleCode(transfer.getRoleCode()).setSourceUserId(transfer.getCountyAgentId())
                    .setSourceRoleCode("county_agent").setCountyAgentUserId(transfer.getCountyAgentId()).setRegionCode(transfer.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setBaseAmount(item.getTotalAmount()).setPerformanceAmount(BigDecimal.ZERO)
                    .setRequestNo("PERFORMANCE:INVENTORY_TURNOVER:" + transferId + ":" + item.getId()).setSourceSnapshotJson(snapshot));
            profitService.record(new JkOperationProfitRecord().setUserId(transfer.getCountyAgentId()).setRoleCode("county_agent")
                    .setIncomeNature("OFFLINE_REALIZED").setSourceType("STOCK_TRANSFER").setSourceId(transferId)
                    .setSourceNo(transfer.getTransferNo()).setSourceItemId(item.getId()).setProductId(item.getProductId())
                    .setSkuId(item.getSkuId()).setQuantity(item.getQuantity()).setRevenueAmount(item.getTotalAmount())
                    .setCostAmount(costAmount).setProfitAmount(spread).setCostSnapshotJson(cost.json)
                    .setRequestNo("PROFIT:STOCK_TRANSFER:" + transferId + ":" + item.getId()));
            commissionService.createForScenario(new JkCommissionRuleTrialRequest()
                    .setSourceType("STOCK_TRANSFER").setSourceId(transferId).setSourceItemId(item.getId()).setSourceNo(transfer.getTransferNo())
                    .setOwnerUserId(transfer.getUserId()).setOwnerRoleCode(transfer.getRoleCode())
                    .setSellerUserId(transfer.getCountyAgentId()).setPurchaserUserId(transfer.getUserId())
                    .setCountyAgentUserId(transfer.getCountyAgentId()).setRegionCode(transfer.getRegionCode())
                    .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setQuantity(item.getQuantity())
                    .setBaseAmount(item.getTotalAmount()).setCostAmount(costAmount).setRegisteredCustomer(true)
                    .setVoucherPresent(true).setAudited(true).setSourceSnapshotJson(snapshot), requestNo + ":ITEM:" + item.getId());
        }
    }

    private CostSnapshot sourceCost(String businessType, Long businessId, Integer productId, Integer skuId) {
        LambdaQueryWrapper<JkStockBatchFlow> query = new LambdaQueryWrapper<JkStockBatchFlow>()
                .eq(JkStockBatchFlow::getBusinessType, businessType).eq(JkStockBatchFlow::getBusinessId, businessId)
                .eq(JkStockBatchFlow::getProductId, productId).eq(JkStockBatchFlow::getFlowType, "OUTBOUND")
                .eq(JkStockBatchFlow::getIsDeleted, false).orderByAsc(JkStockBatchFlow::getId);
        if (skuId == null) query.isNull(JkStockBatchFlow::getSkuId); else query.eq(JkStockBatchFlow::getSkuId, skuId);
        int quantity = 0; BigDecimal totalCost = BigDecimal.ZERO; StringBuilder json = new StringBuilder("["); boolean first = true;
        for (JkStockBatchFlow flow : batchFlowDao.selectList(query)) {
            JkStockBatch batch = batchDao.selectById(flow.getBatchId());
            if (batch == null || batch.getUnitCost() == null) return new CostSnapshot(null, quantity, "{\"error\":\"COST_MISSING\"}");
            quantity += flow.getChangeQty();
            totalCost = totalCost.add(batch.getUnitCost().multiply(BigDecimal.valueOf(flow.getChangeQty())));
            if (!first) json.append(','); first = false;
            json.append("{\"batchId\":").append(batch.getId()).append(",\"batchNo\":\"").append(escape(batch.getBatchNo()))
                    .append("\",\"qty\":").append(flow.getChangeQty()).append(",\"unitCost\":").append(batch.getUnitCost()).append('}');
        }
        json.append(']');
        BigDecimal unitCost = quantity == 0 ? null : totalCost.divide(BigDecimal.valueOf(quantity), 6, RoundingMode.HALF_UP);
        return new CostSnapshot(unitCost, quantity, json.toString());
    }

    private String sourceSnapshot(String no, Integer productId, Integer skuId, Integer quantity, BigDecimal amount) {
        return "{\"sourceNo\":\"" + escape(no) + "\",\"productId\":" + productId + ",\"skuId\":"
                + (skuId == null ? "null" : skuId) + ",\"quantity\":" + quantity + ",\"amount\":" + amount + "}";
    }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static final class CostSnapshot {
        private final BigDecimal unitCost; private final int quantity; private final String json;
        private CostSnapshot(BigDecimal unitCost, int quantity, String json) { this.unitCost = unitCost; this.quantity = quantity; this.json = json; }
    }
}
