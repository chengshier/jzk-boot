package com.zbkj.service.service.impl.jiuzhoukang.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class JkPerformanceServiceImpl implements JkPerformanceService {
    @Autowired private JkPerformanceRecordDao recordDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPerformanceRecord record(JkPerformanceRecord draft) {
        if (draft == null || draft.getOwnerUserId() == null || draft.getActionKey() == null) {
            throw new IllegalArgumentException("业绩记录参数不完整");
        }
        JkPerformanceRecord old = recordDao.selectOne(new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getActionKey, draft.getActionKey()).last("limit 1"));
        if (old != null) return old;
        Date now = new Date();
        draft.setPerformanceNo(draft.getPerformanceNo() == null ? "PF" + IdWorker.getIdStr() : draft.getPerformanceNo())
                .setQuantity(draft.getQuantity() == null ? 0 : draft.getQuantity())
                .setBaseAmount(money(draft.getBaseAmount()))
                .setPerformanceAmount(money(draft.getPerformanceAmount()))
                .setReversedAmount(BigDecimal.ZERO)
                .setStatus(draft.getStatus() == null ? "VALID" : draft.getStatus())
                .setOccurredAt(draft.getOccurredAt() == null ? now : draft.getOccurredAt())
                .setRequestNo(draft.getRequestNo() == null ? draft.getActionKey() : draft.getRequestNo())
                .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        try {
            recordDao.insert(draft);
        } catch (DuplicateKeyException ignored) {
            return recordDao.selectOne(new LambdaQueryWrapper<JkPerformanceRecord>()
                    .eq(JkPerformanceRecord::getActionKey, draft.getActionKey()).last("limit 1"));
        }
        return draft;
    }

    @Override
    public BigDecimal summary(Long ownerUserId, String performanceType) {
        LambdaQueryWrapper<JkPerformanceRecord> query = new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getOwnerUserId, ownerUserId)
                .eq(JkPerformanceRecord::getIsDeleted, false)
                .ne(JkPerformanceRecord::getStatus, "VOID")
                // 冲正明细是审计记录；原记录上的 reversedAmount 已反映冲正结果，汇总时不能再次扣减负数冲正行。
                .notLike(JkPerformanceRecord::getPerformanceType, "_REVERSE");
        if (performanceType != null && !performanceType.trim().isEmpty()) query.eq(JkPerformanceRecord::getPerformanceType, performanceType);
        BigDecimal result = BigDecimal.ZERO;
        for (JkPerformanceRecord row : recordDao.selectList(query)) {
            // 防御性保护：即使未来查询条件被调整，也不能把负数冲正审计行再次计入净业绩。
            if (isReverseAudit(row)) continue;
            result = result.add(money(row.getPerformanceAmount()).subtract(money(row.getReversedAmount())));
        }
        return result.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public PageInfo<JkPerformanceRecord> list(Long ownerUserId, String performanceType, String sourceType, String status, PageParamRequest pageParam) {
        Page<JkPerformanceRecord> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkPerformanceRecord> query = new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getIsDeleted, false).orderByDesc(JkPerformanceRecord::getOccurredAt).orderByDesc(JkPerformanceRecord::getId);
        if (ownerUserId != null) query.eq(JkPerformanceRecord::getOwnerUserId, ownerUserId);
        if (notBlank(performanceType)) query.eq(JkPerformanceRecord::getPerformanceType, performanceType);
        if (notBlank(sourceType)) query.eq(JkPerformanceRecord::getSourceType, sourceType);
        if (notBlank(status)) query.eq(JkPerformanceRecord::getStatus, status);
        return CommonPage.copyPageInfo(page, recordDao.selectList(query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverse(String sourceType, Long sourceId, Long sourceItemId, BigDecimal amount, String requestNo, String reason) {
        List<JkPerformanceRecord> rows = recordDao.selectList(sourceQuery(sourceType, sourceId, sourceItemId));
        BigDecimal totalRemaining = totalRemaining(rows);
        BigDecimal target = amount == null ? totalRemaining : amount.min(totalRemaining).max(BigDecimal.ZERO);
        reverseRows(sourceType, sourceId, rows, totalRemaining, target, requestNo, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reverseByRatio(String sourceType, Long sourceId, Long sourceItemId, BigDecimal ratio, String requestNo, String reason) {
        BigDecimal safeRatio = ratio == null ? BigDecimal.ONE : ratio.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        if (safeRatio.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<JkPerformanceRecord> rows = recordDao.selectList(sourceQuery(sourceType, sourceId, sourceItemId));
        BigDecimal totalRemaining = totalRemaining(rows);
        if (totalRemaining.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal target = totalRemaining.multiply(safeRatio).setScale(2, RoundingMode.HALF_UP).min(totalRemaining);
        reverseRows(sourceType, sourceId, rows, totalRemaining, target, requestNo, reason);
        return target;
    }

    private void reverseRows(String sourceType, Long sourceId, List<JkPerformanceRecord> rows,
                             BigDecimal totalRemaining, BigDecimal target, String requestNo, String reason) {
        if (target.signum() <= 0 || totalRemaining.signum() <= 0) return;
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < rows.size(); index++) {
            JkPerformanceRecord row = rows.get(index);
            BigDecimal rowRemaining = remaining(row);
            if (rowRemaining.signum() <= 0) continue;
            BigDecimal part = index == rows.size() - 1 ? target.subtract(allocated)
                    : target.multiply(rowRemaining).divide(totalRemaining, 2, RoundingMode.HALF_UP);
            part = part.min(rowRemaining).max(BigDecimal.ZERO);
            if (part.signum() <= 0) continue;
            int updated = recordDao.update(null, new UpdateWrapper<JkPerformanceRecord>()
                    .eq("id", row.getId()).eq("reversed_amount", money(row.getReversedAmount()))
                    .set("reversed_amount", money(row.getReversedAmount()).add(part))
                    .set("status", part.compareTo(rowRemaining) >= 0 ? "REVERSED" : "PARTIALLY_REVERSED")
                    .set("update_time", new Date()));
            if (updated != 1) throw new IllegalStateException("业绩冲正并发冲突，请重试");
            record(new JkPerformanceRecord().setSourceType(sourceType).setSourceId(sourceId).setSourceNo(row.getSourceNo())
                    .setSourceItemId(row.getSourceItemId()).setPerformanceType(row.getPerformanceType() + "_REVERSE")
                    .setOwnerUserId(row.getOwnerUserId()).setOwnerRoleCode(row.getOwnerRoleCode()).setSourceUserId(row.getSourceUserId())
                    .setDirectParentUserId(row.getDirectParentUserId()).setCountyAgentUserId(row.getCountyAgentUserId())
                    .setRegionCode(row.getRegionCode()).setProductId(row.getProductId()).setSkuId(row.getSkuId()).setQuantity(0)
                    .setBaseAmount(part.negate()).setPerformanceAmount(part.negate()).setRequestNo(requestNo)
                    .setRelationSnapshotJson(row.getRelationSnapshotJson()).setSourceSnapshotJson("{\"reason\":\"" + escape(reason) + "\"}")
                    .setActionKey("PERF_REVERSE:" + requestNo + ":" + row.getId()));
            allocated = allocated.add(part);
            if (allocated.compareTo(target) >= 0) break;
        }
    }

    private LambdaQueryWrapper<JkPerformanceRecord> sourceQuery(String sourceType, Long sourceId, Long sourceItemId) {
        LambdaQueryWrapper<JkPerformanceRecord> query = new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getSourceType, sourceType).eq(JkPerformanceRecord::getSourceId, sourceId)
                .eq(JkPerformanceRecord::getIsDeleted, false)
                .notLike(JkPerformanceRecord::getPerformanceType, "_REVERSE");
        if (sourceItemId != null) query.eq(JkPerformanceRecord::getSourceItemId, sourceItemId);
        return query;
    }

    private BigDecimal totalRemaining(List<JkPerformanceRecord> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (JkPerformanceRecord row : rows) total = total.add(remaining(row));
        return total;
    }

    private boolean isReverseAudit(JkPerformanceRecord row) {
        return row != null && row.getPerformanceType() != null && row.getPerformanceType().endsWith("_REVERSE");
    }
    private BigDecimal remaining(JkPerformanceRecord row) { return money(row.getPerformanceAmount()).subtract(money(row.getReversedAmount())).max(BigDecimal.ZERO); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
