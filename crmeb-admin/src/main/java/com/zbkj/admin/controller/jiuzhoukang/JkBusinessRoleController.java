package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkBusinessRoleResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("api/admin/jk/business-role")
@Api(tags = "九州康业务角色管理")
public class JkBusinessRoleController {
    @Autowired private JkBusinessRoleService businessRoleService;
    @Autowired private JkAdminActorService adminActorService;

    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_BUSINESS_ROLE_LIST +"')")
    @ApiOperation("业务角色列表") @GetMapping("/list")
    public CommonResult<List<JkBusinessRoleResponse>> list(@Validated JkBusinessRoleSearchRequest request){return CommonResult.success(businessRoleService.getList(request));}

    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_BUSINESS_ROLE_SAVE +"')")
    @ApiOperation("新增或编辑业务角色") @PostMapping("/save")
    public CommonResult<JkBusinessRole> save(@RequestBody @Validated JkBusinessRoleSaveRequest request){return CommonResult.success(businessRoleService.saveRole(request,operator()));}

    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_BUSINESS_ROLE_PERMISSION +"')")
    @ApiOperation("配置业务角色权限") @PostMapping("/assign-permissions")
    public CommonResult<Boolean> assign(@RequestBody @Validated JkRolePermissionAssignRequest request){return CommonResult.success(businessRoleService.assignPermissions(request.getRoleId(),request.getPermissionCodes(),operator()));}

    @PreAuthorize("hasAuthority('"+ JkPermissionCodes.ADMIN_BUSINESS_ROLE_STATUS +"')")
    @ApiOperation("启用禁用业务角色") @PostMapping("/updateStatus")
    public CommonResult<Boolean> updateStatus(@RequestBody @Validated JkBusinessRoleStatusRequest request){return CommonResult.success(businessRoleService.updateEnabled(request.getRoleId(),request.getEnabled()));}

    private Long operator(){Long id=adminActorService.getLinkedFrontUserId(adminActorService.getCurrentAdmin());return id==null?-Long.valueOf(adminActorService.getCurrentAdmin().getId()):id;}
}
