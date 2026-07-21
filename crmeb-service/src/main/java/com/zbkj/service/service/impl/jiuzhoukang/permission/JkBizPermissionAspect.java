package com.zbkj.service.service.impl.jiuzhoukang.permission;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
