package com.zbkj.service.service.impl.jiuzhoukang.business;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRulePlan;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRulePlanPublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRulePlanSaveRequest;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRulePlanDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleDao;
import com.zbkj.service.service.jiuzhoukang.business.JkBusinessRulePlanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JkBusinessRulePlanServiceImpl implements JkBusinessRulePlanService {
    @Autowired private JkBusinessRulePlanDao planDao;
    @Autowired private JkCommissionRuleDao commissionRuleDao;

    @Override
    public List<JkBusinessRulePlan> list(String planCode, String publishStatus) {
        LambdaQueryWrapper<JkBusinessRulePlan> query = new LambdaQueryWrapper<JkBusinessRulePlan>()
                .eq(JkBusinessRulePlan::getIsDeleted, false)
                .orderByAsc(JkBusinessRulePlan::getPlanCode)
                .orderByDesc(JkBusinessRulePlan::getVersionNo);
        if (StrUtil.isNotBlank(planCode)) query.eq(JkBusinessRulePlan::getPlanCode, planCode.trim());
        if (StrUtil.isNotBlank(publishStatus)) query.eq(JkBusinessRulePlan::getPublishStatus, publishStatus.trim());
        return planDao.selectList(query);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        JkBusinessRulePlan plan = require(id);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("plan", plan);
        result.put("rules", commissionRuleDao.selectList(new LambdaQueryWrapper<JkCommissionRule>()
                .eq(JkCommissionRule::getPlanId, id)
                .eq(JkCommissionRule::getIsDeleted, false)
                .orderByDesc(JkCommissionRule::getPriority)
                .orderByDesc(JkCommissionRule::getId)));
        result.put("versions", planDao.selectList(new LambdaQueryWrapper<JkBusinessRulePlan>()
                .eq(JkBusinessRulePlan::getPlanCode, plan.getPlanCode())
                .eq(JkBusinessRulePlan::getIsDeleted, false)
                .orderByDesc(JkBusinessRulePlan::getVersionNo)));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkBusinessRulePlan saveDraft(JkBusinessRulePlanSaveRequest request) {
        JkBusinessRulePlan plan;
        Date now = new Date();
        if (request.getId() != null) {
            plan = require(request.getId());
            if (!"DRAFT".equals(plan.getPublishStatus())) {
                throw new IllegalArgumentException("已发布或已停用方案不可直接编辑，请复制为新版本");
            }
            BeanUtils.copyProperties(request, plan, "id", "versionNo", "publishStatus", "status");
            plan.setApplicableRoleCodes(JSONUtil.toJsonStr(safeList(request.getApplicableRoleCodes())))
                    .setApplicableRegionCodes(JSONUtil.toJsonStr(safeList(request.getApplicableRegionCodes())))
                    .setPriority(request.getPriority() == null ? 0 : request.getPriority())
                    .setUpdateTime(now);
            planDao.updateById(plan);
            return planDao.selectById(plan.getId());
        }

        String code = request.getPlanCode().trim();
        Integer maxVersion = planDao.selectList(new LambdaQueryWrapper<JkBusinessRulePlan>()
                .eq(JkBusinessRulePlan::getPlanCode, code)
                .eq(JkBusinessRulePlan::getIsDeleted, false))
                .stream().map(JkBusinessRulePlan::getVersionNo).filter(v -> v != null).max(Integer::compareTo).orElse(0);
        plan = new JkBusinessRulePlan()
                .setPlanCode(code).setPlanName(request.getPlanName().trim()).setVersionNo(maxVersion + 1)
                .setStatus("DRAFT").setPublishStatus("DRAFT")
                .setApplicableRoleCodes(JSONUtil.toJsonStr(safeList(request.getApplicableRoleCodes())))
                .setApplicableRegionCodes(JSONUtil.toJsonStr(safeList(request.getApplicableRegionCodes())))
                .setPriority(request.getPriority() == null ? 0 : request.getPriority())
                .setChangeSummary(request.getChangeSummary()).setRemark(request.getRemark())
                .setIsDeleted(false).setVersion(0).setCreateTime(now).setUpdateTime(now);
        planDao.insert(plan);
        return planDao.selectById(plan.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkBusinessRulePlan copyVersion(Long id, String changeSummary) {
        JkBusinessRulePlan source = require(id);
        JkBusinessRulePlanSaveRequest request = new JkBusinessRulePlanSaveRequest();
        request.setPlanCode(source.getPlanCode());
        request.setPlanName(source.getPlanName());
        request.setApplicableRoleCodes(parseStringList(source.getApplicableRoleCodes()));
        request.setApplicableRegionCodes(parseStringList(source.getApplicableRegionCodes()));
        request.setPriority(source.getPriority());
        request.setChangeSummary(StrUtil.blankToDefault(changeSummary, "从 V" + source.getVersionNo() + " 复制新版本"));
        request.setRemark(source.getRemark());
        return saveDraft(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkBusinessRulePlan publish(JkBusinessRulePlanPublishRequest request, Long operatorId) {
        JkBusinessRulePlan plan = require(request.getPlanId());
        if (!"DRAFT".equals(plan.getPublishStatus())) throw new IllegalArgumentException("只有草稿方案可以发布");
        if (request.getEffectiveEndTime() != null && !request.getEffectiveEndTime().after(request.getEffectiveStartTime())) {
            throw new IllegalArgumentException("失效时间必须晚于生效时间");
        }
        List<JkBusinessRulePlan> published = planDao.selectList(new LambdaQueryWrapper<JkBusinessRulePlan>()
                .eq(JkBusinessRulePlan::getPlanCode, plan.getPlanCode())
                .eq(JkBusinessRulePlan::getPublishStatus, "PUBLISHED")
                .eq(JkBusinessRulePlan::getIsDeleted, false));
        for (JkBusinessRulePlan old : published) {
            if (overlaps(old.getEffectiveStartTime(), old.getEffectiveEndTime(), request.getEffectiveStartTime(), request.getEffectiveEndTime())) {
                throw new IllegalArgumentException("生效窗口与已发布版本 V" + old.getVersionNo() + " 重叠，请先停用旧版本或调整窗口");
            }
        }
        long configuredRules = commissionRuleDao.selectCount(new LambdaQueryWrapper<JkCommissionRule>()
                .eq(JkCommissionRule::getPlanId, plan.getId())
                .eq(JkCommissionRule::getIsDeleted, false));
        if (configuredRules == 0) {
            throw new IllegalArgumentException("商业方案尚未配置任何业务规则；空方案不能发布，也不会显示为 0 元方案");
        }
        Date now = new Date();
        plan.setEffectiveStartTime(request.getEffectiveStartTime()).setEffectiveEndTime(request.getEffectiveEndTime())
                .setStatus("PUBLISHED").setPublishStatus("PUBLISHED")
                .setPublishedBy(operatorId).setPublishedAt(now)
                .setChangeSummary(StrUtil.blankToDefault(request.getChangeSummary(), plan.getChangeSummary()))
                .setUpdateTime(now);
        planDao.updateById(plan);
        return planDao.selectById(plan.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkBusinessRulePlan disable(Long id, String reason, Long operatorId) {
        JkBusinessRulePlan plan = require(id);
        if (!"PUBLISHED".equals(plan.getPublishStatus())) throw new IllegalArgumentException("只有已发布方案可以停用");
        Date now = new Date();
        plan.setStatus("DISABLED").setPublishStatus("DISABLED")
                .setDisabledBy(operatorId).setDisabledAt(now)
                .setRemark(StrUtil.blankToDefault(reason, "人工停用") + (StrUtil.isBlank(plan.getRemark()) ? "" : "；" + plan.getRemark()))
                .setUpdateTime(now);
        planDao.updateById(plan);
        return planDao.selectById(id);
    }

    @Override
    public List<Map<String, Object>> roleCards() {
        List<Map<String, Object>> cards = new ArrayList<Map<String, Object>>();
        cards.add(roleCard("maker", "创客方案", Arrays.asList("DIRECT_REFERRAL", "SELF_RETAIL", "TEAM_MANAGEMENT", "TIER_REWARD")));
        cards.add(roleCard("partner", "合伙人方案", Arrays.asList("DIRECT_REFERRAL", "SELF_RETAIL", "TEAM_MANAGEMENT", "TIER_REWARD")));
        cards.add(roleCard("county_agent", "区县代理方案", Arrays.asList("DIRECT_REFERRAL", "REGION_MANAGEMENT", "PLATFORM_ORDER_SUBSIDY", "TRANSFER_PLATFORM_SUBSIDY", "TIER_REWARD")));
        return cards;
    }

    private Map<String, Object> roleCard(String roleCode, String roleName, List<String> templates) {
        Map<String, Object> card = new LinkedHashMap<String, Object>();
        card.put("roleCode", roleCode);
        card.put("roleName", roleName);
        List<Map<String, Object>> rewards = new ArrayList<Map<String, Object>>();
        for (String template : templates) {
            Map<String, Object> reward = new LinkedHashMap<String, Object>();
            reward.put("templateCode", template);
            reward.put("templateName", templateName(template));
            JkCommissionRule active = commissionRuleDao.selectOne(new LambdaQueryWrapper<JkCommissionRule>()
                    .eq(JkCommissionRule::getReceiverRoleCode, roleCode)
                    .eq(JkCommissionRule::getTemplateCode, template)
                    .eq(JkCommissionRule::getPublishStatus, "PUBLISHED")
                    .eq(JkCommissionRule::getStatus, true)
                    .eq(JkCommissionRule::getIsDeleted, false)
                    .orderByDesc(JkCommissionRule::getVersionNo).orderByDesc(JkCommissionRule::getId).last("limit 1"));
            reward.put("status", active == null ? "CLOSED" : "PUBLISHED");
            reward.put("statusText", active == null ? "关闭 / 未发布" : "已发布");
            reward.put("ruleId", active == null ? null : active.getId());
            rewards.add(reward);
        }
        card.put("rewards", rewards);
        return card;
    }

    private JkBusinessRulePlan require(Long id) {
        JkBusinessRulePlan plan = id == null ? null : planDao.selectById(id);
        if (plan == null || Boolean.TRUE.equals(plan.getIsDeleted())) throw new IllegalArgumentException("商业方案不存在");
        return plan;
    }

    private boolean overlaps(Date aStart, Date aEnd, Date bStart, Date bEnd) {
        if (aStart == null || bStart == null) return true;
        boolean aBeforeBEnd = bEnd == null || aStart.before(bEnd);
        boolean bBeforeAEnd = aEnd == null || bStart.before(aEnd);
        return aBeforeBEnd && bBeforeAEnd;
    }

    private List<String> safeList(List<String> values) { return values == null ? new ArrayList<String>() : values; }
    private List<String> parseStringList(String json) {
        if (StrUtil.isBlank(json)) return new ArrayList<String>();
        try { return JSONUtil.toList(json, String.class); } catch (Exception ignored) { return new ArrayList<String>(); }
    }
    private String templateName(String code) {
        if ("DIRECT_REFERRAL".equals(code)) return "直属推荐奖励";
        if ("SELF_RETAIL".equals(code)) return "自营销售奖励";
        if ("TEAM_MANAGEMENT".equals(code)) return "团队管理奖励";
        if ("REGION_MANAGEMENT".equals(code)) return "区域管理奖励";
        if ("PLATFORM_ORDER_SUBSIDY".equals(code)) return "平台订货补贴";
        if ("TRANSFER_PLATFORM_SUBSIDY".equals(code)) return "调拨平台补贴";
        if ("TIER_REWARD".equals(code)) return "阶梯奖励";
        return code;
    }
}
