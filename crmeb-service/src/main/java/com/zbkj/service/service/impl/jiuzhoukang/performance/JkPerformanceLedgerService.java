package com.zbkj.service.service.impl.jiuzhoukang.performance;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 业绩账本兼容适配层。
 *
 * <p>V3.1 历史调用方仍依赖该类的 requestNo / reverseBySource 接口，
 * 实际记账统一委托 {@link JkPerformanceService}，避免两套 Service 分别写同一张业绩账本。
 * 待所有调用方迁移到正式接口后删除本兼容层。</p>
 */
@Service
@Deprecated
public class JkPerformanceLedgerService {
    @Autowired private JkPerformanceService performanceService;

    /**
     * 兼容旧调用方以 requestNo 作为幂等键的约定。
     */
    public JkPerformanceRecord record(JkPerformanceRecord value) {
        if (value == null || value.getOwnerUserId() == null || value.getSourceType() == null
                || value.getSourceId() == null || value.getPerformanceType() == null || value.getRequestNo() == null) {
            throw new IllegalArgumentException("业绩记录参数不完整");
        }
        if (value.getActionKey() == null || value.getActionKey().trim().isEmpty()) {
            value.setActionKey(value.getRequestNo());
        }
        return performanceService.record(value);
    }

    public PageInfo<JkPerformanceRecord> list(Long ownerUserId, String performanceType, String sourceType,
                                               String status, PageParamRequest pageParam) {
        return performanceService.list(ownerUserId, performanceType, sourceType, status, pageParam);
    }

    public BigDecimal validAmount(Long ownerUserId) {
        return performanceService.summary(ownerUserId, null);
    }

    /**
     * 兼容旧的按来源比例冲正接口，比例语义已收口到正式 Service。
     * 旧接口缺少业务 requestNo，因此仅作为迁移期兼容入口；关键退款/退货链路应直接调用正式接口并传确定性幂等键。
     */
    public BigDecimal reverseBySource(String sourceType, Long sourceId, BigDecimal ratio, String reason) {
        BigDecimal safeRatio = ratio == null ? BigDecimal.ONE : ratio.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        if (safeRatio.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String requestNo = "PERF_REVERSE_LEGACY:" + sourceType + ":" + sourceId + ":" + IdWorker.getIdStr();
        return performanceService.reverseByRatio(sourceType, sourceId, null, safeRatio, requestNo, reason);
    }
}
