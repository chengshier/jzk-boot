package com.zbkj.service.service.jiuzhoukang.identity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkUserBusinessRoleSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkUserBusinessRoleResponse;

import java.util.List;

public interface JkUserBusinessRoleService extends IService<JkUserBusinessRole> {
    List<JkUserBusinessRole> getUserRoles(Long userId);
    List<JkUserBusinessRoleResponse> getAdminList(JkUserBusinessRoleSearchRequest request, PageParamRequest pageParamRequest);
    Boolean freeze(Long userBusinessRoleId, String reason);
    Boolean unfreeze(Long userBusinessRoleId, String reason);
    Boolean cancel(Long userBusinessRoleId, String reason);
}
