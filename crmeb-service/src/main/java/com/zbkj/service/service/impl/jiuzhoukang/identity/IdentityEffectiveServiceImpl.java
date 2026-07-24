package com.zbkj.service.service.impl.jiuzhoukang.identity;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.user.User;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.FundAccountService;
import com.zbkj.service.service.jiuzhoukang.stock.StockAccountService;
import com.zbkj.service.service.jiuzhoukang.identity.IdentityEffectiveService;
import com.zbkj.service.service.jiuzhoukang.identity.JkUserBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.scope.JkUserDataScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 身份审核通过后的统一生效编排。
 * <p>角色绑定、数据范围、库存/佣金/资金账户初始化和权限缓存刷新必须处于同一事务，
 * 避免出现“页面显示身份已生效，但账户或权限尚未建立”的半完成状态。</p>
 */
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
    @Autowired
    private StockAccountService stockAccountService;
    @Autowired
    private CommissionAccountService commissionAccountService;
    @Autowired
    private FundAccountService fundAccountService;
    @Autowired
    private UserService userService;

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
        initBusinessAccounts(apply.getUserId(), role.getRoleCode(), apply.getRegionCode());
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

    private void initBusinessAccounts(Long userId, String roleCode, String regionCode) {
        User user = userService.getById(userId.intValue());
        String ownerName = user == null ? null : (user.getRealName() == null || user.getRealName().trim().isEmpty() ? user.getNickname() : user.getRealName());
        stockAccountService.initializeBusinessAccount(userId, roleCode, regionCode, ownerName);
        commissionAccountService.initialize(userId, roleCode, regionCode);
        fundAccountService.initialize(userId, roleCode, regionCode);
    }

}
