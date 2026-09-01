package com.zbkj.service.service.impl.jiuzhoukang.profit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.JkOperationProfitRecordDao;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class JkOperationProfitServiceImpl implements JkOperationProfitService {
    @Autowired private JkOperationProfitRecordDao recordDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkOperationProfitRecord record(JkOperationProfitRecord draft) {
        if (draft == null || draft.getUserId() == null || draft.getActionKey() == null) throw new IllegalArgumentException("经营收益参数不完整");
        JkOperationProfitRecord old = recordDao.selectOne(new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getActionKey, draft.getActionKey()).last("limit 1"));
        if (old != null) return old;
        Date now = new Date();
        draft.setProfitNo(draft.getProfitNo() == null ? "OP" + IdWorker.getIdStr() : draft.getProfitNo())
                .setIncomeNature("OFFLINE_REALIZED")
                .setQuantity(draft.getQuantity() == null ? 0 : draft.getQuantity())
                .setRevenueAmount(money(draft.getRevenueAmount())).setCostAmount(money(draft.getCostAmount()))
                .setProfitAmount(money(draft.getProfitAmount())).setReversedAmount(BigDecimal.ZERO)
                .setStatus(draft.getStatus() == null ? "CONFIRMED" : draft.getStatus())
                .setRequestNo(draft.getRequestNo() == null ? draft.getActionKey() : draft.getRequestNo())
                .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        try { recordDao.insert(draft); }
        catch (DuplicateKeyException ignored) {
            return recordDao.selectOne(new LambdaQueryWrapper<JkOperationProfitRecord>()
                    .eq(JkOperationProfitRecord::getActionKey, draft.getActionKey()).last("limit 1"));
        }
        return draft;
    }

    @Override
    public BigDecimal summary(Long userId) {
        BigDecimal result = BigDecimal.ZERO;
        for (JkOperationProfitRecord row : recordDao.selectList(new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getUserId, userId).eq(JkOperationProfitRecord::getIsDeleted, false)
                .ne(JkOperationProfitRecord::getStatus, "VOID")
                // REVERSAL 是负数审计明细；原记录的 reversedAmount 已反映冲正结果，汇总时不能再次扣减。
                .ne(JkOperationProfitRecord::getStatus, "REVERSAL"))) {
            // 防御性保护：即使查询条件未来被调整，也不能把负数冲正审计行重复计入净收益。
            if (row != null && "REVERSAL".equals(row.getStatus())) continue;
            result = result.add(money(row.getProfitAmount()).subtract(money(row.getReversedAmount())));
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public PageInfo<JkOperationProfitRecord> list(Long userId, String sourceType, String status, PageParamRequest pageParam) {
        Page<JkOperationProfitRecord> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkOperationProfitRecord> query = new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getIsDeleted, false).orderByDesc(JkOperationProfitRecord::getId);
        if (userId != null) query.eq(JkOperationProfitRecord::getUserId, userId);
        if (notBlank(sourceType)) query.eq(JkOperationProfitRecord::getSourceType, sourceType);
        if (notBlank(status)) query.eq(JkOperationProfitRecord::getStatus, status);
        return CommonPage.copyPageInfo(page, recordDao.selectList(query));
    }

    @Override
    public JkOperationProfitRecord detail(Long viewerUserId, Long id, boolean admin) {
        JkOperationProfitRecord row = recordDao.selectById(id);
        if (row == null || Boolean.TRUE.equals(row.getIsDeleted())) throw new IllegalArgumentException("经营收益记录不存在");
        if (!admin && !viewerUserId.equals(row.getUserId())) throw new IllegalArgumentException("无权查看该经营收益记录");
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverse(String sourceType, Long sourceId, Long sourceItemId, BigDecimal amount, String requestNo, String reason) {
        List<JkOperationProfitRecord> rows = recordDao.selectList(sourceQuery(sourceType, sourceId, sourceItemId));
        BigDecimal totalRemaining = totalRemaining(rows);
        BigDecimal target = amount == null ? totalRemaining : amount.min(totalRemaining).max(BigDecimal.ZERO);
        reverseRows(sourceType, sourceId, rows, totalRemaining, target, requestNo, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reverseByRatio(String sourceType, Long sourceId, Long sourceItemId, BigDecimal ratio, String requestNo, String reason) {
        BigDecimal safeRatio = ratio == null ? BigDecimal.ONE : ratio.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        if (safeRatio.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<JkOperationProfitRecord> rows = recordDao.selectList(sourceQuery(sourceType, sourceId, sourceItemId));
        BigDecimal totalRemaining = totalRemaining(rows);
        if (totalRemaining.signum() <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        // 比例始终以原始毛利为基准，避免连续部分退回时基于剩余毛利再次乘比例而少冲。
        BigDecimal target = totalOriginal(rows).multiply(safeRatio).setScale(2, RoundingMode.HALF_UP).min(totalRemaining);
        reverseRows(sourceType, sourceId, rows, totalRemaining, target, requestNo, reason);
        return target;
    }

    private void reverseRows(String sourceType, Long sourceId, List<JkOperationProfitRecord> rows,
                             BigDecimal totalRemaining, BigDecimal target, String requestNo, String reason) {
        if (target.signum() <= 0 || totalRemaining.signum() <= 0) return;
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < rows.size(); index++) {
            JkOperationProfitRecord row = rows.get(index);
            BigDecimal remain = remaining(row);
            if (remain.signum() <= 0) continue;
            BigDecimal part = index == rows.size() - 1 ? target.subtract(allocated)
                    : target.multiply(remain).divide(totalRemaining, 2, RoundingMode.HALF_UP);
            part = part.min(remain).max(BigDecimal.ZERO);
            if (part.signum() <= 0) continue;
            int updated = recordDao.update(null, new UpdateWrapper<JkOperationProfitRecord>()
                    .eq("id", row.getId()).eq("reversed_amount", money(row.getReversedAmount()))
                    .set("reversed_amount", money(row.getReversedAmount()).add(part))
                    .set("status", part.compareTo(remain) >= 0 ? "REVERSED" : "PARTIALLY_REVERSED")
                    .set("update_time", new Date()));
            if (updated != 1) throw new IllegalStateException("经营收益冲正并发冲突，请重试");
            record(new JkOperationProfitRecord().setUserId(row.getUserId()).setRoleCode(row.getRoleCode())
                    .setSourceType(sourceType + "_RETURN").setSourceId(sourceId).setSourceNo(row.getSourceNo())
                    .setSourceItemId(row.getSourceItemId()).setProductId(row.getProductId()).setSkuId(row.getSkuId())
                    .setQuantity(0).setRevenueAmount(part.negate()).setCostAmount(BigDecimal.ZERO).setProfitAmount(part.negate())
                    .setStatus("REVERSAL").setCostSnapshotJson(row.getCostSnapshotJson())
                    .setRelationSnapshotJson("{\"reason\":\"" + escape(reason) + "\"}")
                    .setRequestNo(requestNo).setActionKey("PROFIT_REVERSE:" + requestNo + ":" + row.getId()));
            allocated = allocated.add(part);
            if (allocated.compareTo(target) >= 0) break;
        }
    }

    private LambdaQueryWrapper<JkOperationProfitRecord> sourceQuery(String sourceType, Long sourceId, Long sourceItemId) {
        LambdaQueryWrapper<JkOperationProfitRecord> query = new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getSourceType, sourceType).eq(JkOperationProfitRecord::getSourceId, sourceId)
                .eq(JkOperationProfitRecord::getIsDeleted, false)
                .ne(JkOperationProfitRecord::getStatus, "VOID")
                .ne(JkOperationProfitRecord::getStatus, "REVERSAL");
        if (sourceItemId != null) query.eq(JkOperationProfitRecord::getSourceItemId, sourceItemId);
        return query;
    }

    private BigDecimal totalRemaining(List<JkOperationProfitRecord> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (JkOperationProfitRecord row : rows) total = total.add(remaining(row));
        return total;
    }

    private BigDecimal totalOriginal(List<JkOperationProfitRecord> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (JkOperationProfitRecord row : rows) total = total.add(money(row.getProfitAmount()).max(BigDecimal.ZERO));
        return total;
    }

    private BigDecimal remaining(JkOperationProfitRecord row) { return money(row.getProfitAmount()).subtract(money(row.getReversedAmount())).max(BigDecimal.ZERO); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
