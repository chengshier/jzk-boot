package com.zbkj.service.service.impl.jiuzhoukang.performance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.JkOperationProfitRecordDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/** OFFLINE_REALIZED 经营收益账本，不改变佣金账户和资金账户。 */
@Service
public class JkOperationProfitLedgerService {
    @Autowired private JkOperationProfitRecordDao dao;

    @Transactional(rollbackFor = Exception.class)
    public JkOperationProfitRecord record(JkOperationProfitRecord value) {
        if (value == null || value.getUserId() == null || value.getSourceType() == null
                || value.getSourceId() == null || value.getRequestNo() == null) {
            throw new IllegalArgumentException("经营收益记录参数不完整");
        }
        JkOperationProfitRecord old = dao.selectOne(new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getRequestNo, value.getRequestNo()).last("limit 1"));
        if (old != null) return old;
        Date now = new Date();
        BigDecimal revenue = money(value.getRevenueAmount());
        BigDecimal cost = money(value.getCostAmount());
        BigDecimal profit = value.getProfitAmount() == null ? revenue.subtract(cost) : value.getProfitAmount();
        value.setProfitNo(value.getProfitNo() == null ? "OP" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr() : value.getProfitNo())
                .setIncomeNature(value.getIncomeNature() == null ? "OFFLINE_REALIZED" : value.getIncomeNature())
                .setQuantity(value.getQuantity() == null ? 0 : value.getQuantity())
                .setRevenueAmount(revenue.setScale(2, RoundingMode.HALF_UP))
                .setCostAmount(cost.setScale(2, RoundingMode.HALF_UP))
                .setProfitAmount(profit.setScale(2, RoundingMode.HALF_UP))
                .setReversedAmount(money(value.getReversedAmount()))
                .setStatus(value.getStatus() == null ? "CONFIRMED" : value.getStatus())
                .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        try {
            dao.insert(value);
        } catch (DuplicateKeyException ignored) {
            return dao.selectOne(new LambdaQueryWrapper<JkOperationProfitRecord>()
                    .eq(JkOperationProfitRecord::getRequestNo, value.getRequestNo()).last("limit 1"));
        }
        return value;
    }

    public PageInfo<JkOperationProfitRecord> list(Long userId, String sourceType, String status, PageParamRequest pageParam) {
        Page<JkOperationProfitRecord> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkOperationProfitRecord> query = new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getIsDeleted, false)
                .orderByDesc(JkOperationProfitRecord::getCreateTime).orderByDesc(JkOperationProfitRecord::getId);
        if (userId != null) query.eq(JkOperationProfitRecord::getUserId, userId);
        if (notBlank(sourceType)) query.eq(JkOperationProfitRecord::getSourceType, sourceType);
        if (notBlank(status)) query.eq(JkOperationProfitRecord::getStatus, status);
        List<JkOperationProfitRecord> rows = dao.selectList(query);
        return CommonPage.copyPageInfo(page, rows);
    }

    public BigDecimal confirmedProfit(Long userId) {
        BigDecimal total = BigDecimal.ZERO;
        for (JkOperationProfitRecord row : dao.selectList(new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getUserId, userId)
                .eq(JkOperationProfitRecord::getIncomeNature, "OFFLINE_REALIZED")
                .eq(JkOperationProfitRecord::getIsDeleted, false))) {
            total = total.add(money(row.getProfitAmount()).subtract(money(row.getReversedAmount())));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reverseBySource(String sourceType, Long sourceId, BigDecimal ratio, String reason) {
        BigDecimal safeRatio = ratio == null ? BigDecimal.ONE : ratio.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        BigDecimal total = BigDecimal.ZERO;
        List<JkOperationProfitRecord> rows = dao.selectList(new LambdaQueryWrapper<JkOperationProfitRecord>()
                .eq(JkOperationProfitRecord::getSourceType, sourceType)
                .eq(JkOperationProfitRecord::getSourceId, sourceId)
                .eq(JkOperationProfitRecord::getIsDeleted, false));
        for (JkOperationProfitRecord row : rows) {
            BigDecimal remaining = money(row.getProfitAmount()).subtract(money(row.getReversedAmount())).max(BigDecimal.ZERO);
            BigDecimal amount = remaining.multiply(safeRatio).setScale(2, RoundingMode.HALF_UP).min(remaining);
            if (amount.signum() <= 0) continue;
            BigDecimal next = money(row.getReversedAmount()).add(amount);
            row.setReversedAmount(next)
                    .setStatus(next.compareTo(money(row.getProfitAmount())) >= 0 ? "REVERSED" : "PARTIALLY_REVERSED")
                    .setRelationSnapshotJson("{\"reverseReason\":\"" + escape(reason) + "\"}")
                    .setUpdateTime(new Date());
            dao.updateById(row);
            total = total.add(amount);
        }
        return total;
    }

    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
}
