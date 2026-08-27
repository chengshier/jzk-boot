package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.request.jiuzhoukang.JkUserProfileRegionSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkUserProfileRegionResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.profile.JkUserProfileRegionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/jk/user")
@Api(tags = "九州康用户个人资料区域管理")
public class JkUserProfileRegionAdminController {
    @Autowired private JkUserProfileRegionService service;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/{userId}/region")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_USER_PROFILE_REGION_VIEW + "')")
    @ApiOperation("读取用户现有个人资料区域")
    public CommonResult<JkUserProfileRegionResponse> get(@PathVariable Long userId) {
        return CommonResult.success(service.get(userId));
    }

    @PostMapping("/{userId}/region")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_USER_PROFILE_REGION_EDIT + "')")
    @ApiOperation("修改用户现有个人资料区域并记录审计；不覆盖收货地址")
    public CommonResult<JkUserProfileRegionResponse> save(@PathVariable Long userId,
                                                           @RequestBody @Validated JkUserProfileRegionSaveRequest request) {
        SystemAdmin admin = actorService.getCurrentAdmin();
        Long adminId = admin == null || admin.getId() == null ? -1L : admin.getId().longValue();
        String adminName = admin == null ? "unknown" : (admin.getRealName() == null ? admin.getAccount() : admin.getRealName());
        return CommonResult.success(service.saveByAdmin(userId, adminId, adminName, request));
    }
}
