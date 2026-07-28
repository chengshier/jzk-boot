package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.model.jiuzhoukang.JkCommissionMatchLog;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.response.jiuzhoukang.JkCommissionRuleTrialResponse;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionMatchLogDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 第二批统一佣金规则执行器。
 * 只有 status=true、publish_status=PUBLISHED 且处于生效时间窗的规则才允许真实入账。
 */
@Service
public class CommissionScenarioServiceImpl implements CommissionScenarioService {
    @Autowired private JkCommissionRuleDao ruleDao;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private JkCommissionMatchLogDao matchLogDao;
    @Autowired private CommissionAccountService accountService;

    @Override
    public List<JkCommissionRuleTrialResponse> trial(JkCommissionRuleTrialRequest request) {
        Date now = new Date();
        LambdaQueryWrapper<JkCommissionRule> query = new LambdaQueryWrapper<JkCommissionRule>()
                .eq(JkCommissionRule::getIsDeleted, false)
                .eq(JkCommissionRule::getStatus, true)
                .eq(JkCommissionRule::getPublishStatus, "PUBLISHED")
                .eq(JkCommissionRule::getSourceType, request.getSourceType())
                .le(JkCommissionRule::getEffectiveStartTime, now)
                .and(q -> q.isNull(JkCommissionRule::getEffectiveEndTime).or().gt(JkCommissionRule::getEffectiveEndTime, now))
                .orderByDesc(JkCommissionRule::getPriority).orderByDesc(JkCommissionRule::getVersionNo).orderByDesc(JkCommissionRule::getId);
        if (request.getRuleId() != null) query.eq(JkCommissionRule::getId, request.getRuleId());
        List<JkCommissionRuleTrialResponse> result = new ArrayList<JkCommissionRuleTrialResponse>();
        for (JkCommissionRule rule : ruleDao.selectList(query)) result.add(evaluate(rule, request));
        applyStackPolicy(result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(JkCommissionRuleTrialRequest request, String eventKey, String sourceNo, String requestNo) {
        List<JkCommissionRuleTrialResponse> trials = trial(request);
        boolean matched = false;
        for (JkCommissionRuleTrialResponse trial : trials) {
            String logKey = eventKey + ":" + (trial.getRuleId() == null ? "NONE" : trial.getRuleId());
            writeMatchLog(logKey, request, trial);
            if (!"MATCHED".equals(trial.getMatchStatus()) || trial.getCappedAmount() == null || trial.getCappedAmount().signum() <= 0) continue;
            matched = true;
            String actionKey = request.getSourceType() + ":" + request.getSourceItemId() + ":" + trial.getBeneficiaryUserId()
                    + ":" + trial.getRewardType() + ":" + trial.getRuleId() + ":" + trial.getRuleVersionNo();
            JkCommissionRecord old = recordDao.selectOne(new LambdaQueryWrapper<JkCommissionRecord>()
                    .eq(JkCommissionRecord::getIdempotencyKey, actionKey).last("limit 1"));
            if (old != null) continue;
            Date now = new Date();
            Date freezeEnd = addDays(now, settleDelay(trial.getRuleId()));
            JkCommissionRecord record = new JkCommissionRecord().setCommissionNo("CM" + IdWorker.getIdStr())
                    .setSourceType(request.getSourceType()).setSourceId(request.getSourceId()).setSourceNo(sourceNo)
                    .setSourceItemId(request.getSourceItemId()).setReceiverUserId(trial.getBeneficiaryUserId())
                    .setReceiverRoleCode(trial.getBeneficiaryRoleCode()).setRuleId(trial.getRuleId())
                    .setRuleVersion(trial.getRuleVersionNo()).setRuleVersionNo(trial.getRuleVersionNo())
                    .setRewardType(trial.getRewardType()).setIncomeNature("PLATFORM_PAYABLE")
                    .setBaseAmount(trial.getBaseAmount()).setCommissionAmount(trial.getCappedAmount())
                    .setSettledAmount(BigDecimal.ZERO).setReversedAmount(BigDecimal.ZERO).setStatus("PENDING")
                    .setFreezeEndTime(freezeEnd).setRuleSnapshotJson(jsonRule(trial))
                    .setBeneficiarySnapshotJson(jsonBeneficiary(request, trial))
                    .setCalculationSnapshotJson(jsonCalculation(trial)).setIdempotencyKey(actionKey)
                    .setCommissionActionKey(actionKey).setRequestNo(requestNo).setIsDeleted(false)
                    .setCreateTime(now).setUpdateTime(now);
            try {
                recordDao.insert(record);
                accountService.creditPending(record.getReceiverUserId(), record.getReceiverRoleCode(), record.getCommissionAmount(),
                        requestNo, "COMMISSION_SCENARIO:" + actionKey);
            } catch (DuplicateKeyException ignored) {
                // 并发事件由幂等键收口。
            }
        }
        if (!matched && trials.isEmpty()) {
            JkCommissionRuleTrialResponse noRule = new JkCommissionRuleTrialResponse().setScenario(request.getScenario())
                    .setMatchStatus("NO_ACTIVE_RULE").setReasonCode("NO_ACTIVE_RULE").setBaseAmount(request.getBaseAmount());
            noRule.getExplanations().add("当前场景没有已审核发布且处于生效时间窗的规则，业务和业绩继续成功，但不生成可提现佣金");
            writeMatchLog(eventKey + ":NO_ACTIVE_RULE", request, noRule);
        }
    }

    private JkCommissionRuleTrialResponse evaluate(JkCommissionRule rule, JkCommissionRuleTrialRequest request) {
        JkCommissionRuleTrialResponse response = new JkCommissionRuleTrialResponse().setScenario(request.getScenario())
                .setRuleId(rule.getId()).setRuleVersionNo(version(rule)).setRuleCode(rule.getRuleCode())
                .setRewardType(rule.getRewardType()).setCalculationType(rule.getCalculationType())
                .setStackPolicy(rule.getStackPolicy()).setMatchStatus("MATCHED");
        if (rule.getRegionCode() != null && !rule.getRegionCode().trim().isEmpty() && !rule.getRegionCode().equals(request.getRegionCode())) {
            return reject(response, "REGION_NOT_MATCH", "业务区域不在规则适用范围");
        }
        if (Boolean.TRUE.equals(rule.getRequiresRegisteredCustomer()) && !Boolean.TRUE.equals(request.getRegisteredCustomer())) {
            return reject(response, "REGISTERED_CUSTOMER_REQUIRED", "规则要求客户已注册");
        }
        if (Boolean.TRUE.equals(rule.getRequiresVoucher()) && !Boolean.TRUE.equals(request.getVoucherPresent())) {
            return reject(response, "VOUCHER_REQUIRED", "规则要求销售或付款凭证");
        }
        if (Boolean.TRUE.equals(rule.getRequiresAudit()) && !Boolean.TRUE.equals(request.getAudited())) {
            return reject(response, "AUDIT_REQUIRED", "规则要求业务审核通过");
        }
        Long beneficiary = beneficiary(rule.getBeneficiaryType(), request);
        if (beneficiary == null) return reject(response, "BENEFICIARY_NOT_FOUND", "业务快照中无法解析受益人");
        response.setBeneficiaryUserId(beneficiary).setBeneficiaryRoleCode(rule.getReceiverRoleCode());
        BigDecimal base = "REAL_GROSS_PROFIT".equals(rule.getBaseType()) ? money(request.getRealGrossProfit()) : money(request.getBaseAmount());
        response.setBaseAmount(base);
        BigDecimal amount = calculate(rule, request, base);
        if (amount == null) return reject(response, "CALCULATION_CONFIG_INCOMPLETE", "规则计算参数未配置完整");
        response.setRawAmount(amount);
        BigDecimal capped = rule.getPerOrderCap() == null ? amount : amount.min(rule.getPerOrderCap());
        response.setCappedAmount(capped.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        response.setExpectedSettleDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(addDays(new Date(), rule.getSettleDelayDays())));
        response.getExplanations().add("仅使用业务发生时传入的来源和关系快照解析受益人");
        response.getExplanations().add("收入性质为 PLATFORM_PAYABLE；线下经营毛利不会写入佣金账户");
        if (rule.getPerOrderCap() != null && amount.compareTo(capped) > 0) response.getExplanations().add("已应用单笔封顶");
        return response;
    }

    private void applyStackPolicy(List<JkCommissionRuleTrialResponse> rows) {
        Map<String, List<JkCommissionRuleTrialResponse>> groups = new LinkedHashMap<String, List<JkCommissionRuleTrialResponse>>();
        for (JkCommissionRuleTrialResponse row : rows) {
            if (!"MATCHED".equals(row.getMatchStatus())) continue;
            JkCommissionRule rule = ruleDao.selectById(row.getRuleId());
            String group = rule == null || rule.getStackGroup() == null || rule.getStackGroup().trim().isEmpty()
                    ? "RULE:" + row.getRuleId() : rule.getStackGroup();
            groups.computeIfAbsent(group, k -> new ArrayList<JkCommissionRuleTrialResponse>()).add(row);
        }
        for (List<JkCommissionRuleTrialResponse> groupRows : groups.values()) {
            if (groupRows.size() <= 1) continue;
            JkCommissionRule firstRule = ruleDao.selectById(groupRows.get(0).getRuleId());
            String policy = firstRule == null ? "MAX_ONE" : defaultText(firstRule.getStackPolicy(), "MAX_ONE");
            if ("ALLOW_STACK".equals(policy)) continue;
            Comparator<JkCommissionRuleTrialResponse> comparator;
            if ("HIGHEST_PRIORITY".equals(policy)) {
                comparator = Comparator.comparingInt(v -> priority(v.getRuleId()));
            } else {
                comparator = Comparator.comparing(v -> money(v.getCappedAmount()));
            }
            JkCommissionRuleTrialResponse winner = Collections.max(groupRows, comparator);
            for (JkCommissionRuleTrialResponse row : groupRows) {
                if (row == winner) continue;
                row.setMatchStatus("EXCLUDED_BY_STACK").setReasonCode("STACK_POLICY_" + policy);
                row.getExplanations().add("同一叠加组按 " + policy + " 仅保留一条奖励");
            }
        }
    }

    private BigDecimal calculate(JkCommissionRule rule, JkCommissionRuleTrialRequest request, BigDecimal base) {
        String type = defaultText(rule.getCalculationType(), "PERCENT");
        if ("PERCENT".equals(type)) return rule.getRate() == null ? null : base.multiply(rule.getRate()).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        if ("FIXED_PER_ORDER".equals(type)) return rule.getFixedAmount();
        if ("FIXED_PER_ITEM".equals(type)) return rule.getFixedAmount() == null ? null : rule.getFixedAmount().multiply(new BigDecimal(Math.max(1, request.getQuantity() == null ? 1 : request.getQuantity())));
        if ("FIXED_PER_QUANTITY".equals(type)) return rule.getUnitAmount() == null ? null : rule.getUnitAmount().multiply(new BigDecimal(Math.max(0, request.getQuantity() == null ? 0 : request.getQuantity())));
        return null;
    }

    private Long beneficiary(String type, JkCommissionRuleTrialRequest request) {
        if ("DIRECT_PARENT_SNAPSHOT".equals(type)) return request.getDirectParentUserId();
        if ("COUNTY_AGENT_SNAPSHOT".equals(type)) return request.getCountyAgentUserId();
        if ("SELLER_SNAPSHOT".equals(type) || "TRANSFER_SENDER_SNAPSHOT".equals(type) || "PERFORMANCE_OWNER".equals(type)) return request.getSellerUserId();
        if ("PURCHASER_SNAPSHOT".equals(type) || "TRANSFER_RECEIVER_SNAPSHOT".equals(type)) return request.getBuyerUserId();
        return null;
    }

    private void writeMatchLog(String eventKey, JkCommissionRuleTrialRequest request, JkCommissionRuleTrialResponse trial) {
        if (matchLogDao.selectOne(new LambdaQueryWrapper<JkCommissionMatchLog>().eq(JkCommissionMatchLog::getEventKey, eventKey).last("limit 1")) != null) return;
        try {
            matchLogDao.insert(new JkCommissionMatchLog().setEventKey(eventKey).setScenario(request.getScenario())
                    .setSourceType(request.getSourceType()).setSourceId(request.getSourceId()).setSourceItemId(request.getSourceItemId())
                    .setReceiverUserId(trial.getBeneficiaryUserId()).setRewardType(trial.getRewardType())
                    .setRuleId(trial.getRuleId()).setRuleVersionNo(trial.getRuleVersionNo()).setMatchStatus(trial.getMatchStatus())
                    .setReasonCode(trial.getReasonCode()).setCalculationJson(jsonCalculation(trial)).setCreateTime(new Date()));
        } catch (DuplicateKeyException ignored) { }
    }

    private JkCommissionRuleTrialResponse reject(JkCommissionRuleTrialResponse response, String code, String message) {
        response.setMatchStatus("NOT_MATCHED").setReasonCode(code); response.getExplanations().add(message); return response;
    }
    private int version(JkCommissionRule rule) { return rule.getVersionNo() == null ? (rule.getRuleVersion() == null ? 1 : rule.getRuleVersion()) : rule.getVersionNo(); }
    private int priority(Long ruleId) { JkCommissionRule rule = ruleDao.selectById(ruleId); return rule == null || rule.getPriority() == null ? 0 : rule.getPriority(); }
    private int settleDelay(Long ruleId) { JkCommissionRule rule = ruleDao.selectById(ruleId); return rule == null || rule.getSettleDelayDays() == null ? 0 : rule.getSettleDelayDays(); }
    private Date addDays(Date date, Integer days) { java.util.Calendar c = java.util.Calendar.getInstance(); c.setTime(date); c.add(java.util.Calendar.DAY_OF_MONTH, days == null ? 0 : days); return c.getTime(); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String defaultText(String value, String defaultValue) { return value == null || value.trim().isEmpty() ? defaultValue : value; }
    private String jsonRule(JkCommissionRuleTrialResponse trial) { return "{\"ruleId\":" + trial.getRuleId() + ",\"ruleVersionNo\":" + trial.getRuleVersionNo() + ",\"ruleCode\":\"" + escape(trial.getRuleCode()) + "\",\"rewardType\":\"" + escape(trial.getRewardType()) + "\"}"; }
    private String jsonBeneficiary(JkCommissionRuleTrialRequest request, JkCommissionRuleTrialResponse trial) { return "{\"beneficiaryUserId\":" + trial.getBeneficiaryUserId() + ",\"directParentUserId\":" + request.getDirectParentUserId() + ",\"countyAgentUserId\":" + request.getCountyAgentUserId() + ",\"regionCode\":\"" + escape(request.getRegionCode()) + "\"}"; }
    private String jsonCalculation(JkCommissionRuleTrialResponse trial) { return "{\"matchStatus\":\"" + escape(trial.getMatchStatus()) + "\",\"reasonCode\":\"" + escape(trial.getReasonCode()) + "\",\"baseAmount\":" + money(trial.getBaseAmount()) + ",\"rawAmount\":" + money(trial.getRawAmount()) + ",\"cappedAmount\":" + money(trial.getCappedAmount()) + "}"; }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
