package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeDocumentSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkPlatformOrderDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeLogisticsService;
import com.zbkj.service.service.jiuzhoukang.trade.PlatformOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/jk/platform-order")
@Api(tags = "九州康区县代订货管理")
public class JkPlatformOrderController {
    @Autowired private PlatformOrderService platformOrderService;
    @Autowired private JkTradeLogisticsService logisticsService;
    @Autowired private JkAdminActorService adminActorService;

    private Long operatorId() {
        Long userId = adminActorService.getLinkedFrontUserId(adminActorService.getCurrentAdmin());
        if (userId != null) return userId;
        if (adminActorService.isPlatformSuperAdmin(adminActorService.getCurrentAdmin())) {
            return -Long.valueOf(adminActorService.getCurrentAdmin().getId());
        }
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }

    @GetMapping("/list")
    @ApiOperation("区县代订货单列表")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PLATFORM_ORDER_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_PLATFORM_AUDIT, checkDataScope = false)
    public CommonResult<CommonPage<JkPlatformOrder>> list(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(platformOrderService.getAdminList(request, pageParamRequest)));
    }

    @GetMapping("/{id}/detail")
    @ApiOperation("区县代订货单详情")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PLATFORM_ORDER_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_PLATFORM_AUDIT, checkDataScope = false)
    public CommonResult<JkPlatformOrderDetailResponse> detail(@PathVariable Long id) {
        return CommonResult.success(platformOrderService.getAdminDetail(id));
    }

    @PostMapping("/payment/audit")
    @ApiOperation("审核平台订货付款")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PLATFORM_ORDER_AUDIT + "')")
    @JkBizPermission(value = JkBizPermissionCodes.PAYMENT_OFFLINE_AUDIT, checkDataScope = true)
    public CommonResult<JkPlatformOrder> audit(@RequestBody @Validated JkPaymentAuditRequest request) {
        return CommonResult.success(platformOrderService.auditPayment(operatorId(), request));
    }

    @PostMapping("/ship")
    @ApiOperation("平台发货并登记物流")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PLATFORM_ORDER_SHIP + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_PLATFORM_AUDIT, checkDataScope = false)
    public CommonResult<JkPlatformOrder> ship(@RequestBody @Validated JkBusinessActionRequest request) {
        return CommonResult.success(logisticsService.shipPlatformOrder(operatorId(), request));
    }

    @PostMapping("/close")
    @ApiOperation("关闭平台订货单")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PLATFORM_ORDER_CLOSE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_PLATFORM_AUDIT, checkDataScope = false)
    public CommonResult<JkPlatformOrder> close(@RequestBody @Validated JkBusinessActionRequest request) {
        return CommonResult.success(platformOrderService.close(operatorId(), request));
    }
}
