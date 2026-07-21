package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkUserDataScope;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JkDataScopeSupport {

    public static List<JkUserDataScope> buildInitialScopes(Long userId, String regionCode, Long countyAgentId, List<String> permissions) {
        List<JkUserDataScope> scopes = new ArrayList<>();
        scopes.add(baseScope(userId, JkBizConstants.SCOPE_SELF, regionCode, countyAgentId));
        Set<String> scopeTypes = new LinkedHashSet<>();
        for (String permission : permissions) {
            if ("team.view.direct".equals(permission)) {
                scopeTypes.add(JkBizConstants.SCOPE_DIRECT_TEAM);
            }
            if ("team.view.team".equals(permission)) {
                scopeTypes.add(JkBizConstants.SCOPE_TEAM);
            }
            if ("team.view.region".equals(permission) || "stock.view.region".equals(permission)) {
                scopeTypes.add(JkBizConstants.SCOPE_REGION_SELF);
            }
        }
        for (String scopeType : scopeTypes) {
            scopes.add(baseScope(userId, scopeType, regionCode, countyAgentId));
        }
        return scopes;
    }

    private static JkUserDataScope baseScope(Long userId, String scopeType, String regionCode, Long countyAgentId) {
        JkUserDataScope scope = new JkUserDataScope();
        scope.setUserId(userId);
        scope.setScopeType(scopeType);
        scope.setRegionCode(regionCode);
        scope.setCountyAgentId(countyAgentId);
        scope.setEnabled(true);
        scope.setStatus(true);
        scope.setIsDeleted(false);
        return scope;
    }
}
