package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkBusinessPermission;
import com.zbkj.common.request.jiuzhoukang.JkBusinessPermissionSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkBusinessPermissionResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessPermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("api/admin/jk/business-permission")
@Api(tags = "九州康业务权限点管理")
public class JkBusinessPermissionController {
    @Autowired private JkBusinessPermissionService businessPermissionService;
    @Autowired private JkAdminActorService adminActorService;

    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_BUSINESS_PERMISSION_LIST+"')")
    @ApiOperation("权限点列表") @GetMapping("/list")
    public CommonResult<List<JkBusinessPermissionResponse>> list(){return CommonResult.success(businessPermissionService.getList());}

    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_BUSINESS_PERMISSION_SAVE+"')")
    @ApiOperation("新增或编辑权限点") @PostMapping("/save")
    public CommonResult<JkBusinessPermission> save(@RequestBody @Validated JkBusinessPermissionSaveRequest request){return CommonResult.success(businessPermissionService.savePermission(request,operator()));}

    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_BUSINESS_PERMISSION_STATUS+"')")
    @ApiOperation("启用禁用权限点") @PostMapping("/status")
    public CommonResult<Boolean> status(@RequestParam Long id,@RequestParam boolean enabled){return CommonResult.success(businessPermissionService.updateEnabled(id,enabled,operator()));}

    private Long operator(){Long id=adminActorService.getLinkedFrontUserId(adminActorService.getCurrentAdmin());return id==null?-Long.valueOf(adminActorService.getCurrentAdmin().getId()):id;}
}
