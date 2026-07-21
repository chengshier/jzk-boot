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

@Service
public class CommissionSettleServiceImpl implements CommissionSettleService {
    @Autowired private CommissionAccountService commissionAccountService;
    @Autowired private FundAccountService fundAccountService;
    @Autowired private JkCommissionSettleTaskDao taskDao;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private CommissionSettleTaskPersistenceService taskPersistenceService;

    @Override @Transactional
    public void settleToFundAccount(Long userId, String roleCode, BigDecimal amount, String taskNo, String requestNo, String key) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("结算金额必须大于零");
        JkCommissionSettleTask old = taskDao.selectOne(new LambdaQueryWrapper<JkCommissionSettleTask>().eq(JkCommissionSettleTask::getIdempotencyKey, key));
        if (old != null) return;
        JkCommissionSettleTask task = createTask(taskNo, requestNo, key, 1, null);
        try {
            moveAccounts(userId, roleCode, amount, requestNo, key);
            task.setStatus("SUCCESS").setSuccessCount(1).setFinishTime(new Date()).setUpdateTime(new Date());
            taskDao.updateById(task);
        } catch (RuntimeException e) { fail(task, e); throw e; }
    }

    @Override @Transactional
    public JkCommissionSettleTask settleRecords(List<Long> recordIds, Long operatorId, String requestNo, String remark) {
        if (requestNo == null || requestNo.trim().isEmpty()) throw new IllegalArgumentException("requestNo不能为空");
        String key = "MANUAL_RECORD_SETTLE:" + requestNo;
        JkCommissionSettleTask existing = taskDao.selectOne(new LambdaQueryWrapper<JkCommissionSettleTask>().eq(JkCommissionSettleTask::getIdempotencyKey, key));
        if (existing != null) return existing;
        if (recordIds == null || recordIds.isEmpty()) throw new IllegalArgumentException("至少选择一条待结算佣金记录");
        List<JkCommissionRecord> records = recordDao.selectBatchIds(recordIds);
        if (records.size() != new HashSet<Long>(recordIds).size()) throw new IllegalArgumentException("存在不存在的佣金记录");
        Long userId = null; String roleCode = null; List<BigDecimal> amounts = new ArrayList<>();
        for (JkCommissionRecord record : records) {
            if (Boolean.TRUE.equals(record.getIsDeleted()) || !"PENDING_SETTLE".equals(record.getStatus())) throw new IllegalArgumentException("仅允许结算待结算佣金记录");
            if (userId == null) { userId = record.getReceiverUserId(); roleCode = record.getReceiverRoleCode(); }
            if (!userId.equals(record.getReceiverUserId()) || !roleCode.equals(record.getReceiverRoleCode())) throw new IllegalArgumentException("一次结算只能选择同一用户、同一身份的佣金记录");
            amounts.add(record.getCommissionAmount());
        }
        BigDecimal total = CommissionSettlementSupport.requireTotal(amounts);
        JkCommissionSettleTask task = createTask(null, requestNo, key, records.size(), operatorId);
        try {
            moveAccounts(userId, roleCode, total, requestNo, key);
            Date now = new Date();
            for (JkCommissionRecord record : records) {
                record.setSettledAmount(record.getCommissionAmount()).setStatus("SETTLED").setUpdateTime(now);
                recordDao.updateById(record);
            }
            task.setStatus("SUCCESS").setSuccessCount(records.size()).setFinishTime(now).setUpdateTime(now).setFailReason(remark);
            taskDao.updateById(task);
            return task;
        } catch (RuntimeException e) { fail(task, e); throw e; }
    }

    private void moveAccounts(Long userId, String roleCode, BigDecimal amount, String requestNo, String key) {
        commissionAccountService.settle(userId, roleCode, amount, requestNo, "COMMISSION_SETTLE:" + key);
        fundAccountService.creditAvailable(userId, roleCode, amount, requestNo, "FUND_SETTLE:" + key);
    }
    private JkCommissionSettleTask createTask(String taskNo, String requestNo, String key, int totalCount, Long operatorId) {
        Date now = new Date();
        JkCommissionSettleTask task = new JkCommissionSettleTask().setTaskNo(taskNo == null ? "ST" + id() : taskNo).setSettleType("MANUAL")
                .setStatus("RUNNING").setRequestNo(requestNo).setIdempotencyKey(key).setTotalCount(totalCount).setSuccessCount(0).setFailCount(0)
                .setOperatorId(operatorId).setStartTime(now).setCreateTime(now).setUpdateTime(now);
        return taskPersistenceService.create(task);
    }
    private void fail(JkCommissionSettleTask task, RuntimeException e) { taskPersistenceService.markFailed(task.getId(), e.getMessage()); }
    private String id() { return UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
}