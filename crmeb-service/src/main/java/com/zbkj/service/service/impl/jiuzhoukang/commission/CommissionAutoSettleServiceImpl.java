package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionSettleTask;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.service.jiuzhoukang.commission.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommissionAutoSettleServiceImpl implements CommissionAutoSettleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommissionAutoSettleServiceImpl.class);
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private CommissionSettleService settleService;

    @Override
    public int settleDue(int limit, Long operatorId, String triggerNo) {
        int safeLimit = limit <= 0 ? 200 : Math.min(limit, 1000);
        List<JkCommissionRecord> due = recordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                .eq(JkCommissionRecord::getStatus, "PENDING_SETTLE").eq(JkCommissionRecord::getIsDeleted, false)
                .and(q -> q.isNull(JkCommissionRecord::getFreezeEndTime).or().le(JkCommissionRecord::getFreezeEndTime, new Date()))
                .orderByAsc(JkCommissionRecord::getId).last("limit " + safeLimit));
        Map<String, List<JkCommissionRecord>> groups = due.stream().collect(Collectors.groupingBy(
                r -> r.getReceiverUserId() + ":" + r.getReceiverRoleCode(), LinkedHashMap::new, Collectors.toList()));
        int success = 0;
        for (Map.Entry<String, List<JkCommissionRecord>> entry : groups.entrySet()) {
            List<Long> ids = entry.getValue().stream().map(JkCommissionRecord::getId).sorted().collect(Collectors.toList());
            String idKey = ids.stream().map(String::valueOf).collect(Collectors.joining("-"));
            String requestNo = "AUTO_SETTLE:" + (triggerNo == null ? "SCHEDULE" : triggerNo) + ":" + Integer.toHexString(idKey.hashCode());
            try {
                JkCommissionSettleTask task = settleService.settleDueRecords(ids, operatorId, requestNo, "冻结期结束自动结算");
                if (task != null && "SUCCESS".equals(task.getStatus())) success += ids.size();
                else LOGGER.warn("九州康自动结算未成功完成，recordIds={}, taskStatus={}", ids, task == null ? null : task.getStatus());
            } catch (RuntimeException e) {
                LOGGER.error("九州康自动结算失败，recordIds={}", ids, e);
            }
        }
        return success;
    }
}
