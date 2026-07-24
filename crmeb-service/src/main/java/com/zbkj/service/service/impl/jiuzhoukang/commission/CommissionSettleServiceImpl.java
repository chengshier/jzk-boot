package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionSettleTask;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionSettleTaskDao;
import com.zbkj.service.service.jiuzhoukang.commission.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/**
 * 佣金结算服务。
 * <p>只允许结算冻结期已结束的具体佣金记录，并同时更新佣金账户、资金账户、结算任务和流水。
 * 禁止绕过记录直接按任意金额入账，失败任务可以幂等重试。</p>
 */
@Service
public class CommissionSettleServiceImpl implements CommissionSettleService {
    @Autowired private CommissionAccountService commissionAccountService;
    @Autowired private FundAccountService fundAccountService;
    @Autowired private JkCommissionSettleTaskDao taskDao;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private CommissionSettleTaskPersistenceService taskPersistenceService;

    @Override @Transactional
    public void settleToFundAccount(Long userId, String roleCode, BigDecimal amount, String taskNo, String requestNo, String key) {
        throw new UnsupportedOperationException("禁止按账户金额直接结算，请按已到冻结期的佣金记录执行 settleRecords");
    }


    @Override @Transactional
    public JkCommissionSettleTask settleRecords(List<Long> recordIds, Long operatorId, String requestNo, String remark) {
        return settleRecordsInternal(recordIds, operatorId, requestNo, remark, "MANUAL");
    }

    /**
     * 结算指定的到期记录。每条记录都会再次检查状态和 freezeEndTime，防止定时任务与人工操作并发重复入账。
     */
    @Override @Transactional
    public JkCommissionSettleTask settleDueRecords(List<Long> recordIds, Long operatorId, String requestNo, String remark) {
        return settleRecordsInternal(recordIds, operatorId, requestNo, remark, "AUTO");
    }

    private JkCommissionSettleTask settleRecordsInternal(List<Long> recordIds, Long operatorId, String requestNo, String remark, String settleType) {
        if (requestNo == null || requestNo.trim().isEmpty()) throw new IllegalArgumentException("requestNo不能为空");
        String key = settleType + "_RECORD_SETTLE:" + requestNo;
        JkCommissionSettleTask existing = taskDao.selectOne(new LambdaQueryWrapper<JkCommissionSettleTask>().eq(JkCommissionSettleTask::getIdempotencyKey, key));
        if (existing != null && "SUCCESS".equals(existing.getStatus())) return existing;
        if (existing != null && "RUNNING".equals(existing.getStatus())) throw new IllegalStateException("同一批佣金正在结算，请勿重复提交");
        if (recordIds == null || recordIds.isEmpty()) throw new IllegalArgumentException("至少选择一条待结算佣金记录");
        List<JkCommissionRecord> records = recordDao.selectBatchIds(recordIds);
        if (records.size() != new HashSet<Long>(recordIds).size()) throw new IllegalArgumentException("存在不存在的佣金记录");
        Long userId = null; String roleCode = null; List<BigDecimal> amounts = new ArrayList<>();
        Date now = new Date();
        for (JkCommissionRecord record : records) {
            if (Boolean.TRUE.equals(record.getIsDeleted()) || !"PENDING_SETTLE".equals(record.getStatus())) throw new IllegalArgumentException("仅允许结算待结算佣金记录");
            if (record.getFreezeEndTime() != null && record.getFreezeEndTime().after(now)) {
                throw new IllegalArgumentException("佣金尚未到冻结期结束时间，不能提前结算，recordId=" + record.getId());
            }
            if (userId == null) { userId = record.getReceiverUserId(); roleCode = record.getReceiverRoleCode(); }
            if (!userId.equals(record.getReceiverUserId()) || !roleCode.equals(record.getReceiverRoleCode())) throw new IllegalArgumentException("一次结算只能选择同一用户、同一身份的佣金记录");
            amounts.add(record.getCommissionAmount());
        }
        BigDecimal total = CommissionSettlementSupport.requireTotal(amounts);
        JkCommissionSettleTask task = existing != null
                ? taskPersistenceService.restartFailed(existing.getId(), records.size(), operatorId)
                : createTask(null, requestNo, key, records.size(), operatorId, settleType);
        if (task == null || !"RUNNING".equals(task.getStatus())) throw new IllegalStateException("结算任务状态异常，请重试");
        try {
            moveAccounts(userId, roleCode, total, requestNo, key);
            for (JkCommissionRecord record : records) {
                int updated = recordDao.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<JkCommissionRecord>()
                        .eq("id", record.getId()).eq("status", "PENDING_SETTLE").eq("is_deleted", false)
                        .set("settled_amount", record.getCommissionAmount())
                        .set("status", "SETTLED")
                        .set("update_time", now));
                if (updated != 1) throw new IllegalStateException("佣金记录状态已变化，请重试，recordId=" + record.getId());
            }
            task.setStatus("SUCCESS").setSuccessCount(records.size()).setFinishTime(now).setUpdateTime(now).setFailReason(remark);
            taskDao.updateById(task);
            return task;
        } catch (RuntimeException e) { fail(task, e); throw e; }
    }

    private void moveAccounts(Long userId, String roleCode, BigDecimal amount, String requestNo, String key) {
        // 佣金账户与资金账户分别保留同一笔负向待抵扣镜像；两边都按本次结算总额抵扣，
        // 资金账户只会把抵扣后的剩余部分计入 availableAmount，不会重复增加可提现余额。
        commissionAccountService.settle(userId, roleCode, amount, requestNo, "COMMISSION_SETTLE:" + key);
        fundAccountService.creditAvailable(userId, roleCode, amount, requestNo, "FUND_SETTLE:" + key);
    }
    private JkCommissionSettleTask createTask(String taskNo, String requestNo, String key, int totalCount, Long operatorId, String settleType) {
        Date now = new Date();
        JkCommissionSettleTask task = new JkCommissionSettleTask().setTaskNo(taskNo == null ? "ST" + id() : taskNo).setSettleType(settleType)
                .setStatus("RUNNING").setRequestNo(requestNo).setIdempotencyKey(key).setTotalCount(totalCount).setSuccessCount(0).setFailCount(0)
                .setOperatorId(operatorId).setStartTime(now).setCreateTime(now).setUpdateTime(now);
        return taskPersistenceService.create(task);
    }
    private void fail(JkCommissionSettleTask task, RuntimeException e) { taskPersistenceService.markFailed(task.getId(), e.getMessage()); }
    private String id() { return UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
}