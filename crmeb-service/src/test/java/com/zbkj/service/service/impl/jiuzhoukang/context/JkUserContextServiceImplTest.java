package com.zbkj.service.service.impl.jiuzhoukang.context;

import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JkUserContextServiceImplTest {

    @Test
    public void normalUserUsesPermissionsConfiguredForNormalUserRole() throws Exception {
        JkBusinessRole normalUser = new JkBusinessRole().setId(1L).setRoleCode(JkBizConstants.ROLE_NORMAL_USER);
        JkUserContextServiceImpl service = new JkUserContextServiceImpl();
        JkBusinessRoleService roles = (JkBusinessRoleService) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{JkBusinessRoleService.class},
                (proxy, method, args) -> "getPermissionCodes".equals(method.getName())
                        ? Collections.singletonList(JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF) : null);
        Field roleService = JkUserContextServiceImpl.class.getDeclaredField("businessRoleService");
        roleService.setAccessible(true);
        roleService.set(service, roles);
        Method fill = JkUserContextServiceImpl.class.getDeclaredMethod("fillAnonymousContext", JkUserContext.class, Map.class, java.util.List.class);
        fill.setAccessible(true);

        JkUserContext context = new JkUserContext();
        Map<String, JkBusinessRole> rolesByCode = new HashMap<>();
        rolesByCode.put(JkBizConstants.ROLE_NORMAL_USER, normalUser);
        fill.invoke(service, context, rolesByCode, Collections.singletonList(normalUser));

        Assert.assertTrue(context.getPermissions().contains(JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF));
        Assert.assertTrue(context.getPermissions().contains(JkBizPermissionCodes.PRODUCT_TRADE_VIEW));
    }
}
