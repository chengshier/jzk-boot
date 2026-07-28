package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRulePublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessEventDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V3.1 通用佣金规则引擎。
 * 功能完整与规则启用分离：只有 PUBLISHED + status=true 且处于生效时间的规则才会入账。
 */
@Service
public class JkCommissionV31Service {
    @Autowired private JkCommissionRuleDao ruleDao;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private JkBusinessEventDao businessEventDao;
    @Autowired private CommissionAccountService commissionAccountService;
    @Autowired private JkAuditLogService auditLogService;

    public List<JkCommissionRule> templates(String roleCode, String rewardType) {
        LambdaQueryWrapper<JkCommissionRule> query = new LambdaQueryWrapper<JkCommissionRule>()
                .eq(JkCommissionRule::getIsDeleted, false)
                .orderByAsc(JkCommissionRule::getReceiverRoleCode)
                .orderByAsc(JkCommissionRule::getRuleCode)
                .orderByDesc(JkCommissionRule::getRuleVersion);
        if (StrUtil.isNotBlank(roleCode)) query.eq(JkCommissionRule::getReceiverRoleCode, roleCode);
        if (StrUtil.isNotBlank(rewardType)) query.eq(JkCommissionRule::getRewardType, rewardType);
        return ruleDao.selectList(query);
    }

    public List<Map<String, Object>> trial(JkCommissionRuleTrialRequest request) {
        List<Candidate> candidates = evaluate(request, false);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Candidate candidate : applyStackPolicy(candidates)) result.add(candidate.asMap());
        if (result.isEmpty()) {
            Map<String, Object> none = new LinkedHashMap<String, Object>();
            none.put("matched", false);
            none.put("matchResult", "NO_ACTIVE_RULE");
            none.put("message", "当前业务场景没有可用规则；业务与业绩仍可正常完成，不生成平台应付佣金");
            result.add(none);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public JkCommissionRule publish(JkCommissionRulePublishRequest request, Long operatorId) {
        if (!Boolean.TRUE.equals(request.getTrialConfirmed())) throw new IllegalArgumentException("发布前必须完成规则试算并确认结果");
        JkCommissionRule rule = requireRule(request.getRuleId());
        validateRuleParameters(rule);
        if (request.getEffectiveEndTime() != null && !request.getEffectiveEndTime().after(request.getEffectiveStartTime())) {
            throw new IllegalArgumentException("规则结束时间必须晚于开始时间");
        }
        Date now = new Date();
        rule.setEffectiveTime(request.getEffectiveStartTime()).setExpireTime(request.getEffectiveEndTime())
                .setPublishStatus("PUBLISHED").setStatus(true).setPublishedBy(operatorId).setPublishedTime(now)
                .setRemark(StrUtil.blankToDefault(request.getRemark(), rule.getRemark())).setUpdateTime(now);
        ruleDao.updateById(rule);
        auditLogService.saveAuditLog(new JkAuditLog().setBusinessType("COMMISSION_RULE").setBusinessId(rule.getId())
                .setBusinessNo(rule.getRuleNo()).setAuditUserId(operatorId).setAuditUserType("ADMIN")
                .setAuditAction("PUBLISH").setBeforeStatus("DRAFT").setAfterStatus("PUBLISHED")
                .setAuditRemark(request.getRemark()).setOperateSource("ADMIN").setStatus(true).setIsDeleted(false)
                .setCreateUserId(operatorId).setUpdateUserId(operatorId).setCreateTime(now).setUpdateTime(now));
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    public JkCommissionRule disable(Long ruleId, Long operatorId, String reason) {
        if (StrUtil.isBlank(reason)) throw new IllegalArgumentException("停用原因不能为空");
        JkCommissionRule rule = requireRule(ruleId);
        String before = rule.getPublishStatus();
        Date now = new Date();
        rule.setStatus(false).setPublishStatus("DISABLED").setExpireTime(now).setUpdateTime(now);
        ruleDao.updateById(rule);
        auditLogService.saveAuditLog(new JkAuditLog().setBusinessType("COMMISSION_RULE").setBusinessId(rule.getId())
                .setBusinessNo(rule.getRuleNo()).setAuditUserId(operatorId).setAuditUserType("ADMIN")
                .setAuditAction("DISABLE").setBeforeStatus(before).setAfterStatus("DISABLED")
                .setAuditRemark(reason).setOperateSource("ADMIN").setStatus(true).setIsDeleted(false)
                .setCreateUserId(operatorId).setUpdateUserId(operatorId).setCreateTime(now).setUpdateTime(now));
        return rule;
    }

    /**
     * 在业务事务提交后调用。失败只记录可靠事件，不回滚原业务。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<JkCommissionRecord> createForScenario(JkCommissionRuleTrialRequest request, String requestNo) {
        List<Candidate> selected = applyStackPolicy(evaluate(request, true));
        if (selected.isEmpty()) {
            recordMatchEvent(request, requestNo, "NO_ACTIVE_RULE", null);
            return Collections.emptyList();
        }
        List<JkCommissionRecord> records = new ArrayList<JkCommissionRecord>();
        for (Candidate candidate : selected) {
            if (!candidate.selected || candidate.amount.signum() <= 0 || candidate.beneficiaryUserId == null) continue;
            JkCommissionRule rule = candidate.rule;
            String actionKey = request.getSourceType() + ":" + request.getSourceId() + ":"
                    + value(request.getSourceItemId()) + ":" + candidate.beneficiaryUserId + ":"
                    + StrUtil.blankToDefault(rule.getRewardType(), rule.getRuleCode()) + ":" + rule.getId() + ":" + rule.getRuleVersion();
            JkCommissionRecord old = recordDao.selectOne(new LambdaQueryWrapper<JkCommissionRecord>()
                    .eq(JkCommissionRecord::getCommissionActionKey, actionKey).last("limit 1"));
            if (old != null) { records.add(old); continue; }
            Date now = new Date();
            JkCommissionRecord record = new JkCommissionRecord()
                    .setCommissionNo("CM" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                    .setSourceType(request.getSourceType()).setSourceId(request.getSourceId()).setSourceNo(request.getSourceNo())
                    .setSourceItemId(request.getSourceItemId()).setReceiverUserId(candidate.beneficiaryUserId)
                    .setReceiverRoleCode(candidate.beneficiaryRoleCode).setRuleId(rule.getId())
                    .setRuleVersion(rule.getRuleVersion()).setRuleVersionNo(rule.getRuleVersion())
                    .setRewardType(rule.getRewardType()).setBaseAmount(candidate.baseAmount)
                    .setCommissionAmount(candidate.amount).setSettledAmount(BigDecimal.ZERO)
                    .setReversedAmount(BigDecimal.ZERO).setNegativeOffsetAmount(BigDecimal.ZERO)
                    .setIncomeNature("PLATFORM_PAYABLE").setStatus("PENDING_SETTLE")
                    .setFreezeEndTime(addDays(now, rule.getSettleDelayDays() == null ? rule.getFreezeDays() : rule.getSettleDelayDays()))
                    .setRuleSnapshotJson(ruleSnapshot(rule)).setRelationSnapshotJson(request.getRelationSnapshotJson())
                    .setSourceSnapshotJson(request.getSourceSnapshotJson()).setCalculationSnapshotJson(candidate.calculationJson())
                    .setCommissionActionKey(actionKey).setIdempotencyKey(actionKey).setRequestNo(requestNo + ":" + rule.getId())
                    .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
            try {
                recordDao.insert(record);
                commissionAccountService.creditPending(candidate.beneficiaryUserId, candidate.beneficiaryRoleCode,
                        candidate.amount, requestNo, "COMMISSION_V31:" + actionKey);
                records.add(record);
                recordMatchEvent(request, requestNo + ":" + rule.getId(), "MATCHED", candidate);
            } catch (DuplicateKeyException ignored) {
                JkCommissionRecord exists = recordDao.selectOne(new LambdaQueryWrapper<JkCommissionRecord>()
                        .eq(JkCommissionRecord::getCommissionActionKey, actionKey).last("limit 1"));
                if (exists != null) records.add(exists);
            }
        }
        return records;
    }

    private List<Candidate> evaluate(JkCommissionRuleTrialRequest request, boolean activeOnly) {
        Date now = new Date();
        LambdaQueryWrapper<JkCommissionRule> query = new LambdaQueryWrapper<JkCommissionRule>()
                .eq(JkCommissionRule::getIsDeleted, false)
                .eq(JkCommissionRule::getSourceType, request.getSourceType())
                .orderByDesc(JkCommissionRule::getPriority).orderByDesc(JkCommissionRule::getRuleVersion);
        if (request.getRuleId() != null) query.eq(JkCommissionRule::getId, request.getRuleId());
        if (activeOnly) {
            query.eq(JkCommissionRule::getStatus, true).eq(JkCommissionRule::getPublishStatus, "PUBLISHED")
                    .le(JkCommissionRule::getEffectiveTime, now)
                    .and(q -> q.isNull(JkCommissionRule::getExpireTime).or().gt(JkCommissionRule::getExpireTime, now));
        }
        List<Candidate> result = new ArrayList<Candidate>();
        for (JkCommissionRule rule : ruleDao.selectList(query)) {
            Long beneficiary = resolveBeneficiary(rule.getBeneficiaryType(), request);
            String beneficiaryRole = roleOf(beneficiary);
            Candidate candidate = new Candidate(rule, beneficiary, beneficiaryRole);
            if (beneficiary == null) { candidate.reason = "BENEFICIARY_NOT_FOUND"; result.add(candidate); continue; }
            if (StrUtil.isNotBlank(rule.getReceiverRoleCode()) && !rule.getReceiverRoleCode().equals(beneficiaryRole)) {
                candidate.reason = "RECEIVER_ROLE_NOT_MATCH"; result.add(candidate); continue;
            }
            if (StrUtil.isNotBlank(rule.getRegionCode()) && !rule.getRegionCode().equals(request.getRegionCode())) {
                candidate.reason = "REGION_NOT_MATCH"; result.add(candidate); continue;
            }
            if (Boolean.TRUE.equals(rule.getRequiresRegisteredCustomer()) && !Boolean.TRUE.equals(request.getRegisteredCustomer())) {
                candidate.reason = "REGISTERED_CUSTOMER_REQUIRED"; result.add(candidate); continue;
            }
            if (Boolean.TRUE.equals(rule.getRequiresVoucher()) && !Boolean.TRUE.equals(request.getVoucherPresent())) {
                candidate.reason = "VOUCHER_REQUIRED"; result.add(candidate); continue;
            }
            if (Boolean.TRUE.equals(rule.getRequiresAudit()) && !Boolean.TRUE.equals(request.getAudited())) {
                candidate.reason = "AUDIT_REQUIRED"; result.add(candidate); continue;
            }
            candidate.baseAmount = resolveBase(rule.getBaseType(), request);
            candidate.amount = calculate(rule, candidate.baseAmount, request.getQuantity());
            if (rule.getPerOrderCap() != null) candidate.amount = candidate.amount.min(rule.getPerOrderCap());
            candidate.amount = candidate.amount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            candidate.matched = true;
            candidate.reason = "MATCHED";
            result.add(candidate);
        }
        return result;
    }

    private List<Candidate> applyStackPolicy(List<Candidate> candidates) {
        Map<String, List<Candidate>> groups = new LinkedHashMap<String, List<Candidate>>();
        for (Candidate candidate : candidates) {
            if (!candidate.matched) continue;
            String group = StrUtil.blankToDefault(candidate.rule.getStackGroup(), "RULE:" + candidate.rule.getId());
            groups.computeIfAbsent(group, key -> new ArrayList<Candidate>()).add(candidate);
        }
        List<Candidate> selected = new ArrayList<Candidate>();
        for (List<Candidate> rows : groups.values()) {
            String policy = StrUtil.blankToDefault(rows.get(0).rule.getStackPolicy(), "MAX_ONE");
            if ("ALLOW_STACK".equals(policy)) {
                for (Candidate row : rows) { row.selected = true; selected.add(row); }
                continue;
            }
            Comparator<Candidate> comparator = "HIGHEST_AMOUNT".equals(policy)
                    ? Comparator.comparing(c -> c.amount)
                    : Comparator.comparing(c -> c.rule.getPriority() == null ? 0 : c.rule.getPriority());
            Candidate best = Collections.max(rows, comparator);
            best.selected = true;
            selected.add(best);
        }
        return selected;
    }

    private Long resolveBeneficiary(String type, JkCommissionRuleTrialRequest request) {
        String value = StrUtil.blankToDefault(type, "PERFORMANCE_OWNER");
        if ("DIRECT_PARENT_SNAPSHOT".equals(value)) return request.getDirectParentUserId();
        if ("COUNTY_AGENT_SNAPSHOT".equals(value)) return request.getCountyAgentUserId();
        if ("SELLER_SNAPSHOT".equals(value) || "TRANSFER_SENDER_SNAPSHOT".equals(value)) return request.getSellerUserId();
        if ("PURCHASER_SNAPSHOT".equals(value)) return request.getPurchaserUserId();
        if ("TRANSFER_RECEIVER_SNAPSHOT".equals(value)) return request.getOwnerUserId();
        return request.getOwnerUserId();
    }

    private String roleOf(Long userId) {
        if (userId == null) return null;
        JkUserBusinessRole role = userRoleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, userId)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getEffectiveStatus, JkBizConstants.EFFECTIVE_STATUS_ENABLED)
                .eq(JkUserBusinessRole::getFreezeStatus, false).eq(JkUserBusinessRole::getStatus, true)
                .eq(JkUserBusinessRole::getIsDeleted, false).orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId).last("limit 1"));
        return role == null ? null : role.getRoleCode();
    }

    private BigDecimal resolveBase(String baseType, JkCommissionRuleTrialRequest request) {
        if ("VALID_QUANTITY".equals(baseType)) return BigDecimal.valueOf(request.getQuantity() == null ? 0 : request.getQuantity());
        if ("REAL_GROSS_PROFIT".equals(baseType)) return money(request.getBaseAmount()).subtract(money(request.getCostAmount())).max(BigDecimal.ZERO);
        return money(request.getBaseAmount());
    }

    private BigDecimal calculate(JkCommissionRule rule, BigDecimal base, Integer quantity) {
        String type = StrUtil.blankToDefault(rule.getCalculationType(), "PERCENT");
        if ("PERCENT".equals(type) || "TIER_PERCENT".equals(type)) {
            BigDecimal rate = rule.getRate();
            if (rate == null) return BigDecimal.ZERO;
            return base.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        if ("FIXED_PER_ORDER".equals(type)) return money(rule.getFixedAmount());
        if ("FIXED_PER_ITEM".equals(type) || "FIXED_PER_QUANTITY".equals(type)) {
            return money(rule.getUnitAmount()).multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity));
        }
        return BigDecimal.ZERO;
    }

    private void validateRuleParameters(JkCommissionRule rule) {
        if (StrUtil.isBlank(rule.getRuleCode()) || StrUtil.isBlank(rule.getRewardType()) || StrUtil.isBlank(rule.getSourceType())) {
            throw new IllegalArgumentException("规则编码、奖励类型和业务来源不能为空");
        }
        if (StrUtil.isBlank(rule.getBeneficiaryType()) || StrUtil.isBlank(rule.getBaseType()) || StrUtil.isBlank(rule.getCalculationType())) {
            throw new IllegalArgumentException("受益人、计算基数和计算方式不能为空");
        }
        String type = rule.getCalculationType();
        if (("PERCENT".equals(type) || "TIER_PERCENT".equals(type)) && rule.getRate() == null) throw new IllegalArgumentException("比例规则必须填写比例");
        if ("FIXED_PER_ORDER".equals(type) && rule.getFixedAmount() == null) throw new IllegalArgumentException("固定规则必须填写金额");
        if (("FIXED_PER_ITEM".equals(type) || "FIXED_PER_QUANTITY".equals(type)) && rule.getUnitAmount() == null) throw new IllegalArgumentException("按件规则必须填写单位金额");
    }

    private JkCommissionRule requireRule(Long id) {
        JkCommissionRule rule = ruleDao.selectById(id);
        if (rule == null || Boolean.TRUE.equals(rule.getIsDeleted())) throw new IllegalArgumentException("佣金规则不存在");
        return rule;
    }

    private void recordMatchEvent(JkCommissionRuleTrialRequest request, String requestNo, String result, Candidate candidate) {
        String key = "COMMISSION_MATCH:" + requestNo;
        if (businessEventDao.selectOne(new LambdaQueryWrapper<JkBusinessEvent>().eq(JkBusinessEvent::getEventKey, key).last("limit 1")) != null) return;
        Date now = new Date();
        String payload = "{\"result\":\"" + result + "\",\"sourceType\":\"" + escape(request.getSourceType())
                + "\",\"sourceId\":" + request.getSourceId() + ",\"ruleId\":" + (candidate == null ? "null" : candidate.rule.getId()) + "}";
        try {
            businessEventDao.insert(new JkBusinessEvent().setEventKey(key).setEventType("COMMISSION_MATCH")
                    .setBusinessId(request.getSourceId()).setBusinessNo(request.getSourceNo()).setPayloadJson(payload)
                    .setEventStatus("SUCCESS").setRetryCount(0).setMaxRetryCount(8).setOccurredTime(now)
                    .setProcessedTime(now).setCreateTime(now).setUpdateTime(now));
        } catch (DuplicateKeyException ignored) { }
    }

    private String ruleSnapshot(JkCommissionRule rule) {
        return "{\"ruleId\":" + rule.getId() + ",\"ruleCode\":\"" + escape(rule.getRuleCode())
                + "\",\"version\":" + rule.getRuleVersion() + ",\"rewardType\":\"" + escape(rule.getRewardType())
                + "\",\"calculationType\":\"" + escape(rule.getCalculationType()) + "\"}";
    }

    private Date addDays(Date value, Integer days) {
        if (days == null || days <= 0) return value;
        Calendar calendar = Calendar.getInstance(); calendar.setTime(value); calendar.add(Calendar.DAY_OF_MONTH, days); return calendar.getTime();
    }
    private long value(Long id) { return id == null ? 0L : id; }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static final class Candidate {
        private final JkCommissionRule rule;
        private final Long beneficiaryUserId;
        private final String beneficiaryRoleCode;
        private BigDecimal baseAmount = BigDecimal.ZERO;
        private BigDecimal amount = BigDecimal.ZERO;
        private boolean matched;
        private boolean selected;
        private String reason;
        private Candidate(JkCommissionRule rule, Long beneficiaryUserId, String beneficiaryRoleCode) {
            this.rule = rule; this.beneficiaryUserId = beneficiaryUserId; this.beneficiaryRoleCode = beneficiaryRoleCode;
        }
        private Map<String, Object> asMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("matched", matched); map.put("selected", selected); map.put("matchResult", reason);
            map.put("ruleId", rule.getId()); map.put("ruleCode", rule.getRuleCode()); map.put("ruleName", rule.getRuleName());
            map.put("rewardType", rule.getRewardType()); map.put("beneficiaryUserId", beneficiaryUserId);
            map.put("beneficiaryRoleCode", beneficiaryRoleCode); map.put("baseAmount", baseAmount); map.put("rawAmount", amount);
            map.put("cappedAmount", amount); map.put("stackGroup", rule.getStackGroup()); map.put("stackPolicy", rule.getStackPolicy());
            map.put("estimatedSettleDelayDays", rule.getSettleDelayDays()); return map;
        }
        private String calculationJson() {
            return "{\"baseAmount\":" + baseAmount + ",\"amount\":" + amount + ",\"selected\":" + selected
                    + ",\"stackPolicy\":\"" + (rule.getStackPolicy() == null ? "MAX_ONE" : rule.getStackPolicy()) + "\"}";
        }
    }
}
