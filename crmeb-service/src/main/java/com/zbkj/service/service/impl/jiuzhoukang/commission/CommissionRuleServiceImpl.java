package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRuleItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleItemSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRulePublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleSaveRequest;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleItemDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommissionRuleServiceImpl extends ServiceImpl<JkCommissionRuleDao, JkCommissionRule> implements CommissionRuleService {
    @Autowired private JkCommissionRuleItemDao itemDao;
    @Autowired private JkBusinessRoleService businessRoleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkCommissionRule saveRule(JkCommissionRuleSaveRequest request) {
        JkCommissionRule rule = request.getId() == null ? new JkCommissionRule() : getById(request.getId());
        if (rule == null) throw new IllegalArgumentException("佣金规则不存在");
        if ("PUBLISHED".equals(rule.getPublishStatus())) throw new IllegalArgumentException("已发布规则不可直接编辑，请复制为新版本");
        BeanUtils.copyProperties(request, rule);
        validateDraft(rule);
        Date now = new Date();
        if (rule.getId() == null) {
            rule.setRuleNo("CR" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            rule.setRuleVersion(rule.getRuleVersion() == null ? 1 : rule.getRuleVersion());
            rule.setVersionNo(rule.getVersionNo() == null ? rule.getRuleVersion() : rule.getVersionNo());
            rule.setStatus(false).setPublishStatus("DRAFT").setIsDeleted(false).setVersion(0)
                    .setSettleDelayDays(defaultInt(rule.getSettleDelayDays(), defaultInt(rule.getFreezeDays(), 0)))
                    .setPriority(defaultInt(rule.getPriority(), 0))
                    .setRequiresRegisteredCustomer(Boolean.TRUE.equals(rule.getRequiresRegisteredCustomer()))
                    .setRequiresVoucher(Boolean.TRUE.equals(rule.getRequiresVoucher()))
                    .setRequiresAudit(Boolean.TRUE.equals(rule.getRequiresAudit()))
                    .setCreateTime(now).setUpdateTime(now);
            save(rule);
        } else {
            rule.setStatus(false).setPublishStatus("DRAFT").setPublishedAt(null).setPublishedBy(null).setUpdateTime(now);
            updateById(rule);
        }
        return enrich(getById(rule.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkCommissionRule publish(JkCommissionRulePublishRequest request, Long operatorId) {
        JkCommissionRule rule = getById(request.getRuleId());
        if (rule == null || Boolean.TRUE.equals(rule.getIsDeleted())) throw new IllegalArgumentException("佣金规则不存在");
        validatePublish(rule, request);
        Date now = new Date();
        rule.setEffectiveStartTime(request.getEffectiveStartTime()).setEffectiveEndTime(request.getEffectiveEndTime())
                .setEffectiveTime(request.getEffectiveStartTime()).setExpireTime(request.getEffectiveEndTime())
                .setStatus(true).setPublishStatus("PUBLISHED").setPublishedBy(operatorId).setPublishedAt(now)
                .setRemark(StrUtil.blankToDefault(request.getRemark(), rule.getRemark())).setUpdateTime(now);
        updateById(rule);
        return enrich(getById(rule.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkCommissionRule disable(Long id, String reason, Long operatorId) {
        JkCommissionRule rule = getById(id);
        if (rule == null || Boolean.TRUE.equals(rule.getIsDeleted())) throw new IllegalArgumentException("佣金规则不存在");
        Date now = new Date();
        rule.setStatus(false).setPublishStatus("DISABLED").setEffectiveEndTime(now).setExpireTime(now)
                .setRemark(StrUtil.blankToDefault(reason, "人工停用") + "；operator=" + operatorId).setUpdateTime(now);
        updateById(rule);
        return enrich(getById(id));
    }

    @Override
    public JkCommissionRuleItem saveItem(JkCommissionRuleItemSaveRequest request) {
        JkCommissionRule rule = request.getRuleId() == null ? null : getById(request.getRuleId());
        if (rule == null) throw new IllegalArgumentException("佣金规则不存在");
        if ("PUBLISHED".equals(rule.getPublishStatus())) throw new IllegalArgumentException("已发布规则明细不可直接修改");
        if (!Arrays.asList("PERCENT", "FIXED", "FIXED_PER_ORDER", "FIXED_PER_ITEM", "FIXED_PER_QUANTITY").contains(request.getCalculationType())) {
            throw new IllegalArgumentException("佣金计算类型非法");
        }
        if ("PERCENT".equals(request.getCalculationType()) && (request.getCommissionRate() == null || request.getCommissionRate().signum() < 0)) throw new IllegalArgumentException("佣金比例非法");
        if (!"PERCENT".equals(request.getCalculationType()) && request.getFixedAmount() != null && request.getFixedAmount().signum() < 0) throw new IllegalArgumentException("固定佣金非法");
        JkCommissionRuleItem item = request.getId() == null ? new JkCommissionRuleItem() : itemDao.selectById(request.getId());
        if (item == null) item = new JkCommissionRuleItem();
        BeanUtils.copyProperties(request, item);
        Date now = new Date();
        if (item.getId() == null) {
            item.setItemNo("CI" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                    .setPriority(item.getPriority() == null ? 0 : item.getPriority()).setStatus(item.getStatus() == null ? true : item.getStatus())
                    .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
            itemDao.insert(item);
        } else { item.setUpdateTime(now); itemDao.updateById(item); }
        JkCommissionRuleItem saved = itemDao.selectById(item.getId()); enrichRuleItemDisplays(Collections.singletonList(saved)); return saved;
    }

    @Override public List<JkCommissionRuleItem> listItems(Long ruleId) {
        List<JkCommissionRuleItem> items = itemDao.selectList(new LambdaQueryWrapper<JkCommissionRuleItem>()
                .eq(JkCommissionRuleItem::getRuleId, ruleId).eq(JkCommissionRuleItem::getIsDeleted, false)
                .orderByDesc(JkCommissionRuleItem::getPriority));
        enrichRuleItemDisplays(items); return items;
    }

    @Override public boolean updateItemStatus(Long id, boolean status) {
        JkCommissionRuleItem item = itemDao.selectById(id); if (item == null) return false;
        JkCommissionRule rule = getById(item.getRuleId());
        if (rule != null && "PUBLISHED".equals(rule.getPublishStatus())) throw new IllegalArgumentException("已发布规则明细不可直接启停");
        item.setStatus(status).setUpdateTime(new Date()); return itemDao.updateById(item) > 0;
    }

    @Override public boolean updateStatus(Long id, boolean status) {
        JkCommissionRule rule = getById(id); if (rule == null) return false;
        if (status && !"PUBLISHED".equals(rule.getPublishStatus())) throw new IllegalArgumentException("草稿规则必须通过审核发布接口启用");
        if (!status) { disable(id, "旧接口停用", null); return true; }
        rule.setStatus(true).setUpdateTime(new Date()); return updateById(rule);
    }

    @Override
    public List<JkCommissionRule> listActiveRules(String sourceType, String receiverRoleCode) {
        Date now = new Date();
        LambdaQueryWrapper<JkCommissionRule> query = baseQuery(sourceType, receiverRoleCode, true)
                .eq(JkCommissionRule::getPublishStatus, "PUBLISHED")
                .le(JkCommissionRule::getEffectiveStartTime, now)
                .and(q -> q.isNull(JkCommissionRule::getEffectiveEndTime).or().gt(JkCommissionRule::getEffectiveEndTime, now));
        List<JkCommissionRule> rules = list(query); enrichRuleDisplays(rules); return rules;
    }

    @Override
    public List<JkCommissionRule> listRules(String sourceType, String receiverRoleCode, Boolean status) {
        List<JkCommissionRule> rules = list(baseQuery(sourceType, receiverRoleCode, status)); enrichRuleDisplays(rules); return rules;
    }

    private LambdaQueryWrapper<JkCommissionRule> baseQuery(String sourceType, String receiverRoleCode, Boolean status) {
        LambdaQueryWrapper<JkCommissionRule> q = new LambdaQueryWrapper<JkCommissionRule>().eq(JkCommissionRule::getIsDeleted, false);
        if (StrUtil.isNotBlank(sourceType)) q.eq(JkCommissionRule::getSourceType, sourceType);
        if (StrUtil.isNotBlank(receiverRoleCode)) q.eq(JkCommissionRule::getReceiverRoleCode, receiverRoleCode);
        if (status != null) q.eq(JkCommissionRule::getStatus, status);
        return q.orderByDesc(JkCommissionRule::getPriority).orderByDesc(JkCommissionRule::getVersionNo).orderByDesc(JkCommissionRule::getId);
    }

    private void validateDraft(JkCommissionRule rule) {
        if (StrUtil.isBlank(rule.getRuleName())) throw new IllegalArgumentException("规则名称不能为空");
        if (StrUtil.isBlank(rule.getSourceType())) throw new IllegalArgumentException("来源类型不能为空");
        if (StrUtil.isNotBlank(rule.getBeneficiaryType()) && !Arrays.asList("DIRECT_PARENT_SNAPSHOT", "COUNTY_AGENT_SNAPSHOT", "SELLER_SNAPSHOT", "PURCHASER_SNAPSHOT", "TRANSFER_SENDER_SNAPSHOT", "TRANSFER_RECEIVER_SNAPSHOT", "PERFORMANCE_OWNER", "SPECIFIED_ROLE", "SPECIFIED_USER").contains(rule.getBeneficiaryType())) throw new IllegalArgumentException("受益人来源非法");
        if (StrUtil.isNotBlank(rule.getBaseType()) && !Arrays.asList("ITEM_PAID_AMOUNT", "ORDER_PAID_AMOUNT_EXCLUDE_FREIGHT", "OFFLINE_SALE_PAID_AMOUNT", "VALID_PERFORMANCE_AMOUNT", "TRANSFER_AMOUNT", "PLATFORM_ORDER_AMOUNT", "VALID_QUANTITY", "REAL_GROSS_PROFIT").contains(rule.getBaseType())) throw new IllegalArgumentException("计算基数非法");
        if (StrUtil.isNotBlank(rule.getCalculationType()) && !Arrays.asList("PERCENT", "FIXED_PER_ORDER", "FIXED_PER_ITEM", "FIXED_PER_QUANTITY", "TIER_PERCENT").contains(rule.getCalculationType())) throw new IllegalArgumentException("计算方式非法");
        if (StrUtil.isNotBlank(rule.getStackPolicy()) && !Arrays.asList("MAX_ONE", "ALLOW_STACK", "HIGHEST_AMOUNT", "HIGHEST_PRIORITY").contains(rule.getStackPolicy())) throw new IllegalArgumentException("叠加策略非法");
        nonNegative(rule.getRate(), "比例"); nonNegative(rule.getFixedAmount(), "固定金额"); nonNegative(rule.getUnitAmount(), "单位金额");
        nonNegative(rule.getPerOrderCap(), "单笔封顶"); nonNegative(rule.getPerUserPeriodCap(), "周期封顶"); nonNegative(rule.getTotalBudget(), "总预算");
    }

    private void validatePublish(JkCommissionRule rule, JkCommissionRulePublishRequest request) {
        validateDraft(rule);
        if (StrUtil.isBlank(rule.getRuleCode()) || StrUtil.isBlank(rule.getRewardType()) || StrUtil.isBlank(rule.getReceiverRoleCode())
                || StrUtil.isBlank(rule.getBeneficiaryType()) || StrUtil.isBlank(rule.getBaseType()) || StrUtil.isBlank(rule.getCalculationType())) {
            throw new IllegalArgumentException("发布前必须补齐规则编码、奖励类型、受益角色、受益人来源、计算基数和计算方式");
        }
        if ("PERCENT".equals(rule.getCalculationType()) && rule.getRate() == null) throw new IllegalArgumentException("比例规则必须填写比例");
        if ("FIXED_PER_ORDER".equals(rule.getCalculationType()) && rule.getFixedAmount() == null) throw new IllegalArgumentException("固定规则必须填写金额");
        if ("FIXED_PER_QUANTITY".equals(rule.getCalculationType()) && rule.getUnitAmount() == null) throw new IllegalArgumentException("按数量规则必须填写单位金额");
        if (request.getEffectiveEndTime() != null && !request.getEffectiveEndTime().after(request.getEffectiveStartTime())) throw new IllegalArgumentException("失效时间必须晚于生效时间");
    }

    private JkCommissionRule enrich(JkCommissionRule rule) { if (rule != null) enrichRuleDisplays(Collections.singletonList(rule)); return rule; }
    void enrichRuleDisplays(List<JkCommissionRule> rules) {
        if (rules == null || rules.isEmpty()) return;
        Map<String, String> roleNameMap = roleNameMap();
        for (JkCommissionRule rule : rules) {
            rule.setSourceTypeText(labelSourceType(rule.getSourceType()));
            rule.setReceiverRoleName(resolveRoleName(roleNameMap, rule.getReceiverRoleCode()));
            rule.setStatusText(Boolean.TRUE.equals(rule.getStatus()) ? "启用" : "禁用");
            rule.setStatusTag(Boolean.TRUE.equals(rule.getStatus()) ? "success" : "info");
            rule.setPublishStatusText(labelPublishStatus(rule.getPublishStatus()));
            rule.setCapabilityStatusText("PUBLISHED".equals(rule.getPublishStatus()) && Boolean.TRUE.equals(rule.getStatus()) ? "规则已发布" : "能力已配置 / 当前未启用");
        }
    }

    void enrichRuleItemDisplays(List<JkCommissionRuleItem> items) {
        if (items == null || items.isEmpty()) return;
        Map<String, String> roleNameMap = roleNameMap();
        for (JkCommissionRuleItem item : items) {
            item.setReceiverRoleName(resolveRoleName(roleNameMap, item.getReceiverRoleCode()));
            item.setCalculationTypeText(labelCalculationType(item.getCalculationType()));
            item.setStatusText(Boolean.TRUE.equals(item.getStatus()) ? "启用" : "禁用");
            item.setStatusTag(Boolean.TRUE.equals(item.getStatus()) ? "success" : "info");
        }
    }

    private Map<String, String> roleNameMap() {
        List<JkBusinessRole> roles = businessRoleService.getEnabledRoleList();
        return roles == null || roles.isEmpty() ? Collections.<String, String>emptyMap()
                : roles.stream().collect(Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
    }
    private String resolveRoleName(Map<String, String> map, String code) { return StrUtil.isBlank(code) ? "--" : map.getOrDefault(code, code); }
    private String labelSourceType(String value) { if ("RETAIL_ORDER".equals(value)) return "线上零售"; if ("OFFLINE_SALE".equals(value)) return "线下销售"; if ("PLATFORM_ORDER".equals(value)) return "平台订货"; if ("STOCK_TRANSFER".equals(value)) return "库存调拨"; if ("PERFORMANCE_PERIOD".equals(value)) return "周期业绩"; return StrUtil.blankToDefault(value, "--"); }
    private String labelCalculationType(String value) { if ("PERCENT".equals(value)) return "比例"; if (value != null && value.startsWith("FIXED")) return "固定金额"; if ("TIER_PERCENT".equals(value)) return "阶梯比例"; return StrUtil.blankToDefault(value, "--"); }
    private String labelPublishStatus(String value) { if ("DRAFT".equals(value)) return "草稿"; if ("PUBLISHED".equals(value)) return "已发布"; if ("DISABLED".equals(value)) return "已停用"; if ("PENDING_CONFIRMATION".equals(value)) return "待确认迁移"; return StrUtil.blankToDefault(value, "未发布"); }
    private void nonNegative(BigDecimal value, String name) { if (value != null && value.signum() < 0) throw new IllegalArgumentException(name + "不能小于0"); }
    private int defaultInt(Integer value, int fallback) { return value == null ? fallback : value; }
}
