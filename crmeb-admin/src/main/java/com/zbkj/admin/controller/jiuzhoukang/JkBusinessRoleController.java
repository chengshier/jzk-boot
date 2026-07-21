package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.request.jiuzhoukang.JkBusinessRoleSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRoleStatusRequest;
import com.zbkj.common.response.jiuzhoukang.JkBusinessRoleResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/admin/jk/business-role")
@Api(tags = "九州康业务角色管理")
public class JkBusinessRoleController {

    @Autowired
    private JkBusinessRoleService businessRoleService;

    @PreAuthorize("hasAuthority('admin:jk:business:role:list')")
    @ApiOperation("业务角色列表")
    @GetMapping("/list")
    public CommonResult<List<JkBusinessRoleResponse>> list(@Validated JkBusinessRoleSearchRequest request) {
        return CommonResult.success(businessRoleService.getList(request));
    }

    @PreAuthorize("hasAuthority('admin:jk:business:role:update:status')")
    @ApiOperation("启用禁用业务角色")
    @PostMapping("/updateStatus")
    public CommonResult<Boolean> updateStatus(@RequestBody @Validated JkBusinessRoleStatusRequest request) {
        return CommonResult.success(businessRoleService.updateEnabled(request.getRoleId(), request.getEnabled()));
    }
}
