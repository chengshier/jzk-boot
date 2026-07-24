package com.zbkj.service.service.impl.jiuzhoukang.identity;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.jiuzhoukang.JkBizException;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkUserBusinessRoleSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkUserBusinessRoleResponse;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionTriggerService;
import com.zbkj.service.service.jiuzhoukang.identity.JkIdentityApplyService;
import com.zbkj.service.service.jiuzhoukang.identity.JkUserBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.scope.JkAdminDataScopeService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JkUserBusinessRoleServiceImpl extends ServiceImpl<JkUserBusinessRoleDao, JkUserBusinessRole> implements JkUserBusinessRoleService {

    @Autowired
    private UserService userService;
    @Autowired
    private JkBusinessRoleService businessRoleService;
    @Autowired
    private JkPermissionCacheVersionService permissionCacheVersionService;
    @Autowired
    private JkAuditLogService auditLogService;
    @Autowired
    private JkAdminActorService adminActorService;
    @Autowired
    private JkIdentityApplyService identityApplyService;
    @Autowired
    private CommissionTriggerService commissionTriggerService;
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;
    @Autowired
    private JkAdminDataScopeService adminDataScopeService;

    @Override
    public List<JkUserBusinessRole> getUserRoles(Long userId) {
        LambdaQueryWrapper<JkUserBusinessRole> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkUserBusinessRole::getUserId, userId);
        lqw.eq(JkUserBusinessRole::getIsDeleted, false);
        lqw.orderByDesc(JkUserBusinessRole::getIsPrimary).orderByDesc(JkUserBusinessRole::getId);
        return list(lqw);
    }

    @Override
    public List<JkUserBusinessRoleResponse> getAdminList(JkUserBusinessRoleSearchRequest request, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkUserBusinessRole> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkUserBusinessRole::getIsDeleted, false);
        adminDataScopeService.applyUserBusinessRoleScope(lqw);
        if (request != null && StrUtil.isNotBlank(request.getRoleCode())) {
            lqw.eq(JkUserBusinessRole::getRoleCode, request.getRoleCode());
        }
        if (request != null && StrUtil.isNotBlank(request.getAuditStatus())) {
            lqw.eq(JkUserBusinessRole::getAuditStatus, request.getAuditStatus());
        }
        if (request != null && request.getFreezeStatus() != null) {
            lqw.eq(JkUserBusinessRole::getFreezeStatus, request.getFreezeStatus());
        }
        List<JkUserBusinessRole> rows = list(lqw);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, User> userMap = userService.listByIds(rows.stream().map(item -> item.getUserId().intValue()).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(User::getUid, item -> item));
        Map<String, JkBusinessRole> roleMap = businessRoleService.getEnabledRoleList().stream().collect(Collectors.toMap(JkBusinessRole::getRoleCode, item -> item, (a, b) -> a));
        List<JkUserBusinessRoleResponse> responses = rows.stream().map(item -> {
            JkUserBusinessRoleResponse response = new JkUserBusinessRoleResponse();
            response.setId(item.getId());
            response.setUserId(item.getUserId());
            User user = userMap.get(item.getUserId().intValue());
            if (user != null) {
                response.setNickname(user.getNickname());
                response.setPhone(user.getPhone());
            }
            response.setRoleCode(item.getRoleCode());
            JkBusinessRole role = roleMap.get(item.getRoleCode());
            response.setRoleName(role == null ? item.getRoleCode() : role.getRoleName());
            response.setIsPrimary(item.getIsPrimary());
            response.setAuditStatus(item.getAuditStatus());
            response.setFreezeStatus(item.getFreezeStatus());
            response.setFreezeReason(item.getFreezeReason());
            response.setRegionCode(item.getRegionCode());
            response.setBelongCountyAgentId(item.getBelongCountyAgentId());
            response.setEffectiveTime(item.getEffectiveTime());
            response.setPermissionCodes(role == null ? Collections.emptyList() : businessRoleService.getPermissionCodes(role.getId()));
            return response;
        }).collect(Collectors.toList());
        displayEnrichmentSupport.enrichUserBusinessRoles(responses);
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean freeze(Long userBusinessRoleId, String reason) {
        return updateIdentityStatus(userBusinessRoleId, JkBizConstants.AUDIT_STATUS_FROZEN, true, reason, JkBizConstants.AUDIT_ACTION_FREEZE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unfreeze(Long userBusinessRoleId, String reason) {
        return updateIdentityStatus(userBusinessRoleId, JkBizConstants.AUDIT_STATUS_EFFECTIVE, false, reason, JkBizConstants.AUDIT_ACTION_UNFREEZE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(Long userBusinessRoleId, String reason) {
        JkUserBusinessRole role = getRequiredRole(userBusinessRoleId);
        adminDataScopeService.assertCanManageUserBusinessRole(role);
        SystemAdmin admin = adminActorService.getCurrentAdmin();
        String beforeStatus = role.getAuditStatus();
        role.setAuditStatus(JkBizConstants.AUDIT_STATUS_CANCELLED);
        role.setFreezeStatus(false);
        role.setEffectiveStatus(JkBizConstants.EFFECTIVE_STATUS_DISABLED);
        role.setFreezeReason(reason);
        role.setUpdateUserId(Long.valueOf(admin.getId()));
        role.setUpdateTime(DateUtil.date());
        boolean result = updateById(role);
        saveIdentityAuditLog(role, admin, JkBizConstants.AUDIT_ACTION_CANCEL, beforeStatus, JkBizConstants.AUDIT_STATUS_CANCELLED, reason);
        permissionCacheVersionService.refreshUserCacheVersion(role.getUserId(), JkBizConstants.CACHE_CHANGE_IDENTITY_STATUS, "identity cancel", Long.valueOf(admin.getId()));
        if (result) commissionTriggerService.onIdentityFrozen(role.getUserId(), "IDENTITY_CANCEL:" + role.getId() + ":" + role.getUpdateTime().getTime());
        return result;
    }

    private Boolean updateIdentityStatus(Long id, String auditStatus, boolean freezeStatus, String reason, String auditAction) {
        JkUserBusinessRole role = getRequiredRole(id);
        adminDataScopeService.assertCanManageUserBusinessRole(role);
        SystemAdmin admin = adminActorService.getCurrentAdmin();
        String beforeStatus = role.getAuditStatus();
        role.setAuditStatus(auditStatus);
        role.setFreezeStatus(freezeStatus);
        role.setFreezeReason(reason);
        role.setEffectiveStatus(freezeStatus ? JkBizConstants.EFFECTIVE_STATUS_DISABLED : JkBizConstants.EFFECTIVE_STATUS_ENABLED);
        role.setUpdateUserId(Long.valueOf(admin.getId()));
        role.setUpdateTime(DateUtil.date());
        boolean result = updateById(role);
        saveIdentityAuditLog(role, admin, auditAction, beforeStatus, auditStatus, reason);
        permissionCacheVersionService.refreshUserCacheVersion(role.getUserId(), JkBizConstants.CACHE_CHANGE_IDENTITY_STATUS, reason, Long.valueOf(admin.getId()));
        if (result && freezeStatus) commissionTriggerService.onIdentityFrozen(role.getUserId(), "IDENTITY_FREEZE:" + role.getId() + ":" + role.getUpdateTime().getTime());
        if (result && !freezeStatus) commissionTriggerService.onIdentityUnfrozen(role.getUserId(), "IDENTITY_UNFREEZE:" + role.getId() + ":" + role.getUpdateTime().getTime());
        return result;
    }

    private void saveIdentityAuditLog(JkUserBusinessRole role, SystemAdmin admin, String auditAction,
                                      String beforeStatus, String afterStatus, String reason) {
        JkAuditLog auditLog = new JkAuditLog();
        auditLog.setBusinessType(JkBizConstants.BUSINESS_TYPE_IDENTITY_APPLY);
        auditLog.setBusinessId(role.getId());
        auditLog.setBusinessNo(role.getBusinessNo());
        auditLog.setRequestNo(resolveApplyRequestNo(role.getApplyId()));
        auditLog.setAuditUserId(Long.valueOf(admin.getId()));
        auditLog.setAuditUserName(admin.getRealName());
        auditLog.setAuditUserType("ADMIN");
        auditLog.setAuditAction(auditAction);
        auditLog.setBeforeStatus(beforeStatus);
        auditLog.setAfterStatus(afterStatus);
        auditLog.setRejectReason(reason);
        auditLog.setAuditRemark(reason);
        auditLog.setOperateSource(JkBizConstants.OPERATE_SOURCE_ADMIN);
        auditLog.setStatus(true);
        auditLog.setIsDeleted(false);
        auditLog.setCreateUserId(Long.valueOf(admin.getId()));
        auditLog.setUpdateUserId(Long.valueOf(admin.getId()));
        auditLog.setCreateTime(DateUtil.date());
        auditLog.setUpdateTime(DateUtil.date());
        auditLogService.saveAuditLog(auditLog);
    }

    private String resolveApplyRequestNo(Long applyId) {
        if (applyId == null) {
            return null;
        }
        JkIdentityApply apply = identityApplyService.getById(applyId);
        return apply == null ? null : apply.getRequestNo();
    }

    private JkUserBusinessRole getRequiredRole(Long id) {
        JkUserBusinessRole role = getById(id);
        if (role == null || Boolean.TRUE.equals(role.getIsDeleted())) {
            throw new JkBizException("用户业务身份不存在");
        }
        return role;
    }
}
