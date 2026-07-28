package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.jiuzhoukang.JkRelationLimitRule;
import com.zbkj.common.model.jiuzhoukang.JkRelationQuotaReservation;
import com.zbkj.common.model.jiuzhoukang.JkRelationQuotaUsage;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRelationLimitRuleSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkRelationQuotaResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.dao.jiuzhoukang.JkRelationLimitRuleDao;
import com.zbkj.service.dao.jiuzhoukang.JkRelationQuotaReservationDao;
import com.zbkj.service.dao.jiuzhoukang.JkRelationQuotaUsageDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.jiuzhoukang.region.JkRelationQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 直属下级人数额度实现。
 *
 * <p>所有写操作先锁定 parent_user_id 对应的额度行，再以有效关系和有效预占重新计算，
 * 避免只依赖缓存或前端展示造成第 50/51 人并发穿透。</p>
 */
@Service
public class JkRelationQuotaServiceImpl implements JkRelationQuotaService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_WARNING_THRESHOLD = 80;
    private static final String STATUS_RESERVED = "RESERVED";
    private static final String STATUS_CONSUMED = "CONSUMED";

    @Autowired private JkRelationLimitRuleDao ruleDao;
    @Autowired private JkRelationQuotaUsageDao usageDao;
    @Autowired private JkRelationQuotaReservationDao reservationDao;
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private JkUserBusinessRoleDao userRoleDao;

    @Override
    public JkRelationQuotaResponse quota(Long parentUserId, Long childUserId) {
        if (parentUserId == null) {
            return new JkRelationQuotaResponse().setParentUserId(null).setMaxDirectChildren(Integer.MAX_VALUE)
                    .setUsedCount(0).setReservedCount(0).setRemainingCount(Integer.MAX_VALUE)
                    .setWarning(false).setFull(false).setOverflowPolicy("REJECT");
        }
        Identity parent = requireEligibleParent(parentUserId);
        Identity child = identity(childUserId);
        JkRelationLimitRule rule = matchRule(parent, child);
        int used = activeCount(parentUserId);
        int reserved = reservedCount(parentUserId);
        return response(parentUserId, parent, child, rule, used, reserved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void occupy(Long parentUserId, Long childUserId, Long operatorId) {
        if (parentUserId == null) return;
        Identity parent = requireEligibleParent(parentUserId);
        Identity child = identity(childUserId);
        JkRelationQuotaUsage usage = lockUsage(parentUserId);
        int used = activeCount(parentUserId);
        int reserved = reservedCount(parentUserId);
        JkRelationLimitRule rule = matchRule(parent, child);
        assertAvailable(rule, used, reserved, parentUserId);
        updateUsage(usage, used + 1, reserved, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserve(String requestNo, Long parentUserId, Long childUserId, Long operatorId) {
        if (parentUserId == null) return;
        if (StrUtil.isBlank(requestNo)) throw new IllegalArgumentException("换绑额度预占 requestNo 不能为空");
        JkRelationQuotaReservation existing = reservationDao.selectOne(new LambdaQueryWrapper<JkRelationQuotaReservation>()
                .eq(JkRelationQuotaReservation::getRequestNo, requestNo)
                .eq(JkRelationQuotaReservation::getIsDeleted, false).last("limit 1"));
        if (existing != null) {
            if (!Objects.equals(existing.getParentUserId(), parentUserId) || !Objects.equals(existing.getChildUserId(), childUserId)) {
                throw new IllegalArgumentException("requestNo 已被其他关系额度预占使用");
            }
            if (STATUS_RESERVED.equals(existing.getStatus()) || STATUS_CONSUMED.equals(existing.getStatus())) return;
            throw new IllegalArgumentException("该 requestNo 的额度预占已结束，请重新提交申请");
        }

        Identity parent = requireEligibleParent(parentUserId);
        Identity child = identity(childUserId);
        JkRelationQuotaUsage usage = lockUsage(parentUserId);
        int used = activeCount(parentUserId);
        int reserved = reservedCount(parentUserId);
        JkRelationLimitRule rule = matchRule(parent, child);
        assertAvailable(rule, used, reserved, parentUserId);

        Date now = new Date();
        JkRelationQuotaReservation entity = new JkRelationQuotaReservation()
                .setReservationNo("RQ" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setRequestNo(requestNo).setScene("RELATION_CHANGE")
                .setParentUserId(parentUserId).setChildUserId(childUserId)
                .setRuleId(rule.getId()).setStatus(STATUS_RESERVED)
                .setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId)
                .setCreateTime(now).setUpdateTime(now).setTenantId("000000");
        reservationDao.insert(entity);
        updateUsage(usage, used, reserved + 1, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(String requestNo, Long parentUserId, Long childUserId, Long operatorId) {
        if (parentUserId == null) return;
        JkRelationQuotaReservation reservation = StrUtil.isBlank(requestNo) ? null
                : reservationDao.selectOne(new LambdaQueryWrapper<JkRelationQuotaReservation>()
                .eq(JkRelationQuotaReservation::getRequestNo, requestNo)
                .eq(JkRelationQuotaReservation::getIsDeleted, false).last("limit 1"));
        if (reservation == null) {
            occupy(parentUserId, childUserId, operatorId);
            return;
        }
        if (!Objects.equals(reservation.getParentUserId(), parentUserId)
                || !Objects.equals(reservation.getChildUserId(), childUserId)) {
            throw new IllegalArgumentException("换绑额度预占与审核目标不一致");
        }
        if (STATUS_CONSUMED.equals(reservation.getStatus())) return;
        if (!STATUS_RESERVED.equals(reservation.getStatus())) throw new IllegalArgumentException("换绑额度预占已失效，请重新提交申请");

        JkRelationQuotaUsage usage = lockUsage(parentUserId);
        reservation.setStatus(STATUS_CONSUMED).setUpdateUserId(operatorId).setUpdateTime(new Date());
        reservationDao.updateById(reservation);
        updateUsage(usage, activeCount(parentUserId) + 1, reservedCount(parentUserId), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseReservation(String requestNo, String finalStatus, Long operatorId) {
        if (StrUtil.isBlank(requestNo)) return;
        JkRelationQuotaReservation reservation = reservationDao.selectOne(new LambdaQueryWrapper<JkRelationQuotaReservation>()
                .eq(JkRelationQuotaReservation::getRequestNo, requestNo)
                .eq(JkRelationQuotaReservation::getIsDeleted, false).last("limit 1"));
        if (reservation == null || !STATUS_RESERVED.equals(reservation.getStatus())) return;
        JkRelationQuotaUsage usage = lockUsage(reservation.getParentUserId());
        reservation.setStatus(StrUtil.blankToDefault(finalStatus, "RELEASED"))
                .setUpdateUserId(operatorId).setUpdateTime(new Date());
        reservationDao.updateById(reservation);
        updateUsage(usage, activeCount(reservation.getParentUserId()), reservedCount(reservation.getParentUserId()), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncUsage(Long parentUserId, Long operatorId) {
        if (parentUserId == null) return;
        JkRelationQuotaUsage usage = lockUsage(parentUserId);
        updateUsage(usage, activeCount(parentUserId), reservedCount(parentUserId), operatorId);
    }

    @Override
    public PageInfo<JkRelationLimitRule> listRules(String keyword, Boolean status, PageParamRequest pageParam) {
        Page<JkRelationLimitRule> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkRelationLimitRule> query = new LambdaQueryWrapper<JkRelationLimitRule>()
                .eq(JkRelationLimitRule::getIsDeleted, false)
                .orderByDesc(JkRelationLimitRule::getPriority)
                .orderByDesc(JkRelationLimitRule::getId);
        if (StrUtil.isNotBlank(keyword)) {
            query.and(q -> q.like(JkRelationLimitRule::getRuleCode, keyword.trim())
                    .or().like(JkRelationLimitRule::getRuleName, keyword.trim()));
        }
        if (status != null) query.eq(JkRelationLimitRule::getStatus, status);
        List<JkRelationLimitRule> rows = ruleDao.selectList(query);
        Date now = new Date();
        rows.forEach(row -> row.setEffective(isEffective(row, now)));
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkRelationLimitRule saveRule(JkRelationLimitRuleSaveRequest request, Long operatorId) {
        validateRuleRequest(request);
        JkRelationLimitRule duplicate = ruleDao.selectOne(new LambdaQueryWrapper<JkRelationLimitRule>()
                .eq(JkRelationLimitRule::getRuleCode, request.getRuleCode().trim())
                .eq(JkRelationLimitRule::getVersionNo, StrUtil.blankToDefault(request.getVersionNo(), "V1"))
                .eq(JkRelationLimitRule::getIsDeleted, false)
                .ne(request.getId() != null, JkRelationLimitRule::getId, request.getId()).last("limit 1"));
        if (duplicate != null) throw new IllegalArgumentException("相同规则编码和版本已存在");
        Date now = new Date();
        JkRelationLimitRule entity = request.getId() == null ? new JkRelationLimitRule()
                .setIsDeleted(false).setCreateUserId(operatorId).setCreateTime(now).setTenantId("000000")
                : requireRule(request.getId());
        entity.setRuleCode(request.getRuleCode().trim()).setRuleName(request.getRuleName().trim())
                .setPlanId(request.getPlanId()).setVersionNo(StrUtil.blankToDefault(request.getVersionNo(), "V1"))
                .setParentRoleCode(blankToNull(request.getParentRoleCode())).setChildRoleCode(blankToNull(request.getChildRoleCode()))
                .setRegionCode(blankToNull(request.getRegionCode())).setMaxDirectChildren(request.getMaxDirectChildren())
                .setWarningThreshold(request.getWarningThreshold() == null ? DEFAULT_WARNING_THRESHOLD : request.getWarningThreshold())
                .setOverflowPolicy(StrUtil.blankToDefault(request.getOverflowPolicy(), "REJECT").toUpperCase())
                .setPriority(request.getPriority() == null ? 0 : request.getPriority())
                .setEffectiveStartTime(request.getEffectiveStartTime()).setEffectiveEndTime(request.getEffectiveEndTime())
                .setStatus(request.getStatus() == null ? false : request.getStatus()).setRemark(request.getRemark())
                .setUpdateUserId(operatorId).setUpdateTime(now);
        if (request.getId() == null) ruleDao.insert(entity); else ruleDao.updateById(entity);
        return entity.setEffective(isEffective(entity, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkRelationLimitRule updateRuleStatus(Long id, Boolean status, Long operatorId) {
        JkRelationLimitRule entity = requireRule(id);
        entity.setStatus(Boolean.TRUE.equals(status)).setUpdateUserId(operatorId).setUpdateTime(new Date());
        ruleDao.updateById(entity);
        return entity.setEffective(isEffective(entity, new Date()));
    }

    private JkRelationQuotaUsage lockUsage(Long parentUserId) {
        usageDao.insertIgnore(parentUserId);
        JkRelationQuotaUsage usage = usageDao.selectForUpdate(parentUserId);
        if (usage == null) throw new IllegalStateException("无法锁定关系额度，请检查额度表初始化");
        return usage;
    }

    private void updateUsage(JkRelationQuotaUsage usage, int used, int reserved, Long operatorId) {
        usage.setUsedCount(Math.max(0, used)).setReservedCount(Math.max(0, reserved))
                .setVersion(usage.getVersion() == null ? 1 : usage.getVersion() + 1)
                .setUpdateUserId(operatorId).setUpdateTime(new Date());
        usageDao.updateById(usage);
    }

    private int activeCount(Long parentUserId) {
        Integer count = relationDao.selectCount(new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getParentUserId, parentUserId)
                .eq(JkAgentRelation::getStatus, true)
                .eq(JkAgentRelation::getIsDeleted, false));
        return count == null ? 0 : count;
    }

    private int reservedCount(Long parentUserId) {
        Integer count = reservationDao.selectCount(new LambdaQueryWrapper<JkRelationQuotaReservation>()
                .eq(JkRelationQuotaReservation::getParentUserId, parentUserId)
                .eq(JkRelationQuotaReservation::getStatus, STATUS_RESERVED)
                .eq(JkRelationQuotaReservation::getIsDeleted, false)
                .and(q -> q.isNull(JkRelationQuotaReservation::getExpireTime)
                        .or().gt(JkRelationQuotaReservation::getExpireTime, new Date())));
        return count == null ? 0 : count;
    }

    private void assertAvailable(JkRelationLimitRule rule, int used, int reserved, Long parentUserId) {
        int limit = rule.getMaxDirectChildren() == null ? DEFAULT_LIMIT : rule.getMaxDirectChildren();
        if (used + reserved < limit) return;
        if ("APPROVAL".equalsIgnoreCase(rule.getOverflowPolicy())) {
            throw new IllegalArgumentException("目标上级直属人数已达到上限，超额绑定必须走专项审批");
        }
        throw new IllegalArgumentException("目标上级直属人数已达到上限（" + limit + "人），不能继续绑定");
    }

    private JkRelationLimitRule matchRule(Identity parent, Identity child) {
        Date now = new Date();
        List<JkRelationLimitRule> rows = ruleDao.selectList(new LambdaQueryWrapper<JkRelationLimitRule>()
                .eq(JkRelationLimitRule::getStatus, true)
                .eq(JkRelationLimitRule::getIsDeleted, false)
                .and(q -> q.isNull(JkRelationLimitRule::getEffectiveStartTime)
                        .or().le(JkRelationLimitRule::getEffectiveStartTime, now))
                .and(q -> q.isNull(JkRelationLimitRule::getEffectiveEndTime)
                        .or().ge(JkRelationLimitRule::getEffectiveEndTime, now)));
        if (rows == null) rows = Collections.emptyList();
        return rows.stream()
                .filter(rule -> matches(rule.getParentRoleCode(), parent.roleCode))
                .filter(rule -> matches(rule.getChildRoleCode(), child.roleCode))
                .filter(rule -> matches(rule.getRegionCode(), parent.regionCode))
                .max(Comparator.comparingInt((JkRelationLimitRule rule) -> score(rule, parent, child))
                        .thenComparing(rule -> rule.getPriority() == null ? 0 : rule.getPriority())
                        .thenComparing(rule -> rule.getId() == null ? 0L : rule.getId()))
                .orElseGet(this::defaultRule);
    }

    private int score(JkRelationLimitRule rule, Identity parent, Identity child) {
        int score = 0;
        if (StrUtil.isNotBlank(rule.getParentRoleCode()) && Objects.equals(rule.getParentRoleCode(), parent.roleCode)) score += 8;
        if (StrUtil.isNotBlank(rule.getChildRoleCode()) && Objects.equals(rule.getChildRoleCode(), child.roleCode)) score += 4;
        if (StrUtil.isNotBlank(rule.getRegionCode()) && Objects.equals(rule.getRegionCode(), parent.regionCode)) score += 2;
        return score;
    }

    private JkRelationLimitRule defaultRule() {
        return new JkRelationLimitRule().setRuleCode("DEFAULT_DIRECT_LIMIT").setRuleName("默认直属人数限制")
                .setVersionNo("V1").setMaxDirectChildren(DEFAULT_LIMIT).setWarningThreshold(DEFAULT_WARNING_THRESHOLD)
                .setOverflowPolicy("REJECT").setPriority(-1).setStatus(true).setEffective(true);
    }

    private JkRelationQuotaResponse response(Long parentUserId, Identity parent, Identity child,
                                             JkRelationLimitRule rule, int used, int reserved) {
        int limit = rule.getMaxDirectChildren() == null ? DEFAULT_LIMIT : rule.getMaxDirectChildren();
        int remaining = Math.max(0, limit - used - reserved);
        int threshold = rule.getWarningThreshold() == null ? DEFAULT_WARNING_THRESHOLD : rule.getWarningThreshold();
        int percent = limit <= 0 ? 100 : (used + reserved) * 100 / limit;
        return new JkRelationQuotaResponse().setParentUserId(parentUserId).setParentRoleCode(parent.roleCode)
                .setChildRoleCode(child.roleCode).setRegionCode(parent.regionCode).setRuleId(rule.getId())
                .setRuleCode(rule.getRuleCode()).setRuleName(rule.getRuleName()).setMaxDirectChildren(limit)
                .setUsedCount(used).setReservedCount(reserved).setRemainingCount(remaining)
                .setWarningThreshold(threshold).setOverflowPolicy(rule.getOverflowPolicy())
                .setWarning(percent >= threshold).setFull(remaining <= 0);
    }

    private Identity requireEligibleParent(Long userId) {
        Identity identity = identity(userId);
        List<String> allowed = Arrays.asList(JkBizConstants.ROLE_MAKER, JkBizConstants.ROLE_PARTNER, JkBizConstants.ROLE_COUNTY_AGENT);
        if (!allowed.contains(identity.roleCode)) throw new IllegalArgumentException("目标上级不是当前版本允许发展的创客、合伙人或区县代理");
        return identity;
    }

    private Identity identity(Long userId) {
        if (userId == null) return new Identity(JkBizConstants.ROLE_NORMAL_USER, null);
        JkUserBusinessRole role = userRoleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, userId)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getEffectiveStatus, JkBizConstants.EFFECTIVE_STATUS_ENABLED)
                .eq(JkUserBusinessRole::getFreezeStatus, false)
                .eq(JkUserBusinessRole::getStatus, true)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId).last("limit 1"));
        return role == null ? new Identity(JkBizConstants.ROLE_NORMAL_USER, null)
                : new Identity(role.getRoleCode(), role.getRegionCode());
    }

    private void validateRuleRequest(JkRelationLimitRuleSaveRequest request) {
        if (request.getEffectiveStartTime() != null && request.getEffectiveEndTime() != null
                && !request.getEffectiveEndTime().after(request.getEffectiveStartTime())) {
            throw new IllegalArgumentException("失效时间必须晚于生效时间");
        }
        if (!Arrays.asList("REJECT", "APPROVAL").contains(StrUtil.blankToDefault(request.getOverflowPolicy(), "REJECT").toUpperCase())) {
            throw new IllegalArgumentException("超额策略只允许 REJECT 或 APPROVAL");
        }
        validateRole(request.getParentRoleCode(), true);
        validateRole(request.getChildRoleCode(), false);
    }

    private void validateRole(String roleCode, boolean parent) {
        if (StrUtil.isBlank(roleCode)) return;
        List<String> roles = parent
                ? Arrays.asList(JkBizConstants.ROLE_MAKER, JkBizConstants.ROLE_PARTNER, JkBizConstants.ROLE_COUNTY_AGENT)
                : Arrays.asList(JkBizConstants.ROLE_NORMAL_USER, JkBizConstants.ROLE_MAKER, JkBizConstants.ROLE_PARTNER, JkBizConstants.ROLE_COUNTY_AGENT);
        if (!roles.contains(roleCode)) throw new IllegalArgumentException("当前版本不支持该角色进入关系人数规则：" + roleCode);
    }

    private JkRelationLimitRule requireRule(Long id) {
        JkRelationLimitRule rule = ruleDao.selectById(id);
        if (rule == null || Boolean.TRUE.equals(rule.getIsDeleted())) throw new IllegalArgumentException("关系人数规则不存在");
        return rule;
    }

    private boolean isEffective(JkRelationLimitRule rule, Date now) {
        return Boolean.TRUE.equals(rule.getStatus())
                && (rule.getEffectiveStartTime() == null || !rule.getEffectiveStartTime().after(now))
                && (rule.getEffectiveEndTime() == null || !rule.getEffectiveEndTime().before(now));
    }

    private boolean matches(String configured, String actual) {
        return StrUtil.isBlank(configured) || Objects.equals(configured, actual);
    }

    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private static class Identity {
        private final String roleCode;
        private final String regionCode;
        private Identity(String roleCode, String regionCode) {
            this.roleCode = roleCode;
            this.regionCode = regionCode;
        }
    }
}
