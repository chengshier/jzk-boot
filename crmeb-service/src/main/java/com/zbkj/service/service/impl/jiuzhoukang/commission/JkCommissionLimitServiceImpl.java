package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionLimitReservation;
import com.zbkj.common.model.jiuzhoukang.JkCommissionLimitUsage;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionLimitReservationDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionLimitUsageDao;
import com.zbkj.service.service.jiuzhoukang.commission.JkCommissionLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 佣金周期封顶和总预算的最终数据库约束。页面预览不能替代这里的事务锁与唯一动作键。
 */
@Service
public class JkCommissionLimitServiceImpl implements JkCommissionLimitService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    @Autowired private JkCommissionLimitUsageDao usageDao;
    @Autowired private JkCommissionLimitReservationDao reservationDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationResult reserve(JkCommissionRule rule, Long beneficiaryUserId, Date businessTime,
                                     BigDecimal requestedAmount, String actionKey) {
        BigDecimal requested = money(requestedAmount);
        if (rule == null || rule.getId() == null || requested.signum() <= 0) {
            return result(requested, ZERO, "NO_AMOUNT", "无可占用奖励金额", false);
        }
        if (StrUtil.isBlank(actionKey)) throw new IllegalArgumentException("佣金限额动作键不能为空");

        JkCommissionLimitReservation existing = reservationDao.selectOne(
                new LambdaQueryWrapper<JkCommissionLimitReservation>()
                        .eq(JkCommissionLimitReservation::getActionKey, actionKey).last("limit 1"));
        if (existing != null) {
            return result(existing.getRequestedAmount(), existing.getApprovedAmount(),
                    existing.getResultCode(), existing.getResultMessage(), true);
        }

        Date now = new Date();
        JkCommissionLimitReservation reservation = new JkCommissionLimitReservation()
                .setActionKey(actionKey).setRuleId(rule.getId()).setUserId(beneficiaryUserId)
                .setPeriodKey(periodKey(rule, businessTime)).setRequestedAmount(requested)
                .setApprovedAmount(ZERO).setResultCode("RESERVING").setResultMessage("限额占用中")
                .setCreateTime(now);
        try {
            reservationDao.insert(reservation);
        } catch (DuplicateKeyException duplicate) {
            existing = reservationDao.selectOne(new LambdaQueryWrapper<JkCommissionLimitReservation>()
                    .eq(JkCommissionLimitReservation::getActionKey, actionKey).last("limit 1"));
            if (existing == null) throw duplicate;
            return result(existing.getRequestedAmount(), existing.getApprovedAmount(),
                    existing.getResultCode(), existing.getResultMessage(), true);
        }

        BigDecimal approved = requested;
        String periodKey = reservation.getPeriodKey();
        if (rule.getPerUserPeriodCap() != null && rule.getPerUserPeriodCap().signum() > 0) {
            if (beneficiaryUserId == null) approved = ZERO;
            else approved = approved.min(remaining("USER_PERIOD", rule.getId(), beneficiaryUserId,
                    periodKey, money(rule.getPerUserPeriodCap()), now));
        }
        if (rule.getTotalBudget() != null && rule.getTotalBudget().signum() > 0) {
            approved = approved.min(remaining("RULE_BUDGET", rule.getId(), null,
                    "ALL", money(rule.getTotalBudget()), now));
        }
        approved = approved.max(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);

        if (approved.signum() > 0) {
            if (rule.getPerUserPeriodCap() != null && rule.getPerUserPeriodCap().signum() > 0 && beneficiaryUserId != null) {
                consume("USER_PERIOD", rule.getId(), beneficiaryUserId, periodKey,
                        money(rule.getPerUserPeriodCap()), approved, now);
            }
            if (rule.getTotalBudget() != null && rule.getTotalBudget().signum() > 0) {
                consume("RULE_BUDGET", rule.getId(), null, "ALL", money(rule.getTotalBudget()), approved, now);
            }
        }

        String code = approved.signum() == 0 ? "LIMIT_EXHAUSTED"
                : approved.compareTo(requested) < 0 ? "PARTIAL_LIMIT" : "APPROVED";
        String message = "LIMIT_EXHAUSTED".equals(code) ? "周期封顶或规则总预算已用尽"
                : "PARTIAL_LIMIT".equals(code) ? "奖励金额已按周期封顶或总预算缩减"
                : "奖励金额已完成限额占用";
        reservation.setApprovedAmount(approved).setResultCode(code).setResultMessage(message);
        reservationDao.updateById(reservation);
        return result(requested, approved, code, message, false);
    }

    private BigDecimal remaining(String type, Long ruleId, Long userId, String periodKey,
                                 BigDecimal limit, Date now) {
        JkCommissionLimitUsage usage = lockOrCreate(type, ruleId, userId, periodKey, limit, now);
        return limit.subtract(money(usage.getUsedAmount())).max(BigDecimal.ZERO);
    }

    private void consume(String type, Long ruleId, Long userId, String periodKey,
                         BigDecimal limit, BigDecimal amount, Date now) {
        JkCommissionLimitUsage usage = lockOrCreate(type, ruleId, userId, periodKey, limit, now);
        BigDecimal next = money(usage.getUsedAmount()).add(amount);
        if (next.compareTo(limit) > 0) throw new IllegalStateException("佣金封顶或预算占用并发冲突");
        usage.setLimitAmount(limit).setUsedAmount(next)
                .setVersion(usage.getVersion() == null ? 1 : usage.getVersion() + 1).setUpdateTime(now);
        usageDao.updateById(usage);
    }

    private JkCommissionLimitUsage lockOrCreate(String type, Long ruleId, Long userId,
                                                String periodKey, BigDecimal limit, Date now) {
        JkCommissionLimitUsage usage = usageDao.selectForUpdate(type, ruleId, userId, periodKey);
        if (usage != null) return usage;
        JkCommissionLimitUsage created = new JkCommissionLimitUsage()
                .setUsageType(type).setRuleId(ruleId).setUserId(userId).setPeriodKey(periodKey)
                .setLimitAmount(limit).setUsedAmount(ZERO).setVersion(0).setCreateTime(now).setUpdateTime(now);
        try {
            usageDao.insert(created);
        } catch (DuplicateKeyException ignored) {
            // 并发创建由唯一索引裁决，随后重新加锁读取。
        }
        usage = usageDao.selectForUpdate(type, ruleId, userId, periodKey);
        if (usage == null) throw new IllegalStateException("佣金限额占用行创建失败");
        return usage;
    }

    private String periodKey(JkCommissionRule rule, Date businessTime) {
        String periodType = "MONTH";
        if (StrUtil.isNotBlank(rule.getScopeConfigJson())) {
            try {
                JSONObject scope = JSONUtil.parseObj(rule.getScopeConfigJson());
                periodType = StrUtil.blankToDefault(scope.getStr("periodType"), "MONTH");
            } catch (Exception ignored) { }
        }
        Date time = businessTime == null ? new Date() : businessTime;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        if ("YEAR".equals(periodType)) return new SimpleDateFormat("yyyy").format(time);
        if ("QUARTER".equals(periodType)) return calendar.get(Calendar.YEAR) + "-Q" + (calendar.get(Calendar.MONTH) / 3 + 1);
        return new SimpleDateFormat("yyyy-MM").format(time);
    }

    private ReservationResult result(BigDecimal requested, BigDecimal approved,
                                     String code, String message, boolean duplicate) {
        return new ReservationResult().setRequestedAmount(money(requested)).setApprovedAmount(money(approved))
                .setResultCode(code).setResultMessage(message).setDuplicate(duplicate);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
