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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 统一佣金规则试算和真实分发；草稿允许试算，但只有已发布规则允许真实入账。 */
@Service
public class CommissionScenarioServiceImpl implements CommissionScenarioService {
    @Autowired private JkCommissionRuleDao ruleDao;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private JkCommissionMatchLogDao matchLogDao;
    @Autowired private CommissionAccountService accountService;

    @Override
    public List<JkCommissionRuleTrialResponse> trial(JkCommissionRuleTrialRequest request) {
        List<JkCommissionRule> rules;
        if (request.getRuleId() != null) {
            JkCommissionRule rule = ruleDao.selectById(request.getRuleId());
            if (rule == null || Boolean.TRUE.equals(rule.getIsDeleted())) throw new IllegalArgumentException("佣金规则不存在");
            if (!rule.getSourceType().equals(request.getSourceType())) throw new IllegalArgumentException("试算业务来源与规则不一致");
            rules = Collections.singletonList(rule);
        } else {
            Date now = new Date();
            rules = ruleDao.selectList(new LambdaQueryWrapper<JkCommissionRule>()
                    .eq(JkCommissionRule::getIsDeleted, false).eq(JkCommissionRule::getStatus, true)
                    .eq(JkCommissionRule::getPublishStatus, "PUBLISHED").eq(JkCommissionRule::getSourceType, request.getSourceType())
                    .le(JkCommissionRule::getEffectiveStartTime, now)
                    .and(q -> q.isNull(JkCommissionRule::getEffectiveEndTime).or().gt(JkCommissionRule::getEffectiveEndTime, now))
                    .orderByDesc(JkCommissionRule::getPriority).orderByDesc(JkCommissionRule::getVersionNo).orderByDesc(JkCommissionRule::getId));
        }
        List<JkCommissionRuleTrialResponse> result = new ArrayList<JkCommissionRuleTrialResponse>();
        for (JkCommissionRule rule : rules) result.add(evaluate(rule, request));
        applyStackPolicy(result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(JkCommissionRuleTrialRequest request, String eventKey, String sourceNo, String requestNo) {
        if (request.getRuleId() != null) throw new IllegalArgumentException("真实分发禁止指定草稿规则");
        List<JkCommissionRuleTrialResponse> trials = trial(request);
        boolean hasPayableMatch = false;
        for (JkCommissionRuleTrialResponse trial : trials) {
            writeMatchLog(eventKey + ":" + trial.getRuleId(), request, trial);
            if (!"MATCHED".equals(trial.getMatchStatus()) || money(trial.getCappedAmount()).signum() <= 0) continue;
            hasPayableMatch = true;
            String actionKey = request.getSourceType() + ":" + request.getSourceItemId() + ":" + trial.getBeneficiaryUserId()
                    + ":" + trial.getRewardType() + ":" + trial.getRuleId() + ":" + trial.getRuleVersionNo();
            if (recordDao.selectOne(new LambdaQueryWrapper<JkCommissionRecord>()
                    .eq(JkCommissionRecord::getIdempotencyKey, actionKey).last("limit 1")) != null) continue;
            Date now = new Date();
            JkCommissionRecord record = new JkCommissionRecord().setCommissionNo("CM" + IdWorker.getIdStr())
                    .setSourceType(request.getSourceType()).setSourceId(request.getSourceId()).setSourceNo(sourceNo)
                    .setSourceItemId(request.getSourceItemId()).setReceiverUserId(trial.getBeneficiaryUserId())
                    .setReceiverRoleCode(trial.getBeneficiaryRoleCode()).setRuleId(trial.getRuleId())
                    .setRuleVersion(trial.getRuleVersionNo()).setRuleVersionNo(trial.getRuleVersionNo())
                    .setRewardType(trial.getRewardType()).setIncomeNature("PLATFORM_PAYABLE")
                    .setBaseAmount(trial.getBaseAmount()).setCommissionAmount(trial.getCappedAmount())
                    .setSettledAmount(BigDecimal.ZERO).setReversedAmount(BigDecimal.ZERO).setStatus("PENDING_SETTLE")
                    .setFreezeEndTime(addDays(now, settleDelay(trial.getRuleId())))
                    .setRuleSnapshotJson(jsonRule(trial)).setBeneficiarySnapshotJson(jsonBeneficiary(request, trial))
                    .setCalculationSnapshotJson(jsonCalculation(trial)).setIdempotencyKey(actionKey)
                    .setCommissionActionKey(actionKey).setRequestNo(requestNo).setIsDeleted(false)
                    .setCreateTime(now).setUpdateTime(now);
            try {
                recordDao.insert(record);
            } catch (DuplicateKeyException ignored) {
                continue;
            }
            accountService.creditPending(record.getReceiverUserId(), record.getReceiverRoleCode(), record.getCommissionAmount(),
                    requestNo, "COMMISSION_SCENARIO:" + actionKey);
        }
        if (!hasPayableMatch) {
            JkCommissionRuleTrialResponse noRule = new JkCommissionRuleTrialResponse().setScenario(request.getScenario())
                    .setMatchStatus(trials.isEmpty() ? "NO_ACTIVE_RULE" : "NO_PAYABLE_MATCH")
                    .setReasonCode(trials.isEmpty() ? "NO_ACTIVE_RULE" : "ALL_RULES_NOT_MATCHED")
                    .setBaseAmount(request.getBaseAmount());
            noRule.getExplanations().add("业务与业绩继续成功，但当前没有可入账的平台应付佣金");
            writeMatchLog(eventKey + ":NO_PAYABLE_MATCH", request, noRule);
        }
    }

    private JkCommissionRuleTrialResponse evaluate(JkCommissionRule rule, JkCommissionRuleTrialRequest request) {
        JkCommissionRuleTrialResponse response = new JkCommissionRuleTrialResponse().setScenario(request.getScenario())
                .setRuleId(rule.getId()).setRuleVersionNo(version(rule)).setRuleCode(rule.getRuleCode())
                .setRewardType(rule.getRewardType()).setCalculationType(rule.getCalculationType())
                .setStackPolicy(rule.getStackPolicy()).setMatchStatus("MATCHED");
        if (notBlank(rule.getRegionCode()) && !rule.getRegionCode().equals(request.getRegionCode())) return reject(response, "REGION_NOT_MATCH", "业务区域不匹配");
        if (Boolean.TRUE.equals(rule.getRequiresRegisteredCustomer()) && !Boolean.TRUE.equals(request.getRegisteredCustomer())) return reject(response, "REGISTERED_CUSTOMER_REQUIRED", "规则要求注册客户");
        if (Boolean.TRUE.equals(rule.getRequiresVoucher()) && !Boolean.TRUE.equals(request.getVoucherPresent())) return reject(response, "VOUCHER_REQUIRED", "规则要求凭证");
        if (Boolean.TRUE.equals(rule.getRequiresAudit()) && !Boolean.TRUE.equals(request.getAudited())) return reject(response, "AUDIT_REQUIRED", "规则要求审核通过");
        Long beneficiary = beneficiary(rule.getBeneficiaryType(), request);
        if (beneficiary == null) return reject(response, "BENEFICIARY_NOT_FOUND", "业务快照无法解析受益人");
        response.setBeneficiaryUserId(beneficiary).setBeneficiaryRoleCode(rule.getReceiverRoleCode());
        BigDecimal base = "REAL_GROSS_PROFIT".equals(rule.getBaseType()) ? money(request.getRealGrossProfit()) : money(request.getBaseAmount());
        BigDecimal raw = calculate(rule, request, base);
        if (raw == null) return reject(response, "CALCULATION_CONFIG_INCOMPLETE", "计算参数不完整");
        BigDecimal capped = rule.getPerOrderCap() == null ? raw : raw.min(rule.getPerOrderCap());
        response.setBaseAmount(base).setRawAmount(raw).setCappedAmount(capped.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                .setExpectedSettleDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(addDays(new Date(), rule.getSettleDelayDays())));
        response.getExplanations().add("受益人来自业务发生时快照，不读取当前关系");
        response.getExplanations().add("该结果属于 PLATFORM_PAYABLE；线下经营毛利单独记账");
        if (!"PUBLISHED".equals(rule.getPublishStatus())) response.getExplanations().add("当前为草稿试算，结果不会真实入账");
        if (rule.getPerOrderCap() != null && raw.compareTo(capped) > 0) response.getExplanations().add("已应用单笔封顶");
        return response;
    }

    private void applyStackPolicy(List<JkCommissionRuleTrialResponse> rows) {
        Map<String, List<JkCommissionRuleTrialResponse>> groups = new LinkedHashMap<String, List<JkCommissionRuleTrialResponse>>();
        for (JkCommissionRuleTrialResponse row : rows) {
            if (!"MATCHED".equals(row.getMatchStatus())) continue;
            JkCommissionRule rule = ruleDao.selectById(row.getRuleId());
            String group = rule == null || !notBlank(rule.getStackGroup()) ? "RULE:" + row.getRuleId() : rule.getStackGroup();
            if (!groups.containsKey(group)) groups.put(group, new ArrayList<JkCommissionRuleTrialResponse>());
            groups.get(group).add(row);
        }
        for (List<JkCommissionRuleTrialResponse> groupRows : groups.values()) {
            if (groupRows.size() <= 1) continue;
            JkCommissionRule first = ruleDao.selectById(groupRows.get(0).getRuleId());
            String policy = first == null ? "MAX_ONE" : defaultText(first.getStackPolicy(), "MAX_ONE");
            if ("ALLOW_STACK".equals(policy)) continue;
            Comparator<JkCommissionRuleTrialResponse> comparator = "HIGHEST_PRIORITY".equals(policy)
                    ? new Comparator<JkCommissionRuleTrialResponse>() { public int compare(JkCommissionRuleTrialResponse a, JkCommissionRuleTrialResponse b) { return Integer.compare(priority(a.getRuleId()), priority(b.getRuleId())); } }
                    : new Comparator<JkCommissionRuleTrialResponse>() { public int compare(JkCommissionRuleTrialResponse a, JkCommissionRuleTrialResponse b) { return money(a.getCappedAmount()).compareTo(money(b.getCappedAmount())); } };
            JkCommissionRuleTrialResponse winner = Collections.max(groupRows, comparator);
            for (JkCommissionRuleTrialResponse row : groupRows) {
                if (row == winner) continue;
                row.setMatchStatus("EXCLUDED_BY_STACK").setReasonCode("STACK_POLICY_" + policy);
                row.getExplanations().add("同一叠加组仅保留一条奖励");
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
                    .setReceiverUserId(trial.getBeneficiaryUserId()).setRewardType(trial.getRewardType()).setRuleId(trial.getRuleId())
                    .setRuleVersionNo(trial.getRuleVersionNo()).setMatchStatus(trial.getMatchStatus()).setReasonCode(trial.getReasonCode())
                    .setCalculationJson(jsonCalculation(trial)).setCreateTime(new Date()));
        } catch (DuplicateKeyException ignored) { }
    }

    private JkCommissionRuleTrialResponse reject(JkCommissionRuleTrialResponse response, String code, String message) { response.setMatchStatus("NOT_MATCHED").setReasonCode(code); response.getExplanations().add(message); return response; }
    private int version(JkCommissionRule rule) { return rule.getVersionNo() == null ? (rule.getRuleVersion() == null ? 1 : rule.getRuleVersion()) : rule.getVersionNo(); }
    private int priority(Long id) { JkCommissionRule rule = ruleDao.selectById(id); return rule == null || rule.getPriority() == null ? 0 : rule.getPriority(); }
    private int settleDelay(Long id) { JkCommissionRule rule = ruleDao.selectById(id); return rule == null || rule.getSettleDelayDays() == null ? 0 : rule.getSettleDelayDays(); }
    private Date addDays(Date date, Integer days) { Calendar c = Calendar.getInstance(); c.setTime(date); c.add(Calendar.DAY_OF_MONTH, days == null ? 0 : days); return c.getTime(); }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
    private String defaultText(String value, String fallback) { return notBlank(value) ? value : fallback; }
    private String jsonRule(JkCommissionRuleTrialResponse value) { return "{\"ruleId\":" + value.getRuleId() + ",\"ruleVersionNo\":" + value.getRuleVersionNo() + ",\"ruleCode\":\"" + escape(value.getRuleCode()) + "\",\"rewardType\":\"" + escape(value.getRewardType()) + "\"}"; }
    private String jsonBeneficiary(JkCommissionRuleTrialRequest request, JkCommissionRuleTrialResponse value) { return "{\"beneficiaryUserId\":" + value.getBeneficiaryUserId() + ",\"directParentUserId\":" + request.getDirectParentUserId() + ",\"countyAgentUserId\":" + request.getCountyAgentUserId() + ",\"regionCode\":\"" + escape(request.getRegionCode()) + "\"}"; }
    private String jsonCalculation(JkCommissionRuleTrialResponse value) { return "{\"matchStatus\":\"" + escape(value.getMatchStatus()) + "\",\"reasonCode\":\"" + escape(value.getReasonCode()) + "\",\"baseAmount\":" + money(value.getBaseAmount()) + ",\"rawAmount\":" + money(value.getRawAmount()) + ",\"cappedAmount\":" + money(value.getCappedAmount()) + "}"; }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
