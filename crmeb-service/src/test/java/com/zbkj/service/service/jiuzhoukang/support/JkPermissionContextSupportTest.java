package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JkPermissionContextSupportTest {

    @Test
    public void should_resolve_menus_from_permission_prefixes() {
        List<String> menus = JkPermissionContextSupport.resolveMenus(Arrays.asList(
                "promotion.qrcode.generate",
                "team.view.direct",
                "stock.apply",
                "commission.view",
                "unknown.permission"
        ));

        Assert.assertEquals(Arrays.asList("promotion", "team", "stock", "commission"), menus);
    }

    @Test
    public void should_build_frozen_identity_copy_and_disable_reason() {
        JkPermissionContextSupport.IdentityVisualState state =
                JkPermissionContextSupport.resolveIdentityVisualState("创客", "EFFECTIVE", true, "平台冻结测试");

        Assert.assertEquals("创客身份已冻结", state.getIdentityStatusText());
        Assert.assertEquals("平台冻结测试", state.getDisableReason());
    }

    @Test
    public void should_filter_can_apply_roles_from_owned_and_pending_roles() {
        List<String> canApplyRoles = JkPermissionContextSupport.resolveCanApplyRoles(
                Arrays.asList("maker", "partner", "county_agent"),
                Collections.singletonList("maker"),
                Collections.singletonList("partner")
        );

        Assert.assertEquals(Collections.singletonList("county_agent"), canApplyRoles);
    }

    @Test
    public void should_only_return_enabled_front_apply_roles() {
        JkBusinessRole maker = new JkBusinessRole().setRoleCode("maker").setEnabled(true).setAllowFrontApply(true);
        JkBusinessRole countyAgent = new JkBusinessRole().setRoleCode("county_agent").setEnabled(true).setAllowFrontApply(true);
        JkBusinessRole healthAdvisor = new JkBusinessRole().setRoleCode("health_advisor").setEnabled(true).setAllowFrontApply(false);
        JkBusinessRole cityAgent = new JkBusinessRole().setRoleCode("city_agent").setEnabled(false).setAllowFrontApply(false);

        List<String> canApplyRoles = JkPermissionContextSupport.resolveFrontApplyRoleCodes(
                Arrays.asList(maker, countyAgent, healthAdvisor, cityAgent)
        );

        Assert.assertEquals(Arrays.asList("maker", "county_agent"), canApplyRoles);
    }
}
