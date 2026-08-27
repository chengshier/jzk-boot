package com.zbkj.service.service.jiuzhoukang.order;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zbkj.common.model.jiuzhoukang.JkRegionAgent;
import com.zbkj.common.model.user.User;
import com.zbkj.common.response.jiuzhoukang.JkOptionResponse;
import com.zbkj.service.dao.jiuzhoukang.JkRegionAgentDao;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.impl.jiuzhoukang.order.JkRetailAttributionAdminServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

public class JkRetailAttributionAdminServiceImplTest {

    @Test
    public void listsActiveCountyAgentsBoundToTheSelectedRegion() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), JkRegionAgent.class);
        JkRetailAttributionAdminServiceImpl service = new JkRetailAttributionAdminServiceImpl();
        ReflectionTestUtils.setField(service, "regionAgentDao", proxy(JkRegionAgentDao.class, (method, args) ->
                Arrays.asList(new JkRegionAgent().setRegionCode("350203").setCountyAgentUserId(18L))));
        ReflectionTestUtils.setField(service, "userService", proxy(UserService.class, (method, args) ->
                Arrays.asList(new User().setUid(18).setRealName("张三").setPhone("13800000000"))));

        List<JkOptionResponse> options = service.listCountyAgentOptions("350203", "张");

        Assert.assertEquals(1, options.size());
        Assert.assertEquals("18", options.get(0).getValue());
        Assert.assertEquals("张三 / 13800000000（ID:18）", options.get(0).getLabel());
    }

    private <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return null;
            return invocation.apply(method, args);
        }));
    }

    private interface Invocation {
        Object apply(Method method, Object[] args) throws Throwable;
    }
}
