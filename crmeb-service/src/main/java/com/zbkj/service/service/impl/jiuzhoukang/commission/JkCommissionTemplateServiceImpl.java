package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRulePlan;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionTemplateSaveRequest;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRulePlanDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import com.zbkj.service.service.jiuzhoukang.commission.JkCommissionTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 业务模板负责生成受控技术规则；普通运营不接触底层技术枚举。 */
@Service
public class JkCommissionTemplateServiceImpl implements JkCommissionTemplateService {
    @Autowired private JkBusinessRulePlanDao planDao;
    @Autowired private CommissionRuleService ruleService;

    @Override
    public List<Map<String, Object>> templates(String receiverRoleCode) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (TemplateDefinition definition : definitions()) {
            if (StrUtil.isNotBlank(receiverRoleCode) && !definition.roles.contains(receiverRoleCode)) continue;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("templateCode", definition.code);
            item.put("templateName", definition.name);
            item.put("description", definition.description);
            item.put("receiverRoles", definition.roles);
            item.put("supportsPeriod", definition.periodic);
            item.put("defaultStatus", "DISABLED");
            item.put("businessFields", definition.periodic
                    ? Arrays.asList("适用身份", "统计周期", "业绩门槛", "奖励方式和数值", "周期封顶", "备注")
                    : Arrays.asList("适用身份", "奖励方式和数值", "适用商品", "适用区域", "单笔封顶", "备注"));
            item.put("advanced", advanced(definition));
            result.add(item);
        }
        return result;
    }

    @Override
    public JkCommissionRule saveDraft(JkCommissionTemplateSaveRequest request) {
        JkBusinessRulePlan plan = planDao.selectById(request.getPlanId());
        if (plan == null || Boolean.TRUE.equals(plan.getIsDeleted())) throw new IllegalArgumentException("商业方案不存在");
        if (!"DRAFT".equals(plan.getPublishStatus())) throw new IllegalArgumentException("只能在商业方案草稿版本中配置奖励");
        TemplateDefinition template = definition(request.getTemplateCode());
        if (!template.roles.contains(request.getReceiverRoleCode())) throw new IllegalArgumentException("该奖励模板不适用于所选身份");
        validateReward(request, template);

        Map<String, Object> scope = new LinkedHashMap<String, Object>();
        scope.put("productIds", request.getProductIds() == null ? new ArrayList<Integer>() : request.getProductIds());
        scope.put("regionCodes", request.getRegionCodes() == null ? new ArrayList<String>() : request.getRegionCodes());
        scope.put("performanceThreshold", request.getPerformanceThreshold());
        scope.put("periodType", request.getPeriodType());

        JkCommissionRuleSaveRequest save = new JkCommissionRuleSaveRequest();
        save.setId(request.getRuleId());
        save.setPlanId(plan.getId());
        save.setPlanCode(plan.getPlanCode());
        save.setPlanVersionNo(plan.getVersionNo());
        save.setVersionNo(plan.getVersionNo());
        save.setTemplateCode(template.code);
        save.setRuleCode(plan.getPlanCode() + "_V" + plan.getVersionNo() + "_" + template.code + "_" + request.getReceiverRoleCode());
        save.setRuleName(request.getRuleName());
        save.setRuleVersion(plan.getVersionNo());
        save.setSourceType(template.sourceType);
        save.setRewardType(template.rewardType);
        save.setPerformanceType(template.performanceType);
        save.setReceiverRoleCode(request.getReceiverRoleCode());
        save.setBeneficiaryType(template.beneficiaryType);
        save.setBaseType(template.baseType);
        save.setCalculationType(template.tier ? "TIER_PERCENT" : request.getRewardMode());
        save.setTriggerTiming(template.triggerTiming);
        save.setStackGroup(request.getReceiverRoleCode() + ":" + template.code);
        save.setStackPolicy("MAX_ONE");
        save.setPriority(100);
        save.setPerOrderCap(request.getPerOrderCap());
        save.setPerUserPeriodCap(request.getPerUserPeriodCap());
        save.setSettleDelayDays(request.getSettleDelayDays() == null ? 0 : request.getSettleDelayDays());
        save.setRequiresRegisteredCustomer(template.requiresRegisteredCustomer);
        save.setRequiresVoucher(template.requiresVoucher);
        save.setRequiresAudit(template.requiresAudit);
        save.setScopeConfigJson(JSONUtil.toJsonStr(scope));
        save.setRuleConfigJson(JSONUtil.toJsonStr(scope));
        save.setIncomeNature("PLATFORM_PAYABLE");
        save.setRegionCode(request.getRegionCodes() != null && request.getRegionCodes().size() == 1 ? request.getRegionCodes().get(0) : null);
        save.setRemark(request.getRemark());
        applyReward(save, request, template);
        return ruleService.saveRule(save);
    }

    private void applyReward(JkCommissionRuleSaveRequest save, JkCommissionTemplateSaveRequest request, TemplateDefinition template) {
        if (template.tier || "PERCENT".equals(request.getRewardMode())) save.setRate(request.getRewardValue());
        else if ("FIXED_PER_ITEM".equals(request.getRewardMode())) save.setFixedAmount(request.getRewardValue());
        else save.setFixedAmount(request.getRewardValue());
    }

    private void validateReward(JkCommissionTemplateSaveRequest request, TemplateDefinition template) {
        if (request.getRewardValue() == null || request.getRewardValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("奖励数值不能小于 0");
        }
        if (template.tier) {
            if (request.getPerformanceThreshold() == null || request.getPerformanceThreshold().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("阶梯奖励必须填写大于 0 的业绩门槛");
            }
            if (StrUtil.isBlank(request.getPeriodType())) throw new IllegalArgumentException("阶梯奖励必须选择统计周期");
            return;
        }
        if (!Arrays.asList("PERCENT", "FIXED_PER_ORDER", "FIXED_PER_ITEM").contains(request.getRewardMode())) {
            throw new IllegalArgumentException("奖励方式不受支持");
        }
    }

    private Map<String, Object> advanced(TemplateDefinition definition) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("sourceType", definition.sourceType);
        map.put("rewardType", definition.rewardType);
        map.put("beneficiaryType", definition.beneficiaryType);
        map.put("baseType", definition.baseType);
        map.put("triggerTiming", definition.triggerTiming);
        map.put("incomeNature", "PLATFORM_PAYABLE");
        return map;
    }

    private TemplateDefinition definition(String code) {
        for (TemplateDefinition definition : definitions()) if (definition.code.equals(code)) return definition;
        throw new IllegalArgumentException("不支持的奖励模板");
    }

    private List<TemplateDefinition> definitions() {
        return Arrays.asList(
                new TemplateDefinition("DIRECT_REFERRAL", "直属推荐奖励", "普通用户完成有效终端购买后，奖励下单时直属推荐人。",
                        Arrays.asList("maker", "partner", "county_agent"), "RETAIL_ORDER", "DIRECT_RETAIL", null,
                        "DIRECT_PARENT_SNAPSHOT", "ITEM_PAID_AMOUNT", "ORDER_COMPLETED", true, false, false, false),
                new TemplateDefinition("SELF_RETAIL", "自营销售奖励", "创客或合伙人完成经核验的终端销售后，平台额外给予奖励。",
                        Arrays.asList("maker", "partner"), "OFFLINE_SALE", "SELF_RETAIL", null,
                        "SELLER_SNAPSHOT", "OFFLINE_SALE_PAID_AMOUNT", "OFFLINE_SALE_AUDITED", false, true, true, false),
                new TemplateDefinition("TEAM_MANAGEMENT", "团队管理奖励", "团队在统计周期内达到有效终端销售业绩后，奖励团队负责人。",
                        Arrays.asList("maker", "partner", "county_agent"), "PERFORMANCE_PERIOD", "TEAM_MANAGEMENT", "TEAM_VALID_RETAIL",
                        "PERFORMANCE_OWNER", "VALID_PERFORMANCE_AMOUNT", "PERIOD_CLOSED", false, false, true, true),
                new TemplateDefinition("REGION_MANAGEMENT", "区域管理奖励", "区域内有效终端零售完成后，奖励订单归属快照中的区县代理。",
                        Arrays.asList("county_agent"), "RETAIL_ORDER", "REGION_MANAGEMENT", null,
                        "COUNTY_AGENT_SNAPSHOT", "ITEM_PAID_AMOUNT", "ORDER_COMPLETED", true, false, false, false),
                new TemplateDefinition("PLATFORM_ORDER_SUBSIDY", "平台订货补贴", "平台订货完成入库后，按已发布规则给予区县代理额外补贴。",
                        Arrays.asList("county_agent"), "PLATFORM_ORDER", "PLATFORM_ORDER_SUBSIDY", null,
                        "PURCHASER_SNAPSHOT", "PLATFORM_ORDER_AMOUNT", "PLATFORM_ORDER_RECEIVED", false, true, true, false),
                new TemplateDefinition("TRANSFER_PLATFORM_SUBSIDY", "调拨平台补贴", "库存调拨完成后，按已发布规则给予平台另行支付的补贴；线下价差不重复计入。",
                        Arrays.asList("county_agent"), "STOCK_TRANSFER", "TRANSFER_PLATFORM_SUBSIDY", null,
                        "TRANSFER_SENDER_SNAPSHOT", "TRANSFER_AMOUNT", "STOCK_TRANSFER_RECEIVED", false, true, true, false),
                new TemplateDefinition("TIER_REWARD", "阶梯奖励", "统计周期有效终端销售达到门槛后，按受控阶梯规则奖励。",
                        Arrays.asList("maker", "partner", "county_agent"), "PERFORMANCE_PERIOD", "TIER_REWARD", "VALID_RETAIL",
                        "PERFORMANCE_OWNER", "VALID_PERFORMANCE_AMOUNT", "PERIOD_CLOSED", false, false, true, true)
        );
    }

    private static class TemplateDefinition {
        private final String code, name, description, sourceType, rewardType, performanceType, beneficiaryType, baseType, triggerTiming;
        private final List<String> roles;
        private final boolean requiresRegisteredCustomer, requiresVoucher, requiresAudit, periodic, tier;

        private TemplateDefinition(String code, String name, String description, List<String> roles,
                                   String sourceType, String rewardType, String performanceType,
                                   String beneficiaryType, String baseType, String triggerTiming,
                                   boolean requiresRegisteredCustomer, boolean requiresVoucher,
                                   boolean requiresAudit, boolean periodic) {
            this.code = code; this.name = name; this.description = description; this.roles = roles;
            this.sourceType = sourceType; this.rewardType = rewardType; this.performanceType = performanceType;
            this.beneficiaryType = beneficiaryType; this.baseType = baseType; this.triggerTiming = triggerTiming;
            this.requiresRegisteredCustomer = requiresRegisteredCustomer; this.requiresVoucher = requiresVoucher;
            this.requiresAudit = requiresAudit; this.periodic = periodic; this.tier = "TIER_REWARD".equals(code);
        }
    }
}
