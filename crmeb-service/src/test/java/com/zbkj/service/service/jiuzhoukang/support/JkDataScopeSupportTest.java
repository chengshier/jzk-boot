package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.model.jiuzhoukang.JkUserDataScope;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JkDataScopeSupportTest {

    @Test
    public void should_generate_base_and_team_scopes_from_permissions() {
        List<JkUserDataScope> scopes = JkDataScopeSupport.buildInitialScopes(
                10001L,
                "350203",
                30001L,
                Arrays.asList("team.view.direct", "team.view.team", "stock.view.self")
        );

        List<String> scopeTypes = scopes.stream().map(JkUserDataScope::getScopeType).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("SELF", "DIRECT_TEAM", "TEAM"), scopeTypes);
    }

    @Test
    public void should_generate_region_scope_when_region_permission_exists() {
        List<JkUserDataScope> scopes = JkDataScopeSupport.buildInitialScopes(
                10001L,
                "350203",
                30001L,
                Arrays.asList("team.view.region", "stock.view.region")
        );

        Assert.assertEquals(2, scopes.size());
        Assert.assertEquals("SELF", scopes.get(0).getScopeType());
        Assert.assertEquals("REGION_SELF", scopes.get(1).getScopeType());
        Assert.assertEquals("350203", scopes.get(1).getRegionCode());
        Assert.assertEquals(Long.valueOf(30001L), scopes.get(1).getCountyAgentId());
    }
}
