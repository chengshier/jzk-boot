package com.zbkj.service.service.impl.jiuzhoukang.identity;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.jiuzhoukang.JkBizException;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplyAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplyRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplySearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyResponse;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyDetailResponse;
import com.zbkj.common.response.jiuzhoukang.JkAuditLogResponse;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.service.dao.jiuzhoukang.JkIdentityApplyDao;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.identity.IdentityEffectiveService;
import com.zbkj.service.service.jiuzhoukang.identity.JkIdentityApplyService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.scope.JkAdminDataScopeService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JkIdentityApplyServiceImpl extends ServiceImpl<JkIdentityApplyDao, JkIdentityApply> implements JkIdentityApplyService {

    @Autowired
    private JkBusinessRoleService businessRoleService;
    @Autowired
    private JkAuditLogService auditLogService;
    @Autowired
    private JkAdminActorService adminActorService;
    @Autowired
    private IdentityEffectiveService identityEffectiveService;
    @Autowired
    private JkPermissionCacheVersionService permissionCacheVersionService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;
    @Autowired
    private JkAdminDataScopeService adminDataScopeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkIdentityApplyResponse submitApply(Long userId, JkIdentityApplyRequest request) {
        validateFrontApplyRole(request.getApplyRoleCode());
        JkIdentityApply sameRequest = getOne(new LambdaQueryWrapper<JkIdentityApply>()
                .eq(JkIdentityApply::getRequestNo, request.getRequestNo())
                .last(" limit 1"));
        if (sameRequest != null) {
            return enrichSingle(toResponse(sameRequest, roleNameMap()));
        }
        LambdaQueryWrapper<JkIdentityApply> inProgressWrapper = new LambdaQueryWrapper<>();
        inProgressWrapper.eq(JkIdentityApply::getUserId, userId);
        inProgressWrapper.eq(JkIdentityApply::getApplyRoleCode, request.getApplyRoleCode());
        inProgressWrapper.in(JkIdentityApply::getAuditStatus,
                JkBizConstants.AUDIT_STATUS_PENDING,
                JkBizConstants.AUDIT_STATUS_EFFECTIVE,
                JkBizConstants.AUDIT_STATUS_FROZEN);
        inProgressWrapper.eq(JkIdentityApply::getIsDeleted, false);
        inProgressWrapper.last(" limit 1");
        JkIdentityApply inProgress = getOne(inProgressWrapper);
        if (inProgress != null) {
            throw new JkBizException("该身份已有进行中或生效中的申请");
        }
        JkIdentityApply apply = new JkIdentityApply();
        BeanUtils.copyProperties(request, apply);
        apply.setUserId(userId);
        apply.setApplyNo(generateApplyNo());
        apply.setBusinessNo(apply.getApplyNo());
        apply.setAuditStatus(JkBizConstants.AUDIT_STATUS_PENDING);
        apply.setFreezeStatus(false);
        apply.setCurrentAuditLevel(1);
        apply.setVersion(0);
        apply.setStatus(true);
        apply.setIsDeleted(false);
        apply.setCreateUserId(userId);
        apply.setUpdateUserId(userId);
        apply.setCreateTime(DateUtil.date());
        apply.setUpdateTime(DateUtil.date());
        apply.setTenantId("000000");
        save(apply);
        return enrichSingle(toResponse(apply, roleNameMap()));
    }

    @Override
    public List<JkIdentityApplyResponse> getMyApplyList(Long userId, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkIdentityApply> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkIdentityApply::getUserId, userId);
        lqw.eq(JkIdentityApply::getIsDeleted, false);
        lqw.orderByDesc(JkIdentityApply::getId);
        List<JkIdentityApplyResponse> responses = list(lqw).stream().map(item -> toResponse(item, roleNameMap())).collect(Collectors.toList());
        displayEnrichmentSupport.enrichIdentityApplies(responses);
        return responses;
    }

    @Override
    public JkIdentityApplyDetailResponse getMyApplyDetail(Long userId, Long applyId) {
        JkIdentityApply apply = getById(applyId);
        if (apply == null || Boolean.TRUE.equals(apply.getIsDeleted()) || !userId.equals(apply.getUserId())) {
            throw new JkBizException("申请记录不存在");
        }
        LambdaQueryWrapper<JkAuditLog> logQuery = new LambdaQueryWrapper<>();
        logQuery.eq(JkAuditLog::getBusinessType, JkBizConstants.BUSINESS_TYPE_IDENTITY_APPLY);
        logQuery.eq(JkAuditLog::getBusinessId, apply.getId());
        logQuery.eq(JkAuditLog::getIsDeleted, false);
        logQuery.orderByAsc(JkAuditLog::getId);
        List<JkAuditLogResponse> logs = auditLogService.toResponses(auditLogService.list(logQuery));
        JkIdentityApplyDetailResponse response = new JkIdentityApplyDetailResponse();
        response.setApplication(enrichSingle(toResponse(apply, roleNameMap())));
        response.setAuditLogs(logs);
        return response;
    }

    @Override
    public List<JkIdentityApplyResponse> getAdminApplyList(JkIdentityApplySearchRequest request, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkIdentityApply> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkIdentityApply::getIsDeleted, false);
        adminDataScopeService.applyIdentityApplyScope(lqw);
        if (request != null && StrUtil.isNotBlank(request.getAuditStatus())) {
            lqw.eq(JkIdentityApply::getAuditStatus, request.getAuditStatus());
        }
        if (request != null && StrUtil.isNotBlank(request.getApplyRoleCode())) {
            lqw.eq(JkIdentityApply::getApplyRoleCode, request.getApplyRoleCode());
        }
        if (request != null && StrUtil.isNotBlank(request.getRegionCode())) {
            lqw.eq(JkIdentityApply::getRegionCode, request.getRegionCode());
        }
        lqw.orderByDesc(JkIdentityApply::getId);
        List<JkIdentityApplyResponse> responses = list(lqw).stream().map(item -> toResponse(item, roleNameMap())).collect(Collectors.toList());
        displayEnrichmentSupport.enrichIdentityApplies(responses);
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean auditApply(JkIdentityApplyAuditRequest request) {
        JkIdentityApply apply = getById(request.getApplyId());
        if (apply == null || Boolean.TRUE.equals(apply.getIsDeleted())) {
            throw new JkBizException("申请记录不存在");
        }
        adminDataScopeService.assertCanManageIdentityApply(apply);
        if (!JkBizConstants.AUDIT_STATUS_PENDING.equals(apply.getAuditStatus())) {
            throw new JkBizException("当前申请状态不可审核");
        }
        Long adminId = Long.valueOf(adminActorService.getCurrentAdmin().getId());
        String adminName = adminActorService.getCurrentAdmin().getRealName();
        if (JkBizConstants.AUDIT_ACTION_PASS.equals(request.getAuditAction())) {
            apply.setAuditStatus(JkBizConstants.AUDIT_STATUS_EFFECTIVE);
            apply.setRejectReason(null);
            apply.setEffectiveTime(new Date());
            apply.setUpdateUserId(adminId);
            apply.setUpdateTime(DateUtil.date());
            updateById(apply);
            identityEffectiveService.effectiveIdentity(apply, adminId, adminName, request.getAuditRemark());
            permissionCacheVersionService.refreshUserCacheVersion(apply.getUserId(), JkBizConstants.CACHE_CHANGE_IDENTITY_STATUS, "identity pass", adminId);
            return true;
        }
        apply.setAuditStatus(JkBizConstants.AUDIT_STATUS_REJECTED);
        apply.setRejectReason(request.getRejectReason());
        apply.setUpdateUserId(adminId);
        apply.setUpdateTime(DateUtil.date());
        boolean updated = updateById(apply);
        JkAuditLog auditLog = new JkAuditLog();
        auditLog.setBusinessType(JkBizConstants.BUSINESS_TYPE_IDENTITY_APPLY);
        auditLog.setBusinessId(apply.getId());
        auditLog.setBusinessNo(apply.getApplyNo());
        auditLog.setRequestNo(apply.getRequestNo());
        auditLog.setAuditUserId(adminId);
        auditLog.setAuditUserName(adminName);
        auditLog.setAuditUserType("ADMIN");
        auditLog.setAuditAction(JkBizConstants.AUDIT_ACTION_REJECT);
        auditLog.setBeforeStatus(JkBizConstants.AUDIT_STATUS_PENDING);
        auditLog.setAfterStatus(JkBizConstants.AUDIT_STATUS_REJECTED);
        auditLog.setRejectReason(request.getRejectReason());
        auditLog.setAuditRemark(request.getAuditRemark());
        auditLog.setOperateSource(JkBizConstants.OPERATE_SOURCE_ADMIN);
        auditLog.setStatus(true);
        auditLog.setIsDeleted(false);
        auditLog.setCreateUserId(adminId);
        auditLog.setUpdateUserId(adminId);
        auditLog.setCreateTime(DateUtil.date());
        auditLog.setUpdateTime(DateUtil.date());
        auditLogService.saveAuditLog(auditLog);
        permissionCacheVersionService.refreshUserCacheVersion(apply.getUserId(), JkBizConstants.CACHE_CHANGE_IDENTITY_STATUS, "identity reject", adminId);
        return updated;
    }

    private void validateFrontApplyRole(String applyRoleCode) {
        LambdaQueryWrapper<JkBusinessRole> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkBusinessRole::getRoleCode, applyRoleCode);
        lqw.eq(JkBusinessRole::getEnabled, true);
        lqw.eq(JkBusinessRole::getAllowFrontApply, true);
        lqw.eq(JkBusinessRole::getIsDeleted, false);
        lqw.last(" limit 1");
        JkBusinessRole role = businessRoleService.getOne(lqw);
        if (role == null) {
            throw new JkBizException("该身份暂不支持前台申请");
        }
    }

    private String generateApplyNo() {
        Long seq = redisUtil.incrAndCreate("jk:identity:apply:no:" + DateUtil.today().replace("-", ""), 1L);
        return "JKIA" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + String.format("%04d", seq);
    }

    private Map<String, String> roleNameMap() {
        List<JkBusinessRole> roles = businessRoleService.getEnabledRoleList();
        if (roles.isEmpty()) {
            return Collections.emptyMap();
        }
        return roles.stream().collect(Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
    }

    private JkIdentityApplyResponse toResponse(JkIdentityApply item, Map<String, String> roleNameMap) {
        JkIdentityApplyResponse response = new JkIdentityApplyResponse();
        BeanUtils.copyProperties(item, response);
        response.setApplyRoleName(ObjectUtil.defaultIfNull(roleNameMap.get(item.getApplyRoleCode()), item.getApplyRoleCode()));
        return response;
    }

    private JkIdentityApplyResponse enrichSingle(JkIdentityApplyResponse response) {
        List<JkIdentityApplyResponse> list = Collections.singletonList(response);
        displayEnrichmentSupport.enrichIdentityApplies(list);
        return response;
    }
}
