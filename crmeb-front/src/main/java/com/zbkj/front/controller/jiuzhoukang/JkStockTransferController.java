package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentVoucherRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeDocumentSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockTransferDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.trade.StockTransferService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/jk/stock-transfer")
@Api(tags = "九州康创客/合伙人调拨")
public class JkStockTransferController {
    @Autowired
    private StockTransferService service;
    @Autowired
    private FrontTokenComponent token;

    @PostMapping("/create")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    @ApiOperation("创建调拨申请")
    public CommonResult<JkStockTransfer> create(@RequestBody @Validated JkTradeCreateRequest request) {
        return CommonResult.success(service.create(Long.valueOf(token.getUserId()), request));
    }

    @GetMapping("/list")
    @ApiOperation("调拨单列表")
    public CommonResult<CommonPage<JkStockTransfer>> list(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(service.getFrontList(Long.valueOf(token.getUserId()), request, pageParamRequest)));
    }

    @GetMapping("/{id}/detail")
    @ApiOperation("调拨单详情")
    public CommonResult<JkStockTransferDetailResponse> detail(@PathVariable Long id) {
        return CommonResult.success(service.getFrontDetail(Long.valueOf(token.getUserId()), id));
    }

    @PostMapping("/{id}/voucher")
    @ApiOperation("上传调拨付款凭证")
    public CommonResult<JkStockTransfer> voucher(@PathVariable Long id, @RequestBody @Validated JkPaymentVoucherRequest request) {
        return CommonResult.success(service.submitVoucher(Long.valueOf(token.getUserId()), id, request));
    }

    @GetMapping("/handle/list")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<CommonPage<JkStockTransfer>> handleList(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) { return CommonResult.success(CommonPage.restPage(service.getAdminList(Long.valueOf(token.getUserId()), request, pageParamRequest))); }
    @GetMapping("/handle/detail/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransferDetailResponse> handleDetail(@PathVariable Long id) { return CommonResult.success(service.getAdminDetail(Long.valueOf(token.getUserId()), id)); }
    @PostMapping("/handle/audit")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> handleAudit(@RequestBody @Validated JkPaymentAuditRequest request) { return CommonResult.success(service.audit(Long.valueOf(token.getUserId()), request)); }
    @PostMapping("/handle/confirm-payment")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> handleConfirmPayment(@RequestBody @Validated JkPaymentAuditRequest request) { return CommonResult.success(service.confirmPayment(Long.valueOf(token.getUserId()), request)); }
    @PostMapping("/handle/confirm-transfer")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> handleConfirmTransfer(@RequestBody @Validated JkBusinessActionRequest request) { return CommonResult.success(service.dispatch(Long.valueOf(token.getUserId()), request)); }
    @PostMapping("/handle/close")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> handleClose(@RequestBody @Validated JkBusinessActionRequest request) { return CommonResult.success(service.close(Long.valueOf(token.getUserId()), request)); }
    @PostMapping("/{id}/receive")
    @ApiOperation("确认调拨收货入库")
    public CommonResult<JkStockTransfer> receive(@PathVariable Long id, @RequestBody JkBusinessActionRequest request) {
        request.setBusinessId(id);
        return CommonResult.success(service.receive(Long.valueOf(token.getUserId()), request));
    }
}

