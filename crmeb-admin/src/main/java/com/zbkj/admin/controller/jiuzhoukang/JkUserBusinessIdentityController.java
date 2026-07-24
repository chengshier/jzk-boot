package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityStatusOperateRequest;
import com.zbkj.common.request.jiuzhoukang.JkUserBusinessRoleSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkUserBusinessRoleResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.identity.JkUserBusinessRoleService;
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
@RequestMapping("api/admin/jk/identity")
@Api(tags = "九州康用户业务身份管理")
public class JkUserBusinessIdentityController {

    @Autowired
    private JkUserBusinessRoleService userBusinessRoleService;

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_USER_BUSINESS_ROLE_LIST + "')")
    @JkBizPermission(checkDataScope = true)
    @ApiOperation("用户业务身份列表")
    @GetMapping("/user-role/list")
    public CommonResult<CommonPage<JkUserBusinessRoleResponse>> list(@Validated JkUserBusinessRoleSearchRequest request,
                                                                     @Validated PageParamRequest pageParamRequest) {
        List<JkUserBusinessRoleResponse> rows = userBusinessRoleService.getAdminList(request, pageParamRequest);
        return CommonResult.success(CommonPage.restPage(rows));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_IDENTITY_FREEZE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.IDENTITY_FREEZE, checkDataScope = true)
    @ApiOperation("冻结身份")
    @PostMapping("/freeze")
    public CommonResult<Boolean> freeze(@RequestBody @Validated JkIdentityStatusOperateRequest request) {
        return CommonResult.success(userBusinessRoleService.freeze(request.getUserBusinessRoleId(), request.getReason()));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_IDENTITY_UNFREEZE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.IDENTITY_UNFREEZE, checkDataScope = true)
    @ApiOperation("解冻身份")
    @PostMapping("/unfreeze")
    public CommonResult<Boolean> unfreeze(@RequestBody @Validated JkIdentityStatusOperateRequest request) {
        return CommonResult.success(userBusinessRoleService.unfreeze(request.getUserBusinessRoleId(), request.getReason()));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_IDENTITY_CANCEL + "')")
    @JkBizPermission(value = JkBizPermissionCodes.IDENTITY_CANCEL, checkDataScope = true)
    @ApiOperation("取消身份")
    @PostMapping("/cancel")
    public CommonResult<Boolean> cancel(@RequestBody @Validated JkIdentityStatusOperateRequest request) {
        return CommonResult.success(userBusinessRoleService.cancel(request.getUserBusinessRoleId(), request.getReason()));
    }
}
