package com.zbkj.service.service.jiuzhoukang.permission;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRoleSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkBusinessRoleResponse;

import java.util.List;

public interface JkBusinessRoleService extends IService<JkBusinessRole> {
    List<JkBusinessRoleResponse> getList(JkBusinessRoleSearchRequest request);
    List<JkBusinessRole> getEnabledRoleList();
    List<String> getPermissionCodes(Long roleId);
    Boolean updateEnabled(Long roleId, Boolean enabled);
}
