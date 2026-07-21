package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.response.jiuzhoukang.JkBusinessPermissionResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessPermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/admin/jk/business-permission")
@Api(tags = "九州康业务权限点管理")
public class JkBusinessPermissionController {

    @Autowired
    private JkBusinessPermissionService businessPermissionService;

    @PreAuthorize("hasAuthority('admin:jk:business:permission:list')")
    @ApiOperation("权限点列表")
    @GetMapping("/list")
    public CommonResult<List<JkBusinessPermissionResponse>> list() {
        return CommonResult.success(businessPermissionService.getList());
    }
}
