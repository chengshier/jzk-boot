package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkCommissionSettleTask;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionSettleTaskDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

/** 结算任务审计持久化：独立事务保证业务回滚后仍保留失败任务。 */
@Service
public class CommissionSettleTaskPersistenceService {
    @Autowired private JkCommissionSettleTaskDao taskDao;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JkCommissionSettleTask create(JkCommissionSettleTask task) { taskDao.insert(task); return task; }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JkCommissionSettleTask restartFailed(Long taskId, int totalCount, Long operatorId) {
        JkCommissionSettleTask task = taskDao.selectById(taskId);
        if (task == null) return null;
        if (!"FAILED".equals(task.getStatus())) return task;
        Date now = new Date();
        task.setStatus("RUNNING").setTotalCount(totalCount).setSuccessCount(0).setFailCount(0)
                .setFailReason(null).setOperatorId(operatorId).setStartTime(now).setFinishTime(null).setUpdateTime(now);
        taskDao.updateById(task);
        return task;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long taskId, String reason) { JkCommissionSettleTask task = taskDao.selectById(taskId); if (task == null) return; task.setStatus("FAILED").setFailCount(task.getTotalCount()).setFailReason(reason).setFinishTime(new Date()).setUpdateTime(new Date()); taskDao.updateById(task); }
}