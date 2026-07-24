package com.zbkj.service.service.jiuzhoukang.permission;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkBusinessPermission;
import com.zbkj.common.response.jiuzhoukang.JkBusinessPermissionResponse;
import com.zbkj.common.request.jiuzhoukang.JkBusinessPermissionSaveRequest;

import java.util.List;

public interface JkBusinessPermissionService extends IService<JkBusinessPermission> {
    List<JkBusinessPermissionResponse> getList();
    JkBusinessPermission savePermission(JkBusinessPermissionSaveRequest request, Long operatorId);
    Boolean updateEnabled(Long permissionId, Boolean enabled, Long operatorId);
}
