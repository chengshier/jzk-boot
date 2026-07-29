package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeReceiveExceptionHandleRequest;
import com.zbkj.common.response.jiuzhoukang.JkTradeReceiveExceptionDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeReceiveExceptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台订货/调拨异常收货工作台。 */
@RestController
@RequestMapping("api/admin/jk/receive-exception")
@Api(tags = "九州康异常收货处理")
public class JkTradeReceiveExceptionAdminController {
    @Autowired private JkTradeReceiveExceptionService service;
    @Autowired private JkAdminActorService actor;

    @GetMapping("/list")
    @ApiOperation("异常收货列表")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_RECEIVE_EXCEPTION_LIST + "')")
    public CommonResult<CommonPage<JkTradeReceiveException>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long receiverUserId,
            PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(
                service.listAdmin(status, businessType, receiverUserId, page)));
    }

    @GetMapping("/{id}")
    @ApiOperation("异常收货详情")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_RECEIVE_EXCEPTION_LIST + "')")
    public CommonResult<JkTradeReceiveExceptionDetailResponse> detail(@PathVariable Long id) {
        return CommonResult.success(service.detailAdmin(id));
    }

    @PostMapping("/handle")
    @ApiOperation("标记处理中或驳回；完成必须使用异常收货V2分项方案")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_RECEIVE_EXCEPTION_HANDLE + "')")
    public CommonResult<JkTradeReceiveExceptionDetailResponse> handle(
            @RequestBody @Validated JkTradeReceiveExceptionHandleRequest request) {
        if ("RESOLVED".equalsIgnoreCase(request.getAction())) {
            throw new IllegalArgumentException("异常处理完成必须通过 V2 分 SKU 方案，旧接口禁止直接完成");
        }
        return CommonResult.success(service.handle(operatorId(), request));
    }

    private Long operatorId() {
        Long linked = actor.getLinkedFrontUserId(actor.getCurrentAdmin());
        if (linked != null) return linked;
        if (actor.isPlatformSuperAdmin(actor.getCurrentAdmin())) {
            return -Long.valueOf(actor.getCurrentAdmin().getId());
        }
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
