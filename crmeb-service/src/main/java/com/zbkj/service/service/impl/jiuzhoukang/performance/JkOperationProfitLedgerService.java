package com.zbkj.service.service.impl.jiuzhoukang.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.JkOperationProfitRecordDao;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 经营收益账本兼容适配层。
 *
 * <p>历史调用方仍依赖本类的 requestNo / reverseBySource 接口，
 * 实际记账统一委托 {@link JkOperationProfitService}。
 * 这样既保留旧调用契约，也避免继续维护第二套经营收益写账规则。</p>
 */
@Service
@Deprecated
public class JkOperationProfitLedgerService {
    @Autowired private JkOperationProfitService profitService;
    @Autowired private JkOperationProfitRecordDao recordDao;

    /**
     * 兼容旧调用方以 requestNo 作为幂等键的约定。
     * 正式 Service 在成本或利润事实不完整时不会把 revenue 自动当成 profit。
     */
    public JkOperationProfitRecord record(JkOperationProfitRecord value) {
        if (value == null || value.getUserId() == null || value.getSourceType() == null
                || value.getSourceId() == null || value.getRequestNo() == null) {
            throw new IllegalArgumentException("经营收益记录参数不完整");
        }
        if (value.getActionKey() == null || value.getActionKey().trim().isEmpty()) {
            value.setActionKey(value.getRequestNo());
        }
        return profitService.record(value);
    }

    public PageInfo<JkOperationProfitRecord> list(Long userId, String sourceType, String status, PageParamRequest pageParam) {
        return profitService.list(userId, sourceType, status, pageParam);
    }

    public BigDecimal confirmedProfit(Long userId) {
        return profitService.summary(userId);
    }

    /**
     * 兼容旧的按来源比例冲正接口。
     * 比例只用于换算实际冲正金额，写账、并发控制和负数审计明细统一交给正式 Service。
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reverseBySource(String sourceType, Long sourceId, BigDecimal ratio, String reason) {
        BigDecimal safeRatio = ratio == null ? BigDecimal.ONE : ratio.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        if (safeRatio.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<JkOperationProfitRecord> rows = recordDao.selectList(new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getSourceType, sourceType)
                .eq(JkOperationProfitRecord::getSourceId, sourceId)
                .eq(JkOperationProfitRecord::getIsDeleted, false)
                .ne(JkOperationProfitRecord::getStatus, "VOID")
                .ne(JkOperationProfitRecord::getStatus, "REVERSAL"));
        BigDecimal totalRemaining = BigDecimal.ZERO;
        for (JkOperationProfitRecord row : rows) {
            totalRemaining = totalRemaining.add(remaining(row));
        }
        BigDecimal target = totalRemaining.multiply(safeRatio).setScale(2, RoundingMode.HALF_UP).min(totalRemaining);
        if (target.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 旧接口没有业务 requestNo；关键退款/退货调用方会继续迁移到正式 reverse 接口并传业务幂等键。
        String requestNo = "PROFIT_REVERSE_LEGACY:" + sourceType + ":" + sourceId + ":" + IdWorker.getIdStr();
        profitService.reverse(sourceType, sourceId, null, target, requestNo, reason);
        return target;
    }

    private BigDecimal remaining(JkOperationProfitRecord row) {
        return money(row.getProfitAmount()).subtract(money(row.getReversedAmount())).max(BigDecimal.ZERO);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
