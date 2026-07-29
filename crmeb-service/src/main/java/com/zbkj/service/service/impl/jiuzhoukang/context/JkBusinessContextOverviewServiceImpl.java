package com.zbkj.service.service.impl.jiuzhoukang.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttributionAdjustment;
import com.zbkj.common.model.jiuzhoukang.JkRetailRefundAdjustment;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.service.dao.jiuzhoukang.JkAuditLogDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleDao;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionAdjustmentDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailRefundAdjustmentDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.service.jiuzhoukang.context.JkBusinessContextOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 为统一抽屉提供只读业务解释链。该接口不执行审核、退款、冲正或余额修改。
 */
@Service
public class JkBusinessContextOverviewServiceImpl implements JkBusinessContextOverviewService {
    @Autowired private JkRetailOrderAttributionDao attributionDao;
    @Autowired private JkRetailOrderAttributionAdjustmentDao attributionAdjustmentDao;
    @Autowired private JkRetailRefundAdjustmentDao refundAdjustmentDao;
    @Autowired private JkCommissionRecordDao commissionRecordDao;
    @Autowired private JkPerformanceRecordDao performanceRecordDao;
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkStockTransferDao stockTransferDao;
    @Autowired private JkOfflineSaleDao offlineSaleDao;
    @Autowired private JkAuditLogDao auditLogDao;

    @Override
    public Map<String, Object> overview(String businessType, Long businessId) {
        if (businessType == null || businessId == null) throw new IllegalArgumentException("业务类型和业务ID不能为空");
        String type = businessType.trim().toUpperCase();
        Map<String, Object> result = empty(type, businessId);
        if ("RETAIL_ATTRIBUTION".equals(type)) fillAttribution(result, requireAttribution(businessId));
        else if ("COMMISSION_RECORD".equals(type)) fillCommission(result, requireCommission(businessId));
        else if ("PERFORMANCE_RECORD".equals(type)) fillPerformance(result, requirePerformance(businessId));
        else if ("PLATFORM_ORDER".equals(type)) fillPlatformOrder(result, businessId);
        else if ("STOCK_TRANSFER".equals(type)) fillTransfer(result, businessId);
        else if ("OFFLINE_SALE".equals(type)) fillOfflineSale(result, businessId);
        else throw new IllegalArgumentException("不支持的业务上下文类型");
        result.put("auditLogs", auditLogs(type, businessId));
        return result;
    }

    private void fillAttribution(Map<String, Object> result, JkRetailOrderAttribution row) {
        result.put("primaryBusiness", row);
        result.put("attribution", row);
        result.put("title", "零售订单归属");
        result.put("businessNo", row.getOrderNo());
        result.put("statusText", row.getAttributionStatus());
        result.put("relationSnapshot", row.getRelationSnapshotJson());
        result.put("regionResolutionSnapshot", row.getRegionResolutionSnapshotJson());
        result.put("performance", performanceRecordDao.selectList(new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getSourceType, "RETAIL_ORDER")
                .eq(JkPerformanceRecord::getSourceItemId, row.getOrderInfoId())
                .eq(JkPerformanceRecord::getIsDeleted, false)));
        result.put("commission", commissionRecordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                .eq(JkCommissionRecord::getSourceType, "RETAIL_ORDER")
                .eq(JkCommissionRecord::getSourceItemId, row.getOrderInfoId())
                .eq(JkCommissionRecord::getIsDeleted, false)));
        result.put("refund", refundAdjustmentDao.selectList(new LambdaQueryWrapper<JkRetailRefundAdjustment>()
                .eq(JkRetailRefundAdjustment::getAttributionId, row.getId())
                .eq(JkRetailRefundAdjustment::getIsDeleted, false)
                .orderByDesc(JkRetailRefundAdjustment::getId)));
        result.put("adjustments", attributionAdjustmentDao.selectList(new LambdaQueryWrapper<JkRetailOrderAttributionAdjustment>()
                .eq(JkRetailOrderAttributionAdjustment::getAttributionId, row.getId())
                .orderByDesc(JkRetailOrderAttributionAdjustment::getId)));
    }

    private void fillCommission(Map<String, Object> result, JkCommissionRecord record) {
        result.put("primaryBusiness", record);
        result.put("commission", java.util.Collections.singletonList(record));
        result.put("title", "佣金记录");
        result.put("businessNo", record.getCommissionNo());
        result.put("statusText", record.getCommissionStatus());
        if ("RETAIL_ORDER".equals(record.getSourceType()) && record.getSourceItemId() != null) {
            JkRetailOrderAttribution attribution = attributionDao.selectOne(new LambdaQueryWrapper<JkRetailOrderAttribution>()
                    .eq(JkRetailOrderAttribution::getOrderInfoId, record.getSourceItemId())
                    .eq(JkRetailOrderAttribution::getIsDeleted, false).last("limit 1"));
            if (attribution != null) fillAttributionRelations(result, attribution);
        }
        result.put("performance", performanceRecordDao.selectList(new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getSourceType, record.getSourceType())
                .eq(record.getSourceId() != null, JkPerformanceRecord::getSourceId, record.getSourceId())
                .eq(record.getSourceItemId() != null, JkPerformanceRecord::getSourceItemId, record.getSourceItemId())
                .eq(JkPerformanceRecord::getIsDeleted, false)));
    }

    private void fillPerformance(Map<String, Object> result, JkPerformanceRecord record) {
        result.put("primaryBusiness", record);
        result.put("performance", java.util.Collections.singletonList(record));
        result.put("title", "业绩记录");
        result.put("businessNo", record.getPerformanceNo());
        result.put("statusText", record.getStatus());
        result.put("relationSnapshot", record.getRelationSnapshotJson());
        result.put("commission", commissionRecordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                .eq(JkCommissionRecord::getSourceType, record.getSourceType())
                .eq(record.getSourceId() != null, JkCommissionRecord::getSourceId, record.getSourceId())
                .eq(record.getSourceItemId() != null, JkCommissionRecord::getSourceItemId, record.getSourceItemId())
                .eq(JkCommissionRecord::getIsDeleted, false)));
    }

    private void fillPlatformOrder(Map<String, Object> result, Long id) {
        JkPlatformOrder order = platformOrderDao.selectById(id);
        if (order == null || Boolean.TRUE.equals(order.getIsDeleted())) throw new IllegalArgumentException("平台订货单不存在");
        result.put("primaryBusiness", order);
        result.put("title", "平台订货");
        result.put("businessNo", order.getPlatformOrderNo());
        result.put("statusText", order.getStatus());
        fillSourceRecords(result, "PLATFORM_ORDER", id);
    }

    private void fillTransfer(Map<String, Object> result, Long id) {
        JkStockTransfer transfer = stockTransferDao.selectById(id);
        if (transfer == null || Boolean.TRUE.equals(transfer.getIsDeleted())) throw new IllegalArgumentException("库存调拨单不存在");
        result.put("primaryBusiness", transfer);
        result.put("title", "库存调拨");
        result.put("businessNo", transfer.getTransferNo());
        result.put("statusText", transfer.getStatus());
        fillSourceRecords(result, "STOCK_TRANSFER", id);
    }

    private void fillOfflineSale(Map<String, Object> result, Long id) {
        JkOfflineSale sale = offlineSaleDao.selectById(id);
        if (sale == null || Boolean.TRUE.equals(sale.getIsDeleted())) throw new IllegalArgumentException("线下销售单不存在");
        result.put("primaryBusiness", sale);
        result.put("title", "线下终端销售");
        result.put("businessNo", sale.getSaleNo());
        result.put("statusText", sale.getStatus());
        fillSourceRecords(result, "OFFLINE_SALE", id);
    }

    private void fillSourceRecords(Map<String, Object> result, String sourceType, Long sourceId) {
        result.put("performance", performanceRecordDao.selectList(new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getSourceType, sourceType).eq(JkPerformanceRecord::getSourceId, sourceId)
                .eq(JkPerformanceRecord::getIsDeleted, false)));
        result.put("commission", commissionRecordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                .eq(JkCommissionRecord::getSourceType, sourceType).eq(JkCommissionRecord::getSourceId, sourceId)
                .eq(JkCommissionRecord::getIsDeleted, false)));
    }

    private void fillAttributionRelations(Map<String, Object> result, JkRetailOrderAttribution attribution) {
        result.put("attribution", attribution);
        result.put("relationSnapshot", attribution.getRelationSnapshotJson());
        result.put("regionResolutionSnapshot", attribution.getRegionResolutionSnapshotJson());
        result.put("refund", refundAdjustmentDao.selectList(new LambdaQueryWrapper<JkRetailRefundAdjustment>()
                .eq(JkRetailRefundAdjustment::getAttributionId, attribution.getId())
                .eq(JkRetailRefundAdjustment::getIsDeleted, false)));
        result.put("adjustments", attributionAdjustmentDao.selectList(new LambdaQueryWrapper<JkRetailOrderAttributionAdjustment>()
                .eq(JkRetailOrderAttributionAdjustment::getAttributionId, attribution.getId())));
    }

    private List<JkAuditLog> auditLogs(String businessType, Long businessId) {
        return auditLogDao.selectList(new LambdaQueryWrapper<JkAuditLog>()
                .eq(JkAuditLog::getBusinessType, businessType)
                .eq(JkAuditLog::getBusinessId, businessId)
                .eq(JkAuditLog::getIsDeleted, false)
                .orderByDesc(JkAuditLog::getId));
    }

    private Map<String, Object> empty(String type, Long id) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("businessType", type);
        result.put("businessId", id);
        result.put("primaryBusiness", null);
        result.put("attribution", null);
        result.put("relationSnapshot", null);
        result.put("regionResolutionSnapshot", null);
        result.put("performance", new ArrayList<Object>());
        result.put("commission", new ArrayList<Object>());
        result.put("refund", new ArrayList<Object>());
        result.put("reverse", new ArrayList<Object>());
        result.put("adjustments", new ArrayList<Object>());
        result.put("auditLogs", new ArrayList<Object>());
        result.put("readOnly", true);
        return result;
    }

    private JkRetailOrderAttribution requireAttribution(Long id) {
        JkRetailOrderAttribution row = attributionDao.selectById(id);
        if (row == null || Boolean.TRUE.equals(row.getIsDeleted())) throw new IllegalArgumentException("零售订单归属不存在");
        return row;
    }

    private JkCommissionRecord requireCommission(Long id) {
        JkCommissionRecord row = commissionRecordDao.selectById(id);
        if (row == null || Boolean.TRUE.equals(row.getIsDeleted())) throw new IllegalArgumentException("佣金记录不存在");
        return row;
    }

    private JkPerformanceRecord requirePerformance(Long id) {
        JkPerformanceRecord row = performanceRecordDao.selectById(id);
        if (row == null || Boolean.TRUE.equals(row.getIsDeleted())) throw new IllegalArgumentException("业绩记录不存在");
        return row;
    }
}
