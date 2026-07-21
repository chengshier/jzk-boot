package com.zbkj.service.service.jiuzhoukang.permission;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkBusinessPermission;
import com.zbkj.common.response.jiuzhoukang.JkBusinessPermissionResponse;

import java.util.List;

public interface JkBusinessPermissionService extends IService<JkBusinessPermission> {
    List<JkBusinessPermissionResponse> getList();
}
