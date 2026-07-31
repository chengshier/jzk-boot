package com.zbkj.service.service.impl.jiuzhoukang.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttributionAdjustment;
import com.zbkj.common.model.jiuzhoukang.JkRetailRefundAdjustment;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionAdjustmentDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailRefundAdjustmentDao;
import com.zbkj.service.service.jiuzhoukang.context.JkBusinessContextOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 统一业务抽屉只读上下文。 */
@Service
public class JkBusinessContextOverviewServiceImpl implements JkBusinessContextOverviewService {
    @Autowired private JkRetailOrderAttributionDao attributionDao;
    @Autowired private JkRetailOrderAttributionAdjustmentDao attributionAdjustmentDao;
    @Autowired private JkRetailRefundAdjustmentDao refundAdjustmentDao;
    @Autowired private JkCommissionRecordDao commissionRecordDao;
    @Autowired private JkPerformanceRecordDao performanceRecordDao;

    @Override
    public Map<String, Object> overview(String businessType, Long businessId) {
        if (businessId == null) throw new IllegalArgumentException("业务ID不能为空");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("businessType", businessType);
        result.put("businessId", businessId);
        if ("RETAIL_ATTRIBUTION".equals(businessType)) {
            fillAttribution(result, attributionDao.selectById(businessId));
        } else if ("COMMISSION_RECORD".equals(businessType)) {
            fillCommission(result, commissionRecordDao.selectById(businessId));
        } else if ("PERFORMANCE_RECORD".equals(businessType)) {
            fillPerformance(result, performanceRecordDao.selectById(businessId));
        } else {
            throw new IllegalArgumentException("暂不支持该业务上下文");
        }
        return result;
    }

    private void fillAttribution(Map<String, Object> result, JkRetailOrderAttribution row) {
        if (row == null || Boolean.TRUE.equals(row.getIsDeleted())) throw new IllegalArgumentException("零售归属不存在");
        result.put("primaryBusiness", row);
        result.put("attribution", row);
        result.put("title", "零售订单归属");
        result.put("businessNo", row.getOrderNo());
        result.put("statusText", row.getAttributionStatus());
        fillAttributionRelations(result, row);
    }

    private void fillAttributionRelations(Map<String, Object> result, JkRetailOrderAttribution row) {
        result.put("relationSnapshot", row.getRelationSnapshotJson());
        result.put("amountSnapshot", row.getAmountSnapshotJson());
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
        if (record == null || Boolean.TRUE.equals(record.getIsDeleted())) throw new IllegalArgumentException("佣金记录不存在");
        result.put("primaryBusiness", record);
        result.put("commission", java.util.Collections.singletonList(record));
        result.put("title", "佣金记录");
        result.put("businessNo", record.getCommissionNo());
        result.put("statusText", record.getStatus());
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
        if (record == null || Boolean.TRUE.equals(record.getIsDeleted())) throw new IllegalArgumentException("业绩记录不存在");
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
}
