package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationBindRequest;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationForceAdjustRequest;
import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentRelationService;
import com.zbkj.service.service.jiuzhoukang.region.JkRelationQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JkAgentRelationServiceImpl implements JkAgentRelationService {
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private UserService userService;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private JkBusinessRoleDao roleDao;
    @Autowired private JkRegionDao regionDao;
    @Autowired private JkPermissionCacheVersionService cacheVersionService;
    @Autowired private JkRelationQuotaService quotaService;
    @Autowired private JkAuditLogService auditLogService;

    @Override
    public List<JkAgentRelationResponse> list(Long userId, Long parentUserId, Boolean activeOnly) {
        LambdaQueryWrapper<JkAgentRelation> query = new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getIsDeleted, false).orderByDesc(JkAgentRelation::getId);
        if (userId != null) query.eq(JkAgentRelation::getUserId, userId);
        if (parentUserId != null) query.eq(JkAgentRelation::getParentUserId, parentUserId);
        if (Boolean.TRUE.equals(activeOnly)) query.eq(JkAgentRelation::getStatus, true);
        List<JkAgentRelation> rows = relationDao.selectList(query);

        Set<Long> userIds = new HashSet<Long>();
        for (JkAgentRelation relation : rows) {
            if (relation.getUserId() != null) userIds.add(relation.getUserId());
            if (relation.getParentUserId() != null) userIds.add(relation.getParentUserId());
        }
        if (userIds.isEmpty()) return Collections.emptyList();

        List<Integer> integerIds = userIds.stream().map(Long::intValue).collect(Collectors.toList());
        Map<Integer, User> users = userService.listByIds(integerIds).stream()
                .collect(Collectors.toMap(User::getUid, value -> value, (a, b) -> a));
        Map<Long, JkUserBusinessRole> identityMap = loadPrimaryIdentities(userIds);
        Map<String, String> roleNames = roleDao.selectList(new LambdaQueryWrapper<JkBusinessRole>()
                        .eq(JkBusinessRole::getIsDeleted, false)).stream()
                .collect(Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
        Set<String> regionCodes = identityMap.values().stream().map(JkUserBusinessRole::getRegionCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Map<String, String> regionNames = regionCodes.isEmpty() ? Collections.emptyMap()
                : regionDao.selectList(new LambdaQueryWrapper<JkRegion>()
                .in(JkRegion::getRegionCode, regionCodes).eq(JkRegion::getIsDeleted, false)).stream()
                .collect(Collectors.toMap(JkRegion::getRegionCode, JkRegion::getRegionName, (a, b) -> a));

        List<JkAgentRelationResponse> result = new ArrayList<JkAgentRelationResponse>();
        for (JkAgentRelation row : rows) {
            JkAgentRelationResponse response = new JkAgentRelationResponse();
            response.setId(row.getId());
            response.setUserId(row.getUserId());
            response.setParentUserId(row.getParentUserId());
            response.setRootUserId(row.getRootUserId());
            response.setRelationType(row.getRelationType());
            response.setBindSource(row.getBindSource());
            response.setSourceCode(row.getSourceCode());
            response.setEffectiveTime(row.getEffectiveTime());
            response.setExpireTime(row.getExpireTime());
            response.setChangeReason(row.getChangeReason());
            response.setRemark(row.getRemark());
            response.setStatus(row.getStatus());
            response.setCreateTime(row.getCreateTime());

            User user = row.getUserId() == null ? null : users.get(row.getUserId().intValue());
            User parent = row.getParentUserId() == null ? null : users.get(row.getParentUserId().intValue());
            if (user != null) {
                response.setUserName(StrUtil.blankToDefault(user.getRealName(), user.getNickname()));
                response.setUserPhone(user.getPhone());
                response.setUserAvatar(user.getAvatar());
            }
            if (parent != null) {
                response.setParentName(StrUtil.blankToDefault(parent.getRealName(), parent.getNickname()));
                response.setParentPhone(parent.getPhone());
            }
            JkUserBusinessRole identity = identityMap.get(row.getUserId());
            if (identity != null) {
                response.setRoleCode(identity.getRoleCode());
                response.setRoleName(roleNames.getOrDefault(identity.getRoleCode(), identity.getRoleCode()));
                response.setRegionCode(identity.getRegionCode());
                response.setRegionName(StrUtil.isBlank(identity.getRegionCode()) ? null : regionNames.get(identity.getRegionCode()));
                response.setFreezeStatus(identity.getFreezeStatus());
                response.setIdentityStatusText(Boolean.TRUE.equals(identity.getFreezeStatus()) ? "身份冻结" : "身份正常");
            }
            result.add(response);
        }
        return result;
    }

    private Map<Long, JkUserBusinessRole> loadPrimaryIdentities(Set<Long> userIds) {
        List<JkUserBusinessRole> roles = userRoleDao.selectList(new LambdaQueryWrapper<JkUserBusinessRole>()
                .in(JkUserBusinessRole::getUserId, userIds)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId));
        Map<Long, JkUserBusinessRole> result = new LinkedHashMap<Long, JkUserBusinessRole>();
        for (JkUserBusinessRole role : roles) result.putIfAbsent(role.getUserId(), role);
        return result;
    }

    /**
     * 普通绑定只允许首次建立关系。已有有效上级时不能被扫码、邀请码或后台普通接口覆盖。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkAgentRelationResponse bind(JkAgentRelationBindRequest request, Long operatorId) {
        validateRequest(request);
        JkAgentRelation current = currentRelation(request.getUserId());
        if (current != null) {
            if (Objects.equals(current.getParentUserId(), request.getParentUserId())) return responseOf(request.getUserId());
            throw new IllegalArgumentException("当前用户已有有效上级，请提交换绑申请；管理员特殊调整请使用强制调整入口");
        }
        assertNoCycle(request.getUserId(), request.getParentUserId());
        quotaService.occupy(request.getParentUserId(), request.getUserId(), operatorId);
        JkAgentRelation created = createRelation(request, null, operatorId);
        // 九州康关系以 jk_agent_relation 为唯一事实来源，不能再回写 CRMEB spreadUid。
        quotaService.syncUsage(request.getParentUserId(), operatorId);
        saveAudit(created, "INITIAL_BIND", null, request.getParentUserId(), request.getChangeReason(), operatorId);
        refresh(request.getUserId(), "首次绑定上下级关系", operatorId);
        return responseOf(request.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkAgentRelationResponse changeFromApprovedApply(JkAgentRelationBindRequest request, Long currentRelationId,
                                                            String reservationRequestNo, Long operatorId) {
        validateRequest(request);
        JkAgentRelation current = currentRelation(request.getUserId());
        if (current == null || !Objects.equals(current.getId(), currentRelationId)) {
            throw new IllegalArgumentException("当前关系已变化，不能执行本次换绑");
        }
        if (Objects.equals(current.getParentUserId(), request.getParentUserId())) return responseOf(request.getUserId());
        assertNoCycle(request.getUserId(), request.getParentUserId());
        quotaService.consume(reservationRequestNo, request.getParentUserId(), request.getUserId(), operatorId);
        Long oldParentUserId = current.getParentUserId();
        JkAgentRelation created = replaceCurrent(current, request, operatorId);
        // 换绑同样只维护九州康关系，避免重新启用 CRMEB 推广链路。
        quotaService.syncUsage(oldParentUserId, operatorId);
        quotaService.syncUsage(request.getParentUserId(), operatorId);
        saveAudit(created, "CHANGE_APPROVED", oldParentUserId, request.getParentUserId(), request.getChangeReason(), operatorId);
        refresh(request.getUserId(), "换绑申请审核通过", operatorId);
        return responseOf(request.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkAgentRelationResponse forceAdjust(JkAgentRelationForceAdjustRequest request, Long operatorId) {
        if (request == null || request.getUserId() == null) throw new IllegalArgumentException("下级用户不能为空");
        if (StrUtil.isBlank(request.getReason())) throw new IllegalArgumentException("管理员强制调整必须填写原因");
        JkAgentRelationBindRequest bind = new JkAgentRelationBindRequest();
        bind.setUserId(request.getUserId());
        bind.setParentUserId(request.getParentUserId());
        bind.setRelationType("DIRECT");
        bind.setBindSource("ADMIN_FORCE");
        bind.setSourceCode(request.getSourceCode());
        bind.setChangeReason(request.getReason().trim());
        bind.setRemark(request.getRemark());
        validateRequest(bind);

        JkAgentRelation current = currentRelation(request.getUserId());
        if (current != null && Objects.equals(current.getParentUserId(), request.getParentUserId())) {
            saveAudit(current, "FORCE_ADJUST_NO_CHANGE", current.getParentUserId(), request.getParentUserId(), request.getReason(), operatorId);
            return responseOf(request.getUserId());
        }
        assertNoCycle(request.getUserId(), request.getParentUserId());
        quotaService.occupy(request.getParentUserId(), request.getUserId(), operatorId);
        Long oldParentUserId = current == null ? null : current.getParentUserId();
        JkAgentRelation created = current == null ? createRelation(bind, null, operatorId) : replaceCurrent(current, bind, operatorId);
        // 管理员调整不与 CRMEB 分销字段耦合。
        quotaService.syncUsage(oldParentUserId, operatorId);
        quotaService.syncUsage(request.getParentUserId(), operatorId);
        saveAudit(created, "ADMIN_FORCE_ADJUST", oldParentUserId, request.getParentUserId(), request.getReason(), operatorId);
        refresh(request.getUserId(), "管理员强制调整上下级关系", operatorId);
        return responseOf(request.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean invalidate(Long id, String reason, Long operatorId) {
        if (StrUtil.isBlank(reason)) throw new IllegalArgumentException("关系失效原因不能为空");
        JkAgentRelation entity = relationDao.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) return false;
        if (!Boolean.TRUE.equals(entity.getStatus())) return true;
        Date now = new Date();
        Long oldParentUserId = entity.getParentUserId();
        entity.setStatus(false).setExpireTime(now).setChangeReason(reason.trim()).setUpdateUserId(operatorId).setUpdateTime(now);
        int result = relationDao.updateById(entity);
        if (result != 1) throw new IllegalStateException("关系状态已变化，请刷新后重试");
        // 作废九州康关系不触碰 CRMEB spreadUid。
        quotaService.syncUsage(oldParentUserId, operatorId);
        saveAudit(entity, "INVALIDATE", oldParentUserId, null, reason, operatorId);
        refresh(entity.getUserId(), "上下级关系失效", operatorId);
        return true;
    }

    private void validateRequest(JkAgentRelationBindRequest request) {
        if (request == null || request.getUserId() == null) throw new IllegalArgumentException("下级用户不能为空");
        if (request.getParentUserId() != null && request.getUserId().equals(request.getParentUserId())) {
            throw new IllegalArgumentException("不能绑定自己为上级");
        }
        User user = userService.getById(request.getUserId().intValue());
        if (user == null) throw new IllegalArgumentException("下级用户不存在");
        if (request.getParentUserId() != null && userService.getById(request.getParentUserId().intValue()) == null) {
            throw new IllegalArgumentException("上级用户不存在");
        }
    }

    private JkAgentRelation replaceCurrent(JkAgentRelation current, JkAgentRelationBindRequest request, Long operatorId) {
        Date now = new Date();
        current.setStatus(false).setExpireTime(now)
                .setChangeReason(StrUtil.blankToDefault(request.getChangeReason(), "关系换绑"))
                .setUpdateUserId(operatorId).setUpdateTime(now);
        if (relationDao.updateById(current) != 1) throw new IllegalStateException("原关系已变化，请刷新后重试");
        return createRelation(request, current.getId(), operatorId);
    }

    private JkAgentRelation createRelation(JkAgentRelationBindRequest request, Long replacedRelationId, Long operatorId) {
        Date now = new Date();
        Long root = resolveRoot(request.getParentUserId());
        JkAgentRelation entity = new JkAgentRelation().setUserId(request.getUserId())
                .setParentUserId(request.getParentUserId()).setRootUserId(root)
                .setRelationType(StrUtil.blankToDefault(request.getRelationType(), "DIRECT"))
                .setBindSource(StrUtil.blankToDefault(request.getBindSource(), "ADMIN"))
                .setSourceCode(request.getSourceCode()).setEffectiveTime(now)
                .setReplacedRelationId(replacedRelationId).setChangeReason(request.getChangeReason()).setRemark(request.getRemark())
                .setStatus(true).setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId)
                .setCreateTime(now).setUpdateTime(now).setTenantId("000000");
        relationDao.insert(entity);
        return entity;
    }

    private void saveAudit(JkAgentRelation relation, String action, Long oldParentUserId, Long newParentUserId,
                           String reason, Long operatorId) {
        Date now = new Date();
        String beforeStatus = oldParentUserId == null ? "PLATFORM" : String.valueOf(oldParentUserId);
        String afterStatus = newParentUserId == null ? "PLATFORM" : String.valueOf(newParentUserId);
        JkAuditLog log = new JkAuditLog().setBusinessType("AGENT_RELATION")
                .setBusinessId(relation == null ? null : relation.getId())
                .setBusinessNo(relation == null ? null : relation.getSourceCode())
                .setRequestNo(relation == null ? null : relation.getSourceCode())
                .setAuditUserId(operatorId).setAuditUserType(operatorId != null && operatorId < 0 ? "ADMIN" : "BUSINESS_USER")
                .setAuditAction(action).setBeforeStatus(beforeStatus).setAfterStatus(afterStatus)
                .setAuditRemark(reason).setOperateSource(JkBizConstants.OPERATE_SOURCE_ADMIN)
                .setStatus(true).setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId)
                .setCreateTime(now).setUpdateTime(now).setTenantId("000000");
        auditLogService.saveAuditLog(log);
    }

    private void refresh(Long userId, String reason, Long operatorId) {
        cacheVersionService.refreshUserCacheVersion(userId, "AGENT_RELATION", reason, operatorId);
    }

    private JkAgentRelation currentRelation(Long userId) {
        return relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getUserId, userId).eq(JkAgentRelation::getStatus, true)
                .eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
    }

    private JkAgentRelationResponse responseOf(Long userId) {
        List<JkAgentRelationResponse> rows = list(userId, null, true);
        if (rows.isEmpty()) throw new IllegalStateException("关系保存后未查询到有效记录");
        return rows.get(0);
    }

    private void assertNoCycle(Long userId, Long parentUserId) {
        Long cursor = parentUserId;
        Set<Long> seen = new HashSet<Long>();
        for (int i = 0; cursor != null && i < 100; i++) {
            if (userId.equals(cursor)) throw new IllegalArgumentException("绑定会形成循环关系");
            if (!seen.add(cursor)) throw new IllegalArgumentException("现有关系链存在循环，请先修复");
            JkAgentRelation relation = relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                    .eq(JkAgentRelation::getUserId, cursor).eq(JkAgentRelation::getStatus, true)
                    .eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
            cursor = relation == null ? null : relation.getParentUserId();
        }
        if (cursor != null) throw new IllegalArgumentException("关系层级超过系统安全上限");
    }

    private Long resolveRoot(Long parentUserId) {
        if (parentUserId == null) return null;
        Long cursor = parentUserId;
        Long root = parentUserId;
        for (int i = 0; i < 100; i++) {
            JkAgentRelation relation = relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                    .eq(JkAgentRelation::getUserId, cursor).eq(JkAgentRelation::getStatus, true)
                    .eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
            if (relation == null || relation.getParentUserId() == null) {
                return relation != null && relation.getRootUserId() != null ? relation.getRootUserId() : root;
            }
            root = relation.getParentUserId();
            cursor = relation.getParentUserId();
        }
        return root;
    }
}
