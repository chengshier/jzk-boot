package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentVoucherRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeDocumentSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkPlatformOrderDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.trade.PlatformOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/jk/platform-order")
@Api(tags = "九州康区县代平台订货")
public class JkPlatformOrderController {
    @Autowired
    private PlatformOrderService platformOrderService;
    @Autowired
    private FrontTokenComponent frontTokenComponent;

    @PostMapping("/create")
    @ApiOperation("创建平台订货单")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_PLATFORM_ORDER, checkDataScope = false)
    public CommonResult<JkPlatformOrder> create(@RequestBody @Validated JkTradeCreateRequest request) {
        return CommonResult.success(platformOrderService.create(Long.valueOf(frontTokenComponent.getUserId()), request));
    }

    @GetMapping("/list")
    @ApiOperation("平台订货单列表")
    public CommonResult<CommonPage<JkPlatformOrder>> list(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(platformOrderService.getFrontList(Long.valueOf(frontTokenComponent.getUserId()), request, pageParamRequest)));
    }

    @GetMapping("/{id}/detail")
    @ApiOperation("平台订货单详情")
    public CommonResult<JkPlatformOrderDetailResponse> detail(@PathVariable Long id) {
        return CommonResult.success(platformOrderService.getFrontDetail(Long.valueOf(frontTokenComponent.getUserId()), id));
    }

    @PostMapping("/{id}/voucher")
    @ApiOperation("上传线下付款凭证")
    public CommonResult<JkPlatformOrder> voucher(@PathVariable Long id, @RequestBody @Validated JkPaymentVoucherRequest request) {
        return CommonResult.success(platformOrderService.submitVoucher(Long.valueOf(frontTokenComponent.getUserId()), id, request));
    }

    @PostMapping("/{id}/cancel")
    @ApiOperation("申请人取消未进入付款审核的订货单")
    @JkBizPermission(value = JkBizPermissionCodes.TRADE_CANCEL_SELF, checkDataScope = true)
    public CommonResult<JkPlatformOrder> cancel(@PathVariable Long id, @RequestBody JkBusinessActionRequest request) {
        request.setBusinessId(id);
        return CommonResult.success(platformOrderService.cancel(Long.valueOf(frontTokenComponent.getUserId()), request));
    }

    @PostMapping("/{id}/receive")
    @ApiOperation("区县代确认收货入库")
    public CommonResult<JkPlatformOrder> receive(@PathVariable Long id, @RequestBody JkBusinessActionRequest request) {
        request.setBusinessId(id);
        return CommonResult.success(platformOrderService.receive(Long.valueOf(frontTokenComponent.getUserId()), request));
    }
}
