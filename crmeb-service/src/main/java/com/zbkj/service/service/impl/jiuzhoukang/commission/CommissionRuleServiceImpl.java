package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRuleItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleItemSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleSaveRequest;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleItemDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 佣金规则维护服务。
 * V3.1 起新增规则必须先保存为关闭草稿；已发布版本再次编辑时创建新版本，禁止原地改写历史规则。
 */
@Service
public class CommissionRuleServiceImpl extends ServiceImpl<JkCommissionRuleDao, JkCommissionRule> implements CommissionRuleService {
    @Autowired private JkCommissionRuleItemDao itemDao;
    @Autowired private JkBusinessRoleService businessRoleService;

    @Override
    public JkCommissionRule saveRule(JkCommissionRuleSaveRequest request) {
        JkCommissionRule source = request.getId() == null ? null : getById(request.getId());
        boolean forkPublished = source != null && "PUBLISHED".equals(source.getPublishStatus());
        JkCommissionRule rule = source == null || forkPublished ? new JkCommissionRule() : source;
        BeanUtils.copyProperties(request, rule, "id", "effectiveTime", "expireTime");
        Date now = new Date();
        if (rule.getId() == null) {
            rule.setRuleNo(StrUtil.isNotBlank(request.getRuleCode())
                            ? "V31-" + request.getRuleCode() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                            : "CR" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                    .setRuleVersion(forkPublished ? safeVersion(source.getRuleVersion()) + 1 : safeVersion(request.getRuleVersion()))
                    .setStatus(false).setPublishStatus("DRAFT")
                    .setEffectiveTime(null).setExpireTime(null).setPublishedBy(null).setPublishedTime(null)
                    .setStackPolicy(StrUtil.blankToDefault(rule.getStackPolicy(), "MAX_ONE"))
                    .setPriority(rule.getPriority() == null ? 0 : rule.getPriority())
                    .setSettleDelayDays(rule.getSettleDelayDays() == null ? safeDays(rule.getFreezeDays()) : rule.getSettleDelayDays())
                    .setRequiresRegisteredCustomer(Boolean.TRUE.equals(rule.getRequiresRegisteredCustomer()))
                    .setRequiresVoucher(Boolean.TRUE.equals(rule.getRequiresVoucher()))
                    .setRequiresAudit(Boolean.TRUE.equals(rule.getRequiresAudit()))
                    .setIsDeleted(false).setVersion(0).setCreateTime(now).setUpdateTime(now);
            save(rule);
        } else {
            // 草稿阶段不允许提前写入生效时间，发布接口统一设置。
            rule.setStatus(false).setPublishStatus("DRAFT").setEffectiveTime(null).setExpireTime(null).setUpdateTime(now);
            updateById(rule);
        }
        JkCommissionRule saved = getById(rule.getId());
        enrichRuleDisplays(Collections.singletonList(saved));
        return saved;
    }

    @Override
    public JkCommissionRuleItem saveItem(JkCommissionRuleItemSaveRequest request) {
        JkCommissionRule parent = request.getRuleId() == null ? null : getById(request.getRuleId());
        if (parent == null) throw new IllegalArgumentException("佣金规则不存在");
        if ("PUBLISHED".equals(parent.getPublishStatus())) throw new IllegalArgumentException("已发布规则不能原地修改明细，请创建新版本");
        if (!"PERCENT".equals(request.getCalculationType()) && !"FIXED".equals(request.getCalculationType())) {
            throw new IllegalArgumentException("佣金计算类型非法");
        }
        if ("PERCENT".equals(request.getCalculationType())
                && (request.getCommissionRate() == null || request.getCommissionRate().signum() < 0)) {
            throw new IllegalArgumentException("佣金比例非法");
        }
        if ("FIXED".equals(request.getCalculationType())
                && (request.getFixedAmount() == null || request.getFixedAmount().signum() < 0)) {
            throw new IllegalArgumentException("固定佣金非法");
        }
        JkCommissionRuleItem item = request.getId() == null ? new JkCommissionRuleItem() : itemDao.selectById(request.getId());
        if (item == null) item = new JkCommissionRuleItem();
        BeanUtils.copyProperties(request, item);
        Date now = new Date();
        if (item.getId() == null) {
            item.setItemNo("CI" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                    .setPriority(item.getPriority() == null ? 0 : item.getPriority())
                    .setStatus(item.getStatus() == null ? true : item.getStatus())
                    .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
            itemDao.insert(item);
        } else {
            item.setUpdateTime(now);
            itemDao.updateById(item);
        }
        JkCommissionRuleItem saved = itemDao.selectById(item.getId());
        enrichRuleItemDisplays(Collections.singletonList(saved));
        return saved;
    }

    @Override
    public List<JkCommissionRuleItem> listItems(Long ruleId) {
        List<JkCommissionRuleItem> items = itemDao.selectList(new LambdaQueryWrapper<JkCommissionRuleItem>()
                .eq(JkCommissionRuleItem::getRuleId, ruleId).eq(JkCommissionRuleItem::getIsDeleted, false)
                .orderByDesc(JkCommissionRuleItem::getPriority));
        enrichRuleItemDisplays(items);
        return items;
    }

    @Override
    public boolean updateItemStatus(Long id, boolean status) {
        JkCommissionRuleItem item = itemDao.selectById(id);
        if (item == null) return false;
        JkCommissionRule parent = getById(item.getRuleId());
        if (parent != null && "PUBLISHED".equals(parent.getPublishStatus())) {
            throw new IllegalArgumentException("已发布规则不能原地修改明细状态");
        }
        item.setStatus(status).setUpdateTime(new Date());
        return itemDao.updateById(item) > 0;
    }

    @Override
    public boolean updateStatus(Long id, boolean status) {
        JkCommissionRule rule = getById(id);
        if (rule == null) return false;
        if (status && StrUtil.isNotBlank(rule.getPublishStatus()) && !"PUBLISHED".equals(rule.getPublishStatus())) {
            throw new IllegalArgumentException("V3.1 规则必须完成试算并通过发布接口启用");
        }
        rule.setStatus(status).setUpdateTime(new Date());
        if (!status && "PUBLISHED".equals(rule.getPublishStatus())) rule.setPublishStatus("DISABLED").setExpireTime(new Date());
        return updateById(rule);
    }

    @Override
    public List<JkCommissionRule> listActiveRules(String sourceType, String receiverRoleCode) {
        LambdaQueryWrapper<JkCommissionRule> query = baseQuery(sourceType, receiverRoleCode)
                .eq(JkCommissionRule::getStatus, true)
                .and(q -> q.isNull(JkCommissionRule::getPublishStatus).or().eq(JkCommissionRule::getPublishStatus, "PUBLISHED"))
                .and(q -> q.isNull(JkCommissionRule::getEffectiveTime).or().le(JkCommissionRule::getEffectiveTime, new Date()))
                .and(q -> q.isNull(JkCommissionRule::getExpireTime).or().gt(JkCommissionRule::getExpireTime, new Date()));
        List<JkCommissionRule> rules = list(query);
        enrichRuleDisplays(rules);
        return rules;
    }

    @Override
    public List<JkCommissionRule> listRules(String sourceType, String receiverRoleCode, Boolean status) {
        LambdaQueryWrapper<JkCommissionRule> query = baseQuery(sourceType, receiverRoleCode);
        if (status != null) query.eq(JkCommissionRule::getStatus, status);
        List<JkCommissionRule> rules = list(query);
        enrichRuleDisplays(rules);
        return rules;
    }

    private LambdaQueryWrapper<JkCommissionRule> baseQuery(String sourceType, String receiverRoleCode) {
        LambdaQueryWrapper<JkCommissionRule> query = new LambdaQueryWrapper<JkCommissionRule>()
                .eq(JkCommissionRule::getIsDeleted, false);
        if (StrUtil.isNotBlank(sourceType)) query.eq(JkCommissionRule::getSourceType, sourceType);
        if (StrUtil.isNotBlank(receiverRoleCode)) query.eq(JkCommissionRule::getReceiverRoleCode, receiverRoleCode);
        return query.orderByDesc(JkCommissionRule::getRuleVersion).orderByDesc(JkCommissionRule::getId);
    }

    void enrichRuleDisplays(List<JkCommissionRule> rules) {
        if (rules == null || rules.isEmpty()) return;
        Map<String, String> roleNameMap = roleNameMap();
        for (JkCommissionRule rule : rules) {
            rule.setSourceTypeText(labelSourceType(rule.getSourceType()));
            rule.setReceiverRoleName(resolveRoleName(roleNameMap, rule.getReceiverRoleCode()));
            String statusText = "PUBLISHED".equals(rule.getPublishStatus()) && Boolean.TRUE.equals(rule.getStatus())
                    ? "已发布" : "DRAFT".equals(rule.getPublishStatus()) ? "草稿（未启用）"
                    : "DISABLED".equals(rule.getPublishStatus()) ? "已停用" : Boolean.TRUE.equals(rule.getStatus()) ? "启用" : "禁用";
            rule.setStatusText(statusText);
            rule.setStatusTag(Boolean.TRUE.equals(rule.getStatus()) ? "success" : "info");
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
        if (roles == null || roles.isEmpty()) return Collections.emptyMap();
        return roles.stream().collect(Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
    }

    private String resolveRoleName(Map<String, String> roleNameMap, String roleCode) {
        return StrUtil.isBlank(roleCode) ? "--" : roleNameMap.getOrDefault(roleCode, roleCode);
    }

    private String labelSourceType(String sourceType) {
        if (StrUtil.isBlank(sourceType)) return "--";
        if ("RETAIL_ORDER".equals(sourceType)) return "线上零售";
        if ("RETAIL_SALE".equals(sourceType)) return "终端零售";
        if ("OFFLINE_SALE".equals(sourceType)) return "线下销售";
        if ("PLATFORM_ORDER".equals(sourceType)) return "平台订货";
        if ("STOCK_TRANSFER".equals(sourceType)) return "库存调拨";
        if ("PERFORMANCE_PERIOD".equals(sourceType)) return "周期业绩";
        return sourceType;
    }

    private String labelCalculationType(String calculationType) {
        if (StrUtil.isBlank(calculationType)) return "--";
        if ("PERCENT".equals(calculationType)) return "比例";
        if ("FIXED".equals(calculationType)) return "固定金额";
        return calculationType;
    }

    private int safeVersion(Integer value) { return value == null || value < 1 ? 1 : value; }
    private int safeDays(Integer value) { return value == null || value < 0 ? 0 : value; }
}
