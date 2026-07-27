package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeDocumentSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockTransferDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeLogisticsService;
import com.zbkj.service.service.jiuzhoukang.trade.StockTransferService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/jk/stock-transfer")
@Api(tags = "九州康创客/合伙人调拨管理")
public class JkStockTransferController {
    @Autowired private StockTransferService service;
    @Autowired private JkTradeLogisticsService logisticsService;
    @Autowired private JkAdminActorService actor;

    private Long user() {
        Long id = actor.getLinkedFrontUserId(actor.getCurrentAdmin());
        if (id == null) throw new IllegalStateException("后台管理员未绑定业务用户");
        return id;
    }

    @GetMapping("/list")
    @ApiOperation("调拨单列表")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_TRANSFER_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_AUDIT, checkDataScope = true)
    public CommonResult<CommonPage<JkStockTransfer>> list(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(service.getAdminList(user(), request, pageParamRequest)));
    }

    @GetMapping("/{id}/detail")
    @ApiOperation("调拨单详情")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_TRANSFER_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransferDetailResponse> detail(@PathVariable Long id) {
        return CommonResult.success(service.getAdminDetail(user(), id));
    }

    @PostMapping("/audit")
    @ApiOperation("审核调拨申请")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_TRANSFER_AUDIT + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransfer> audit(@RequestBody @Validated JkPaymentAuditRequest request) {
        return CommonResult.success(service.audit(user(), request));
    }

    @PostMapping("/payment/confirm")
    @ApiOperation("确认调拨付款")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_TRANSFER_PAYMENT + "')")
    @JkBizPermission(value = JkBizPermissionCodes.PAYMENT_OFFLINE_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransfer> payment(@RequestBody @Validated JkPaymentAuditRequest request) {
        return CommonResult.success(service.confirmPayment(user(), request));
    }

    @PostMapping("/dispatch")
    @ApiOperation("区县代拨货并登记物流")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_TRANSFER_DISPATCH + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> dispatch(@RequestBody @Validated JkBusinessActionRequest request) {
        return CommonResult.success(logisticsService.dispatchStockTransfer(user(), request));
    }

    @PostMapping("/close")
    @ApiOperation("关闭调拨单")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_TRANSFER_CLOSE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransfer> close(@RequestBody @Validated JkBusinessActionRequest request) {
        return CommonResult.success(service.close(user(), request));
    }
}
