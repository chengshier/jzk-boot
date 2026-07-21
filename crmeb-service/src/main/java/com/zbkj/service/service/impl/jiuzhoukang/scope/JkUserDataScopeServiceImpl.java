package com.zbkj.service.service.impl.jiuzhoukang.scope;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.jiuzhoukang.JkUserDataScope;
import com.zbkj.service.dao.jiuzhoukang.JkUserDataScopeDao;
import com.zbkj.service.service.jiuzhoukang.scope.JkUserDataScopeService;
import com.zbkj.service.service.jiuzhoukang.support.JkDataScopeSupport;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JkUserDataScopeServiceImpl extends ServiceImpl<JkUserDataScopeDao, JkUserDataScope> implements JkUserDataScopeService {

    @Override
    public List<JkUserDataScope> getByUserId(Long userId) {
        LambdaQueryWrapper<JkUserDataScope> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkUserDataScope::getUserId, userId);
        lqw.eq(JkUserDataScope::getIsDeleted, false);
        lqw.eq(JkUserDataScope::getEnabled, true);
        lqw.orderByAsc(JkUserDataScope::getId);
        return list(lqw);
    }

    @Override
    public void rebuildUserScopes(Long userId, String regionCode, Long countyAgentId, List<String> permissions, Long operatorId) {
        LambdaQueryWrapper<JkUserDataScope> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(JkUserDataScope::getUserId, userId);
        remove(deleteWrapper);
        List<JkUserDataScope> scopes = JkDataScopeSupport.buildInitialScopes(userId, regionCode, countyAgentId, permissions);
        scopes.forEach(scope -> {
            scope.setCreateUserId(operatorId);
            scope.setUpdateUserId(operatorId);
            scope.setCreateTime(DateUtil.date());
            scope.setUpdateTime(DateUtil.date());
            scope.setTenantId("000000");
        });
        saveBatch(scopes);
    }
}
