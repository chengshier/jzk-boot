package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionReverse;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionReverseDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseSupport;
import com.zbkj.service.service.jiuzhoukang.commission.FundAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 按原佣金状态执行冲正；已提现金额形成待抵扣，历史流水不删除。 */
@Service
public class CommissionReverseServiceImpl implements CommissionReverseService {
    @Autowired private JkCommissionReverseDao reverseDao;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private CommissionAccountService accountService;
    @Autowired private FundAccountService fundAccountService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkCommissionReverse reverse(Long recordId, String sourceType, Long sourceId, String sourceNo,
                                       String reverseType, BigDecimal amount, String requestNo, Long operatorId, String reason) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("冲正金额必须大于零");
        JkCommissionReverse old = reverseDao.selectOne(new LambdaQueryWrapper<JkCommissionReverse>()
                .eq(JkCommissionReverse::getRequestNo, requestNo).last("limit 1"));
        if (old != null) return old;
        JkCommissionRecord record = recordDao.selectById(recordId);
        if (record == null || Boolean.TRUE.equals(record.getIsDeleted())) throw new IllegalArgumentException("佣金记录不存在");

        BigDecimal priorReversed = priorReversed(recordId);
        BigDecimal before = CommissionReverseSupport.remaining(record.getCommissionAmount(), priorReversed);
        CommissionReverseSupport.requireReverseAmount(before, amount);
        String status = record.getStatus();
        if ("PENDING_SETTLE".equals(status) || "PENDING".equals(status) || "CREATED".equals(status)) {
            accountService.reversePending(record.getReceiverUserId(), record.getReceiverRoleCode(), amount, requestNo,
                    "REVERSE_PENDING:" + requestNo);
        } else if ("SETTLED".equals(status)) {
            accountService.reverseSettledOrFrozen(record.getReceiverUserId(), record.getReceiverRoleCode(), amount, requestNo,
                    "REVERSE_COMMISSION_BALANCE:" + requestNo);
            fundAccountService.reverseCommissionAcrossBalances(record.getReceiverUserId(), record.getReceiverRoleCode(), amount,
                    recordId, requestNo, "REVERSE_FUND_BALANCE:" + requestNo);
        } else if ("REVERSED".equals(status)) {
            return reverseDao.selectOne(new LambdaQueryWrapper<JkCommissionReverse>()
                    .eq(JkCommissionReverse::getOriginalCommissionRecordId, recordId).orderByDesc(JkCommissionReverse::getId).last("limit 1"));
        } else {
            throw new IllegalArgumentException("当前佣金状态不能冲正：" + status);
        }

        Date now = new Date();
        BigDecimal after = before.subtract(amount);
        JkCommissionReverse reverse = new JkCommissionReverse().setReverseNo("RV" + id())
                .setOriginalCommissionRecordId(recordId).setSourceType(sourceType).setSourceId(sourceId).setSourceNo(sourceNo)
                .setReverseType(reverseType).setReverseAmount(amount).setBeforeAmount(before).setAfterAmount(after)
                .setReason(reason).setStatus("SUCCESS").setRequestNo(requestNo).setOperatorId(operatorId)
                .setCreateTime(now).setUpdateTime(now);
        reverseDao.insert(reverse);
        BigDecimal totalReversed = priorReversed.add(amount);
        String nextStatus = after.signum() == 0 ? "REVERSED"
                : ("SETTLED".equals(status) ? "SETTLED" : "PENDING_SETTLE");
        int updated = recordDao.update(null, new UpdateWrapper<JkCommissionRecord>()
                .eq("id", recordId).eq("status", status)
                .set("reversed_amount", totalReversed).set("status", nextStatus).set("update_time", now));
        if (updated != 1) throw new IllegalStateException("佣金记录状态已变化，请重试");
        return reverse;
    }

    private BigDecimal priorReversed(Long recordId) {
        List<JkCommissionReverse> reverses = reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>()
                .eq(JkCommissionReverse::getOriginalCommissionRecordId, recordId).eq(JkCommissionReverse::getStatus, "SUCCESS"));
        BigDecimal total = BigDecimal.ZERO;
        for (JkCommissionReverse reverse : reverses) total = total.add(reverse.getReverseAmount() == null ? BigDecimal.ZERO : reverse.getReverseAmount());
        return total;
    }

    private String id() { return UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
}
