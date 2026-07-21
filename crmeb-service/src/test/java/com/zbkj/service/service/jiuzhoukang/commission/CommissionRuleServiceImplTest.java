package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRuleItem;
import com.zbkj.service.service.impl.jiuzhoukang.commission.CommissionRuleServiceImpl;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

public class CommissionRuleServiceImplTest {

    @Test
    public void enrichesRuleAndItemDisplayTexts() {
        CommissionRuleServiceImpl service = new CommissionRuleServiceImpl();
        ReflectionTestUtils.setField(service, "businessRoleService", proxy(JkBusinessRoleService.class, (method, args) -> {
            if ("getEnabledRoleList".equals(method.getName())) {
                return Arrays.asList(
                        new JkBusinessRole().setRoleCode("maker").setRoleName("创客"),
                        new JkBusinessRole().setRoleCode("partner").setRoleName("合伙人")
                );
            }
            return Collections.emptyList();
        }));

        JkCommissionRule rule = new JkCommissionRule()
                .setSourceType("RETAIL_ORDER")
                .setReceiverRoleCode("maker")
                .setStatus(true);
        JkCommissionRuleItem item = new JkCommissionRuleItem()
                .setReceiverRoleCode("partner")
                .setCalculationType("PERCENT")
                .setStatus(false);

        ReflectionTestUtils.invokeMethod(service, "enrichRuleDisplays", Collections.singletonList(rule));
        ReflectionTestUtils.invokeMethod(service, "enrichRuleItemDisplays", Collections.singletonList(item));

        Assert.assertEquals("零售订单", rule.getSourceTypeText());
        Assert.assertEquals("创客", rule.getReceiverRoleName());
        Assert.assertEquals("启用", rule.getStatusText());
        Assert.assertEquals("success", rule.getStatusTag());

        Assert.assertEquals("合伙人", item.getReceiverRoleName());
        Assert.assertEquals("比例", item.getCalculationTypeText());
        Assert.assertEquals("禁用", item.getStatusText());
        Assert.assertEquals("info", item.getStatusTag());
    }

    private <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) {
                    return type.getSimpleName() + "Proxy";
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
            }
            return invocation.apply(method, args);
        }));
    }

    private interface Invocation {
        Object apply(Method method, Object[] args) throws Throwable;
    }
}
