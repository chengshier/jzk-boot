package com.zbkj.service.service.impl.jiuzhoukang.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.exception.jiuzhoukang.JkBizException;
import com.zbkj.common.exception.jiuzhoukang.JkForbiddenException;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkUserDataScope;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.model.user.User;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.identity.JkIdentityApplyService;
import com.zbkj.service.service.jiuzhoukang.identity.JkUserBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.scope.JkUserDataScopeService;
import com.zbkj.service.service.jiuzhoukang.support.JkPermissionContextSupport;
import com.zbkj.service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 九州康业务上下文装配入口。
 * <p>后台菜单权限仍由 CRMEB Spring Security 负责；本类只负责业务身份、业务权限、区域和数据范围。
 * 任何需要区分普通用户、创客、合伙人、区县代或平台管理员的 Service，都应读取本上下文，不能自行拼角色判断。</p>
 */
@Service
public class JkUserContextServiceImpl implements JkUserContextService {

    @Autowired
    private FrontTokenComponent frontTokenComponent;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private JkUserBusinessRoleService userBusinessRoleService;
    @Autowired
    private JkBusinessRoleService businessRoleService;
    @Autowired
    private JkUserDataScopeService userDataScopeService;
    @Autowired
    private JkPermissionCacheVersionService permissionCacheVersionService;
    @Autowired
    private JkAdminActorService adminActorService;
    @Autowired
    private JkIdentityApplyService identityApplyService;
    @Autowired
    private UserService userService;

    @Override
    public JkUserContext getFrontContext(Long userId) {
        if (userId == null) {
            return getAnonymousContext();
        }
        JkUserContext cached = redisUtil.get(JkBizConstants.REDIS_CONTEXT_KEY_PREFIX + userId);
        Long currentVersion = permissionCacheVersionService.getUserCacheVersion(userId);
        if (cached != null && Objects.equals(cached.getCacheVersion(), currentVersion)) {
            return cached;
        }
        JkUserContext context = buildContext(userId);
        context.setCacheVersion(currentVersion);
        redisUtil.set(JkBizConstants.REDIS_CONTEXT_KEY_PREFIX + userId, context, 1800L);
        return context;
    }

    @Override
    public JkUserContext getAnonymousContext() {
        JkUserContext context = new JkUserContext();
        fillAnonymousContext(context, Collections.emptyMap(), Collections.emptyList());
        context.setUserId(0L);
        context.setCacheVersion(0L);
        return context;
    }

    @Override
    public JkUserContext getAdminContext() {
        SystemAdmin admin = adminActorService.getCurrentAdmin();
        if (adminActorService.isPlatformSuperAdmin(admin)) {
            JkUserContext context = new JkUserContext();
            context.setUserId(-1L);
            context.setPrimaryRoleCode("platform_admin");
            context.setPrimaryRoleName("平台管理员");
            context.setAuditStatus(JkBizConstants.AUDIT_STATUS_EFFECTIVE);
            context.setFreezeStatus(false);
            context.setRoles(Collections.singletonList("platform_admin"));
            context.setPermissions(Collections.singletonList("platform.all"));
            JkUserDataScope scope = new JkUserDataScope();
            scope.setScopeType(JkBizConstants.SCOPE_PLATFORM_ALL);
            context.setDataScopes(Collections.singletonList(scope));
            context.setCanApplyRoles(Collections.emptyList());
            context.setCacheVersion(0L);
            return context;
        }
        Long linkedUserId = adminActorService.getLinkedFrontUserId(admin);
        if (linkedUserId == null) {
            throw new JkBizException("当前后台管理员未配置九州康业务用户映射");
        }
        return getFrontContext(linkedUserId);
    }

    @Override
    public void assertHasPermission(String permissionCode, boolean checkDataScope) {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String uri = request.getRequestURI();
        JkUserContext context;
        if (uri.contains("/api/admin/")) {
            context = getAdminContext();
        } else {
            Integer currentUserId = frontTokenComponent.getUserId();
            context = currentUserId == null ? getAnonymousContext() : getFrontContext(Long.valueOf(currentUserId));
        }
        if (Boolean.TRUE.equals(context.getFreezeStatus()) || !JkBizConstants.AUDIT_STATUS_EFFECTIVE.equals(context.getAuditStatus())) {
            throw new JkForbiddenException(StrUtil.blankToDefault(context.getFreezeReason(), "当前身份未生效或已冻结"));
        }
        if (StrUtil.isNotBlank(permissionCode)
                && !context.getPermissions().contains(permissionCode)
                && !context.getPermissions().contains("platform.all")) {
            throw new JkForbiddenException("无业务权限");
        }
        if (checkDataScope && CollUtil.isEmpty(context.getDataScopes())) {
            throw new JkForbiddenException("无数据范围权限");
        }
    }

    private JkUserContext buildContext(Long userId) {
        JkUserContext context = new JkUserContext();
        context.setUserId(userId);
        User user = userService.getById(userId.intValue());
        // 已生效业务身份天然保留入口；普通用户必须由后台或固定入口码显式开通。
        context.setEntryAccess(user != null && Boolean.TRUE.equals(user.getJkEntryAccess()));
        List<JkUserBusinessRole> roleBindings = userBusinessRoleService.getUserRoles(userId);
        List<JkBusinessRole> enabledRoles = businessRoleService.getEnabledRoleList();
        Map<String, JkBusinessRole> enabledRoleMap = enabledRoles.stream()
                .collect(Collectors.toMap(JkBusinessRole::getRoleCode, item -> item, (a, b) -> a));
        if (CollUtil.isEmpty(roleBindings)) {
            fillAnonymousContext(context, enabledRoleMap, enabledRoles);
            return context;
        }

        List<JkUserBusinessRole> effectiveRoleBindings = roleBindings.stream()
                .filter(this::isEffectiveRoleBinding)
                .sorted(Comparator.comparing(JkUserBusinessRole::getIsPrimary).reversed().thenComparing(JkUserBusinessRole::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        List<String> permissions = resolveEffectivePermissions(effectiveRoleBindings, enabledRoleMap);
        context.setPermissions(permissions);
        context.setDataScopes(effectiveRoleBindings.isEmpty() ? Collections.emptyList() : userDataScopeService.getByUserId(userId));
        context.setCanApplyRoles(resolveCanApplyRoles(userId, enabledRoles, roleBindings));

        if (!effectiveRoleBindings.isEmpty()) {
            context.setEntryAccess(true);
            JkUserBusinessRole primary = effectiveRoleBindings.get(0);
            context.setPrimaryRoleCode(primary.getRoleCode());
            context.setPrimaryRoleName(roleName(primary.getRoleCode(), enabledRoleMap));
            context.setAuditStatus(primary.getAuditStatus());
            context.setFreezeStatus(primary.getFreezeStatus());
            context.setFreezeReason(primary.getFreezeReason());
            context.setRegionCode(primary.getRegionCode());
            context.setBelongCountyAgentId(primary.getBelongCountyAgentId());
            context.setRoles(effectiveRoleBindings.stream().map(JkUserBusinessRole::getRoleCode).distinct().collect(Collectors.toList()));
            JkPermissionContextSupport.IdentityVisualState visualState =
                    JkPermissionContextSupport.resolveIdentityVisualState(context.getPrimaryRoleName(), context.getAuditStatus(),
                            Boolean.TRUE.equals(context.getFreezeStatus()), resolveDisableReason(userId, primary));
            context.setFreezeReason(visualState.getDisableReason());
            return context;
        }

        JkUserBusinessRole latestFrozen = roleBindings.stream()
                .filter(item -> JkBizConstants.AUDIT_STATUS_FROZEN.equals(item.getAuditStatus()) || Boolean.TRUE.equals(item.getFreezeStatus()))
                .sorted(Comparator.comparing(JkUserBusinessRole::getIsPrimary).reversed().thenComparing(JkUserBusinessRole::getId, Comparator.reverseOrder()))
                .findFirst().orElse(null);
        if (latestFrozen != null) {
            context.setPrimaryRoleCode(JkBizConstants.ROLE_NORMAL_USER);
            context.setPrimaryRoleName(roleName(JkBizConstants.ROLE_NORMAL_USER, enabledRoleMap));
            context.setAuditStatus(JkBizConstants.AUDIT_STATUS_FROZEN);
            context.setFreezeStatus(true);
            context.setFreezeReason(latestFrozen.getFreezeReason());
            context.setRegionCode(latestFrozen.getRegionCode());
            context.setBelongCountyAgentId(latestFrozen.getBelongCountyAgentId());
            context.setRoles(Collections.singletonList(JkBizConstants.ROLE_NORMAL_USER));
            return context;
        }

        fillAnonymousContext(context, enabledRoleMap, enabledRoles);
        context.setCanApplyRoles(resolveCanApplyRoles(userId, enabledRoles, roleBindings));
        context.setFreezeReason(resolveDisableReason(userId, roleBindings.stream()
                .sorted(Comparator.comparing(JkUserBusinessRole::getIsPrimary).reversed().thenComparing(JkUserBusinessRole::getId, Comparator.reverseOrder()))
                .findFirst().orElse(null)));
        return context;
    }

    private List<String> resolveEffectivePermissions(List<JkUserBusinessRole> effectiveRoleBindings, Map<String, JkBusinessRole> enabledRoleMap) {
        List<String> permissions = new ArrayList<>();
        for (JkUserBusinessRole roleBinding : effectiveRoleBindings) {
            JkBusinessRole role = enabledRoleMap.get(roleBinding.getRoleCode());
            if (role != null) {
                permissions.addAll(businessRoleService.getPermissionCodes(role.getId()));
            }
        }
        List<String> result = permissions.stream().distinct().collect(Collectors.toList());
        if (!result.contains(JkBizPermissionCodes.PRODUCT_TRADE_VIEW)) {
            result.add(JkBizPermissionCodes.PRODUCT_TRADE_VIEW);
        }
        return result;
    }

    private boolean isEffectiveRoleBinding(JkUserBusinessRole roleBinding) {
        return JkBizConstants.AUDIT_STATUS_EFFECTIVE.equals(roleBinding.getAuditStatus())
                && !Boolean.TRUE.equals(roleBinding.getFreezeStatus())
                && JkBizConstants.EFFECTIVE_STATUS_ENABLED.equals(roleBinding.getEffectiveStatus());
    }

    private void fillAnonymousContext(JkUserContext context, Map<String, JkBusinessRole> enabledRoleMap, List<JkBusinessRole> enabledRoles) {
        context.setPrimaryRoleCode(JkBizConstants.ROLE_NORMAL_USER);
        context.setPrimaryRoleName(roleName(JkBizConstants.ROLE_NORMAL_USER, enabledRoleMap));
        context.setAuditStatus(JkBizConstants.AUDIT_STATUS_EFFECTIVE);
        context.setFreezeStatus(false);
        context.setRoles(Collections.singletonList(JkBizConstants.ROLE_NORMAL_USER));
        List<String> permissions = new ArrayList<>();
        JkBusinessRole normalUser = enabledRoleMap.get(JkBizConstants.ROLE_NORMAL_USER);
        if (normalUser != null) {
            permissions.addAll(businessRoleService.getPermissionCodes(normalUser.getId()));
        }
        if (!permissions.contains("identity.apply.submit")) {
            permissions.add("identity.apply.submit");
        }
        if (!permissions.contains(JkBizPermissionCodes.PRODUCT_TRADE_VIEW)) {
            permissions.add(JkBizPermissionCodes.PRODUCT_TRADE_VIEW);
        }
        context.setPermissions(permissions);
        // 已登录普通用户（无业务角色绑定，走匿名兜底）也应有 SELF 数据范围，否则会被 @JkBizPermission(checkDataScope=true) 拦在"无数据范围权限"。
        // 不落库：SELF 是登录用户的天然权利，随上下文缓存即可；匿名用户（userId<=0）保持空列表。
        Long uid = context.getUserId();
        if (uid != null && uid > 0) {
            JkUserDataScope selfScope = new JkUserDataScope();
            selfScope.setUserId(uid);
            selfScope.setScopeType(JkBizConstants.SCOPE_SELF);
            selfScope.setEnabled(true);
            selfScope.setStatus(true);
            selfScope.setIsDeleted(false);
            context.setDataScopes(Collections.singletonList(selfScope));
        } else {
            context.setDataScopes(Collections.emptyList());
        }
        context.setCanApplyRoles(JkPermissionContextSupport.resolveFrontApplyRoleCodes(enabledRoles));
        JkPermissionContextSupport.IdentityVisualState visualState =
                JkPermissionContextSupport.resolveIdentityVisualState("普通用户", JkBizConstants.AUDIT_STATUS_EFFECTIVE, false, null);
        context.setFreezeReason(visualState.getDisableReason());
    }

    private List<String> resolveCanApplyRoles(Long userId, List<JkBusinessRole> enabledRoles, List<JkUserBusinessRole> roleBindings) {
        List<String> enabledApplyRoles = JkPermissionContextSupport.resolveFrontApplyRoleCodes(enabledRoles).stream()
                .filter(item -> !JkBizConstants.ROLE_NORMAL_USER.equals(item))
                .collect(Collectors.toList());
        List<String> ownedRoles = roleBindings.stream()
                .filter(item -> JkBizConstants.AUDIT_STATUS_EFFECTIVE.equals(item.getAuditStatus()) || JkBizConstants.AUDIT_STATUS_FROZEN.equals(item.getAuditStatus()))
                .map(JkUserBusinessRole::getRoleCode)
                .collect(Collectors.toList());
        LambdaQueryWrapper<JkIdentityApply> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkIdentityApply::getUserId, userId);
        lqw.eq(JkIdentityApply::getIsDeleted, false);
        lqw.eq(JkIdentityApply::getAuditStatus, JkBizConstants.AUDIT_STATUS_PENDING);
        List<String> pendingRoles = identityApplyService.list(lqw).stream().map(JkIdentityApply::getApplyRoleCode).collect(Collectors.toList());
        return JkPermissionContextSupport.resolveCanApplyRoles(enabledApplyRoles, ownedRoles, pendingRoles);
    }

    private String resolveDisableReason(Long userId, JkUserBusinessRole primary) {
        if (primary != null && StrUtil.isNotBlank(primary.getFreezeReason())) {
            return primary.getFreezeReason();
        }
        LambdaQueryWrapper<JkIdentityApply> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkIdentityApply::getUserId, userId);
        lqw.orderByDesc(JkIdentityApply::getId);
        lqw.last(" limit 1");
        JkIdentityApply latestApply = identityApplyService.getOne(lqw);
        return latestApply == null ? null : latestApply.getRejectReason();
    }

    private String roleName(String roleCode, Map<String, JkBusinessRole> enabledRoleMap) {
        return ObjectUtil.isNotNull(enabledRoleMap.get(roleCode)) ? enabledRoleMap.get(roleCode).getRoleName() : roleCode;
    }
}
