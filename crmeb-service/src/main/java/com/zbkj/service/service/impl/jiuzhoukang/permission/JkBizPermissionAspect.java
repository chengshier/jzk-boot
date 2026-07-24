package com.zbkj.service.service.impl.jiuzhoukang.permission;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 九州康业务权限切面。
 * <p>它是 CRMEB 登录认证之后的第二层业务校验，只判断业务权限和基础数据范围。
 * 涉及具体用户、区域或业务单据的行级权限，仍必须由对应 Service 按目标数据再次校验。</p>
 */
@Aspect
@Component
public class JkBizPermissionAspect {

    @Autowired
    private JkUserContextService userContextService;

    @Around("@annotation(jkBizPermission)")
    public Object around(ProceedingJoinPoint pjp, JkBizPermission jkBizPermission) throws Throwable {
        userContextService.assertHasPermission(jkBizPermission.value(), jkBizPermission.checkDataScope());
        return pjp.proceed();
    }
}
