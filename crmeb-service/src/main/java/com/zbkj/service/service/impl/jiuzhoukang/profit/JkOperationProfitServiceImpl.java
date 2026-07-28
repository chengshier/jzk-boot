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
                .ne(JkOperationProfitRecord::getStatus, "VOID"))) {
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
        LambdaQueryWrapper<JkOperationProfitRecord> query = new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getSourceType, sourceType).eq(JkOperationProfitRecord::getSourceId, sourceId)
                .eq(JkOperationProfitRecord::getIsDeleted, false).ne(JkOperationProfitRecord::getStatus, "VOID");
        if (sourceItemId != null) query.eq(JkOperationProfitRecord::getSourceItemId, sourceItemId);
        List<JkOperationProfitRecord> rows = recordDao.selectList(query);
        BigDecimal total = BigDecimal.ZERO;
        for (JkOperationProfitRecord row : rows) total = total.add(remaining(row));
        BigDecimal target = amount == null ? total : amount.min(total).max(BigDecimal.ZERO);
        if (target.signum() <= 0 || total.signum() <= 0) return;
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < rows.size(); index++) {
            JkOperationProfitRecord row = rows.get(index);
            BigDecimal remain = remaining(row);
            if (remain.signum() <= 0) continue;
            BigDecimal part = index == rows.size() - 1 ? target.subtract(allocated)
                    : target.multiply(remain).divide(total, 2, RoundingMode.HALF_UP);
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

    private BigDecimal remaining(JkOperationProfitRecord row) { return money(row.getProfitAmount()).subtract(money(row.getReversedAmount())).max(BigDecimal.ZERO); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
