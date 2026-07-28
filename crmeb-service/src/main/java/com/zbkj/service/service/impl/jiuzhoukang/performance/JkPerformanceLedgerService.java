package com.zbkj.service.service.impl.jiuzhoukang.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/** V3.1 独立业绩账本。业绩与佣金分离，规则关闭时仍可完整记录业务事实。 */
@Service
public class JkPerformanceLedgerService {
    @Autowired private JkPerformanceRecordDao dao;

    @Transactional(rollbackFor = Exception.class)
    public JkPerformanceRecord record(JkPerformanceRecord value) {
        if (value == null || value.getOwnerUserId() == null || value.getSourceType() == null
                || value.getSourceId() == null || value.getPerformanceType() == null || value.getRequestNo() == null) {
            throw new IllegalArgumentException("业绩记录参数不完整");
        }
        JkPerformanceRecord old = dao.selectOne(new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getRequestNo, value.getRequestNo()).last("limit 1"));
        if (old != null) return old;
        Date now = new Date();
        value.setPerformanceNo(value.getPerformanceNo() == null ? "PF" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr() : value.getPerformanceNo())
                .setQuantity(nvl(value.getQuantity()))
                .setBaseAmount(money(value.getBaseAmount()))
                .setPerformanceAmount(money(value.getPerformanceAmount()))
                .setReversedAmount(money(value.getReversedAmount()))
                .setStatus(value.getStatus() == null ? "VALID" : value.getStatus())
                .setOccurredAt(value.getOccurredAt() == null ? now : value.getOccurredAt())
                .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        try {
            dao.insert(value);
        } catch (DuplicateKeyException ignored) {
            return dao.selectOne(new LambdaQueryWrapper<JkPerformanceRecord>()
                    .eq(JkPerformanceRecord::getRequestNo, value.getRequestNo()).last("limit 1"));
        }
        return value;
    }

    public PageInfo<JkPerformanceRecord> list(Long ownerUserId, String performanceType, String sourceType,
                                               String status, PageParamRequest pageParam) {
        Page<JkPerformanceRecord> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkPerformanceRecord> query = new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getIsDeleted, false).orderByDesc(JkPerformanceRecord::getOccurredAt)
                .orderByDesc(JkPerformanceRecord::getId);
        if (ownerUserId != null) query.eq(JkPerformanceRecord::getOwnerUserId, ownerUserId);
        if (notBlank(performanceType)) query.eq(JkPerformanceRecord::getPerformanceType, performanceType);
        if (notBlank(sourceType)) query.eq(JkPerformanceRecord::getSourceType, sourceType);
        if (notBlank(status)) query.eq(JkPerformanceRecord::getStatus, status);
        List<JkPerformanceRecord> rows = dao.selectList(query);
        return CommonPage.copyPageInfo(page, rows);
    }

    public BigDecimal validAmount(Long ownerUserId) {
        BigDecimal total = BigDecimal.ZERO;
        for (JkPerformanceRecord row : dao.selectList(new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getOwnerUserId, ownerUserId)
                .eq(JkPerformanceRecord::getStatus, "VALID")
                .eq(JkPerformanceRecord::getIsDeleted, false))) {
            total = total.add(money(row.getPerformanceAmount()).subtract(money(row.getReversedAmount())));
        }
        return total.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reverseBySource(String sourceType, Long sourceId, BigDecimal ratio, String reason) {
        BigDecimal safeRatio = ratio == null ? BigDecimal.ONE : ratio.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        BigDecimal reversed = BigDecimal.ZERO;
        List<JkPerformanceRecord> rows = dao.selectList(new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getSourceType, sourceType)
                .eq(JkPerformanceRecord::getSourceId, sourceId)
                .eq(JkPerformanceRecord::getIsDeleted, false));
        for (JkPerformanceRecord row : rows) {
            BigDecimal remaining = money(row.getPerformanceAmount()).subtract(money(row.getReversedAmount())).max(BigDecimal.ZERO);
            BigDecimal amount = remaining.multiply(safeRatio).setScale(2, RoundingMode.HALF_UP).min(remaining);
            if (amount.signum() <= 0) continue;
            row.setReversedAmount(money(row.getReversedAmount()).add(amount))
                    .setStatus(money(row.getReversedAmount()).add(amount).compareTo(money(row.getPerformanceAmount())) >= 0 ? "REVERSED" : "PARTIALLY_REVERSED")
                    .setSourceSnapshotJson(appendReason(row.getSourceSnapshotJson(), reason)).setUpdateTime(new Date());
            dao.updateById(row);
            reversed = reversed.add(amount);
        }
        return reversed;
    }

    private String appendReason(String json, String reason) {
        String base = json == null ? "{}" : json;
        return "{\"original\":" + quote(base) + ",\"reverseReason\":" + quote(reason) + "}";
    }
    private String quote(String value) { return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
}
