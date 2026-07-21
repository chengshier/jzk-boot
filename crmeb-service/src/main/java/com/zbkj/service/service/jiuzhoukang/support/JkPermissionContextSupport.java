package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JkPermissionContextSupport {

    public static List<String> resolveMenus(List<String> permissions) {
        Set<String> menus = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (permission == null) {
                continue;
            }
            if (permission.startsWith("promotion.")) {
                menus.add("promotion");
            } else if (permission.startsWith("team.")) {
                menus.add("team");
            } else if (permission.startsWith("stock.")) {
                menus.add("stock");
            } else if (permission.startsWith("commission.")) {
                menus.add("commission");
            } else if (permission.startsWith("identity.")) {
                menus.add("identity");
            }
        }
        return new ArrayList<>(menus);
    }

    public static IdentityVisualState resolveIdentityVisualState(String primaryRoleName, String auditStatus, boolean frozen, String reason) {
        IdentityVisualState state = new IdentityVisualState();
        String roleName = primaryRoleName == null ? "当前" : primaryRoleName;
        if (frozen) {
            state.setIdentityStatusText(roleName + "身份已冻结");
            state.setDisableReason(reason);
            return state;
        }
        if ("EFFECTIVE".equals(auditStatus)) {
            state.setIdentityStatusText(roleName + "身份已生效");
            return state;
        }
        if ("PENDING".equals(auditStatus)) {
            state.setIdentityStatusText(roleName + "身份审核中");
            return state;
        }
        if ("REJECTED".equals(auditStatus)) {
            state.setIdentityStatusText(roleName + "身份已驳回");
            state.setDisableReason(reason);
            return state;
        }
        state.setIdentityStatusText("暂无生效身份");
        state.setDisableReason(reason);
        return state;
    }

    public static List<String> resolveCanApplyRoles(List<String> enabledRoleCodes, List<String> ownedRoleCodes, List<String> pendingRoleCodes) {
        Set<String> owned = new LinkedHashSet<>(ownedRoleCodes);
        Set<String> pending = new LinkedHashSet<>(pendingRoleCodes);
        List<String> result = new ArrayList<>();
        for (String roleCode : enabledRoleCodes) {
            if (owned.contains(roleCode) || pending.contains(roleCode)) {
                continue;
            }
            result.add(roleCode);
        }
        return result;
    }

    public static List<String> resolveFrontApplyRoleCodes(List<JkBusinessRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (JkBusinessRole role : roles) {
            if (role == null || role.getRoleCode() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(role.getEnabled()) && Boolean.TRUE.equals(role.getAllowFrontApply())) {
                result.add(role.getRoleCode());
            }
        }
        return result;
    }

    public static String scopeTypeText(String scopeType) {
        if (scopeType == null || scopeType.trim().isEmpty()) {
            return "未设置";
        }
        if ("PLATFORM".equals(scopeType)) return "平台范围";
        if ("PLATFORM_ALL".equals(scopeType)) return "平台全部数据";
        if ("PROVINCE".equals(scopeType)) return "省级范围";
        if ("CITY".equals(scopeType)) return "市级范围";
        if ("COUNTY".equals(scopeType)) return "区县范围";
        if ("REGION_SELF".equals(scopeType)) return "所属区域";
        if ("SELF".equals(scopeType)) return "个人范围";
        if ("DIRECT_TEAM".equals(scopeType)) return "直属团队";
        if ("TEAM".equals(scopeType)) return "团队范围";
        if ("PERSONAL".equals(scopeType)) return "个人范围";
        return scopeType;
    }

    public static class IdentityVisualState {
        private String identityStatusText;
        private String disableReason;

        public String getIdentityStatusText() {
            return identityStatusText;
        }

        public void setIdentityStatusText(String identityStatusText) {
            this.identityStatusText = identityStatusText;
        }

        public String getDisableReason() {
            return disableReason;
        }

        public void setDisableReason(String disableReason) {
            this.disableReason = disableReason;
        }
    }
}
