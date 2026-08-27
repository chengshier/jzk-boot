package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkReceiveExceptionResolution;
import com.zbkj.common.request.jiuzhoukang.JkReceiveExceptionResolutionActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkReceiveExceptionResolutionCreateRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.trade.JkReceiveExceptionResolutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 异常收货 V2 分项处理。 */
@RestController
@RequestMapping("api/admin/jk/receive-exception-resolution")
@Api(tags = "九州康异常收货V2处理")
public class JkReceiveExceptionResolutionAdminController {
    @Autowired private JkReceiveExceptionResolutionService service;
    @Autowired private JkAdminActorService actor;

    @GetMapping("/list")
    @ApiOperation("查询异常单处理方案")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_RECEIVE_EXCEPTION_LIST + "')")
    public CommonResult<List<JkReceiveExceptionResolution>> list(@RequestParam Long exceptionId) {
        return CommonResult.success(service.list(exceptionId));
    }

    @PostMapping("/create")
    @ApiOperation("创建分SKU补发、退款、退回或接受方案")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_RECEIVE_EXCEPTION_HANDLE + "')")
    public CommonResult<JkReceiveExceptionResolution> create(@RequestBody @Validated JkReceiveExceptionResolutionCreateRequest request) {
        return CommonResult.success(service.create(operatorId(), request));
    }

    @PostMapping("/complete")
    @ApiOperation("确认处理方案已真实完成")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_RECEIVE_EXCEPTION_HANDLE + "')")
    public CommonResult<JkReceiveExceptionResolution> complete(@RequestBody @Validated JkReceiveExceptionResolutionActionRequest request) {
        return CommonResult.success(service.complete(operatorId(), request));
    }

    @PostMapping("/cancel")
    @ApiOperation("取消尚未确认的处理方案")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_RECEIVE_EXCEPTION_HANDLE + "')")
    public CommonResult<JkReceiveExceptionResolution> cancel(@RequestBody @Validated JkReceiveExceptionResolutionActionRequest request) {
        return CommonResult.success(service.cancel(operatorId(), request));
    }

    private Long operatorId() {
        Long linked = actor.getLinkedFrontUserId(actor.getCurrentAdmin());
        if (linked != null) return linked;
        if (actor.isPlatformSuperAdmin(actor.getCurrentAdmin())) return -Long.valueOf(actor.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
