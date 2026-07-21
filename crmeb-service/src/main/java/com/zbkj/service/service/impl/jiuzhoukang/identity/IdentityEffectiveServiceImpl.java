package com.zbkj.service.service.impl.jiuzhoukang.identity;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.identity.IdentityEffectiveService;
import com.zbkj.service.service.jiuzhoukang.identity.JkUserBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.scope.JkUserDataScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IdentityEffectiveServiceImpl implements IdentityEffectiveService {

    @Autowired
    private JkUserBusinessRoleService userBusinessRoleService;
    @Autowired
    private JkBusinessRoleService businessRoleService;
    @Autowired
    private JkUserDataScopeService userDataScopeService;
    @Autowired
    private JkAuditLogService auditLogService;
    @Autowired
    private JkPermissionCacheVersionService permissionCacheVersionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void effectiveIdentity(JkIdentityApply apply, Long auditUserId, String auditUserName, String auditRemark) {
        JkBusinessRole role = businessRoleService.getEnabledRoleList().stream()
                .filter(item -> item.getRoleCode().equals(apply.getApplyRoleCode()))
                .findFirst().orElse(null);
        if (role == null) {
            return;
        }
        LambdaQueryWrapper<JkUserBusinessRole> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkUserBusinessRole::getUserId, apply.getUserId());
        lqw.eq(JkUserBusinessRole::getRoleCode, apply.getApplyRoleCode());
        lqw.last(" limit 1");
        JkUserBusinessRole binding = userBusinessRoleService.getOne(lqw);
        if (binding == null) {
            binding = new JkUserBusinessRole();
            binding.setUserId(apply.getUserId());
            binding.setRoleId(role.getId());
            binding.setRoleCode(role.getRoleCode());
            binding.setIsPrimary(true);
            binding.setCreateTime(DateUtil.date());
        }
        binding.setApplyId(apply.getId());
        binding.setRegionCode(apply.getRegionCode());
        binding.setBelongCountyAgentId(apply.getBelongCountyAgentId());
        binding.setAuditStatus(JkBizConstants.AUDIT_STATUS_EFFECTIVE);
        binding.setFreezeStatus(false);
        binding.setEffectiveStatus(JkBizConstants.EFFECTIVE_STATUS_ENABLED);
        binding.setEffectiveTime(DateUtil.date());
        binding.setStatus(true);
        binding.setIsDeleted(false);
        binding.setUpdateTime(DateUtil.date());
        if (binding.getId() == null) {
            userBusinessRoleService.save(binding);
        } else {
            userBusinessRoleService.updateById(binding);
        }
        List<String> permissionCodes = businessRoleService.getPermissionCodes(role.getId());
        userDataScopeService.rebuildUserScopes(apply.getUserId(), apply.getRegionCode(), apply.getBelongCountyAgentId(), permissionCodes, auditUserId);
        initStockAccountPlaceholder(apply.getUserId(), role.getRoleCode());
        initCommissionAccountPlaceholder(apply.getUserId(), role.getRoleCode());
        initPromotionCodePlaceholder(apply.getUserId(), role.getRoleCode());
        permissionCacheVersionService.refreshUserCacheVersion(apply.getUserId(), JkBizConstants.CACHE_CHANGE_USER_ROLE, "identity effective", auditUserId);

        JkAuditLog auditLog = new JkAuditLog();
        auditLog.setBusinessType(JkBizConstants.BUSINESS_TYPE_IDENTITY_APPLY);
        auditLog.setBusinessId(apply.getId());
        auditLog.setBusinessNo(apply.getApplyNo());
        auditLog.setRequestNo(apply.getRequestNo());
        auditLog.setAuditUserId(auditUserId);
        auditLog.setAuditUserName(auditUserName);
        auditLog.setAuditUserType("ADMIN");
        auditLog.setAuditAction(JkBizConstants.AUDIT_ACTION_PASS);
        auditLog.setBeforeStatus(JkBizConstants.AUDIT_STATUS_PENDING);
        auditLog.setAfterStatus(JkBizConstants.AUDIT_STATUS_EFFECTIVE);
        auditLog.setAuditRemark(auditRemark);
        auditLog.setOperateSource(JkBizConstants.OPERATE_SOURCE_ADMIN);
        auditLog.setStatus(true);
        auditLog.setIsDeleted(false);
        auditLog.setCreateUserId(auditUserId);
        auditLog.setUpdateUserId(auditUserId);
        auditLog.setCreateTime(DateUtil.date());
        auditLog.setUpdateTime(DateUtil.date());
        auditLogService.saveAuditLog(auditLog);
    }

    @Override
    public void initStockAccountPlaceholder(Long userId, String roleCode) {
        // 阶段一占位：未进入真实库存账户业务闭环
    }

    @Override
    public void initCommissionAccountPlaceholder(Long userId, String roleCode) {
        // 阶段一占位：未进入真实佣金账户业务闭环
    }

    @Override
    public void initPromotionCodePlaceholder(Long userId, String roleCode) {
        // 阶段一占位：未进入真实推广码业务闭环
    }
}
