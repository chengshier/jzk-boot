package com.zbkj.service.service.jiuzhoukang.scope;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkUserDataScope;

import java.util.List;

public interface JkUserDataScopeService extends IService<JkUserDataScope> {
    List<JkUserDataScope> getByUserId(Long userId);
    void rebuildUserScopes(Long userId, String regionCode, Long countyAgentId, java.util.List<String> permissions, Long operatorId);
}
