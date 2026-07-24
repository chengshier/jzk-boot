package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturn;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkStockTransferReturnDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.trade.StockTransferReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/jk/stock-transfer-return")
public class JkStockTransferReturnController {
    @Autowired private StockTransferReturnService service;
    @Autowired private FrontTokenComponent token;
    private Long user() { return Long.valueOf(token.getUserId()); }

    @PostMapping("/create")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_APPLY, checkDataScope = true)
    public CommonResult<JkStockTransferReturn> create(@RequestBody @Validated JkStockTransferReturnCreateRequest request) { return CommonResult.success(service.create(user(), request)); }

    @GetMapping("/list")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_APPLY, checkDataScope = true)
    public CommonResult<CommonPage<JkStockTransferReturn>> list(@RequestParam(required = false) String status, PageParamRequest page) { return CommonResult.success(CommonPage.restPage(service.getFrontList(user(), status, page))); }

    @GetMapping("/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_APPLY, checkDataScope = true)
    public CommonResult<JkStockTransferReturnDetailResponse> detail(@PathVariable Long id) { return CommonResult.success(service.getFrontDetail(user(), id)); }

    @PostMapping("/{id}/cancel")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_APPLY, checkDataScope = true)
    public CommonResult<JkStockTransferReturn> cancel(@PathVariable Long id, @RequestBody JkBusinessActionRequest request) { request.setBusinessId(id); return CommonResult.success(service.cancel(user(), request)); }

    @PostMapping("/{id}/ship")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_APPLY, checkDataScope = true)
    public CommonResult<JkStockTransferReturn> ship(@PathVariable Long id, @RequestBody @Validated JkStockTransferReturnShipRequest request) { return CommonResult.success(service.ship(user(), id, request)); }

    @GetMapping("/handle/list")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT, checkDataScope = true)
    public CommonResult<CommonPage<JkStockTransferReturn>> handleList(@RequestParam(required = false) String status, PageParamRequest page) { return CommonResult.success(CommonPage.restPage(service.getHandleList(user(), status, page))); }

    @GetMapping("/handle/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransferReturnDetailResponse> handleDetail(@PathVariable Long id) { return CommonResult.success(service.getHandleDetail(user(), id)); }

    @PostMapping("/handle/audit")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransferReturn> audit(@RequestBody @Validated JkPaymentAuditRequest request) { return CommonResult.success(service.audit(user(), request)); }

    @PostMapping("/handle/receive")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransferReturn> receive(@RequestBody @Validated JkBusinessActionRequest request) { return CommonResult.success(service.receive(user(), request)); }

    @PostMapping("/handle/refund")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransferReturn> refund(@RequestBody @Validated JkStockTransferReturnRefundRequest request) { return CommonResult.success(service.confirmRefund(user(), request)); }

    @PostMapping("/handle/close")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT, checkDataScope = true)
    public CommonResult<JkStockTransferReturn> close(@RequestBody JkBusinessActionRequest request) { return CommonResult.success(service.close(user(), request)); }
}
