package com.zbkj.service.service.impl.jiuzhoukang.scope;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.jiuzhoukang.JkForbiddenException;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkUserDataScope;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserDataScopeDao;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.scope.JkAdminDataScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JkAdminDataScopeServiceImpl implements JkAdminDataScopeService {

    @Autowired
    private JkAdminActorService adminActorService;
    @Autowired
    private JkUserBusinessRoleDao userBusinessRoleDao;
    @Autowired
    private JkUserDataScopeDao userDataScopeDao;

    @Override
    public void applyIdentityApplyScope(LambdaQueryWrapper<JkIdentityApply> wrapper) {
        Scope scope = resolveScope();
        if (scope.platformAll) return;
        if (!scope.countyAgent) {
            wrapper.eq(JkIdentityApply::getId, -1L);
            return;
        }
        wrapper.ne(JkIdentityApply::getApplyRoleCode, JkBizConstants.ROLE_COUNTY_AGENT);
        if (StrUtil.isNotBlank(scope.regionCode)) {
            wrapper.and(q -> q.eq(JkIdentityApply::getBelongCountyAgentId, scope.userId)
                    .or().eq(JkIdentityApply::getRegionCode, scope.regionCode));
        } else {
            wrapper.eq(JkIdentityApply::getBelongCountyAgentId, scope.userId);
        }
    }

    @Override
    public void assertCanManageIdentityApply(JkIdentityApply apply) {
        if (apply == null) throw new JkForbiddenException("申请记录不存在或无权访问");
        Scope scope = resolveScope();
        if (scope.platformAll) return;
        boolean inScope = scope.countyAgent
                && !JkBizConstants.ROLE_COUNTY_AGENT.equals(apply.getApplyRoleCode())
                && (scope.userId.equals(apply.getBelongCountyAgentId())
                || (StrUtil.isNotBlank(scope.regionCode) && scope.regionCode.equals(apply.getRegionCode())));
        if (!inScope) throw new JkForbiddenException("无权审核该身份申请");
    }

    @Override
    public void applyUserBusinessRoleScope(LambdaQueryWrapper<JkUserBusinessRole> wrapper) {
        Scope scope = resolveScope();
        if (scope.platformAll) return;
        if (!scope.countyAgent) {
            wrapper.eq(JkUserBusinessRole::getId, -1L);
            return;
        }
        wrapper.ne(JkUserBusinessRole::getRoleCode, JkBizConstants.ROLE_COUNTY_AGENT);
        if (StrUtil.isNotBlank(scope.regionCode)) {
            wrapper.and(q -> q.eq(JkUserBusinessRole::getBelongCountyAgentId, scope.userId)
                    .or().eq(JkUserBusinessRole::getRegionCode, scope.regionCode));
        } else {
            wrapper.eq(JkUserBusinessRole::getBelongCountyAgentId, scope.userId);
        }
    }

    @Override
    public void assertCanManageUserBusinessRole(JkUserBusinessRole role) {
        if (role == null) throw new JkForbiddenException("用户业务身份不存在或无权访问");
        Scope scope = resolveScope();
        if (scope.platformAll) return;
        boolean inScope = scope.countyAgent
                && !JkBizConstants.ROLE_COUNTY_AGENT.equals(role.getRoleCode())
                && (scope.userId.equals(role.getBelongCountyAgentId())
                || (StrUtil.isNotBlank(scope.regionCode) && scope.regionCode.equals(role.getRegionCode())));
        if (!inScope) throw new JkForbiddenException("无权操作该业务身份");
    }

    @Override
    public void applyAuditLogScope(LambdaQueryWrapper<JkAuditLog> wrapper) {
        Scope scope = resolveScope();
        if (scope.platformAll) return;
        // 审核日志跨多种业务，非平台账号先按“本人操作记录”收紧，避免无业务关联表时泄漏全局日志。
        wrapper.eq(JkAuditLog::getAuditUserId, scope.adminId);
    }

    private Scope resolveScope() {
        SystemAdmin admin = adminActorService.getCurrentAdmin();
        if (adminActorService.isPlatformSuperAdmin(admin)) {
            return Scope.platform(Long.valueOf(admin.getId()));
        }
        Long userId = adminActorService.getLinkedFrontUserId(admin);
        if (userId == null) throw new JkForbiddenException("当前后台管理员未配置九州康业务用户映射");
        List<JkUserDataScope> scopes = userDataScopeDao.selectList(new LambdaQueryWrapper<JkUserDataScope>()
                .eq(JkUserDataScope::getUserId, userId)
                .eq(JkUserDataScope::getEnabled, true)
                .eq(JkUserDataScope::getIsDeleted, false));
        for (JkUserDataScope item : scopes) {
            if (JkBizConstants.SCOPE_PLATFORM_ALL.equals(item.getScopeType())) {
                return Scope.platform(Long.valueOf(admin.getId()));
            }
        }
        JkUserBusinessRole primary = userBusinessRoleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, userId)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getEffectiveStatus, JkBizConstants.EFFECTIVE_STATUS_ENABLED)
                .eq(JkUserBusinessRole::getFreezeStatus, false)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId)
                .last(" limit 1"));
        if (primary == null) throw new JkForbiddenException("当前后台映射用户没有生效业务身份");
        return new Scope(Long.valueOf(admin.getId()), userId, primary.getRegionCode(),
                JkBizConstants.ROLE_COUNTY_AGENT.equals(primary.getRoleCode()), false);
    }

    private static final class Scope {
        private final Long adminId;
        private final Long userId;
        private final String regionCode;
        private final boolean countyAgent;
        private final boolean platformAll;

        private Scope(Long adminId, Long userId, String regionCode, boolean countyAgent, boolean platformAll) {
            this.adminId = adminId;
            this.userId = userId;
            this.regionCode = regionCode;
            this.countyAgent = countyAgent;
            this.platformAll = platformAll;
        }

        private static Scope platform(Long adminId) {
            return new Scope(adminId, -1L, null, false, true);
        }
    }
}
