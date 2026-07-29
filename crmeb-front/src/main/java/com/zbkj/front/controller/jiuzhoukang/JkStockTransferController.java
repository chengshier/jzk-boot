package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
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
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeLogisticsService;
import com.zbkj.service.service.jiuzhoukang.trade.StockTransferService;
import com.zbkj.service.service.jiuzhoukang.wechat.JkSubscriptionBusinessNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/jk/stock-transfer")
@Api(tags = "九州康创客/合伙人调拨")
public class JkStockTransferController {
    @Autowired private StockTransferService service;
    @Autowired private JkTradeLogisticsService logisticsService;
    @Autowired private FrontTokenComponent token;
    @Autowired private JkSubscriptionBusinessNotificationService notificationService;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @PostMapping("/create")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    @ApiOperation("创建调拨申请")
    public CommonResult<JkStockTransfer> create(@RequestBody @Validated JkTradeCreateRequest request) { return CommonResult.success(service.create(userId(), request)); }

    @GetMapping("/list")
    @ApiOperation("调拨单列表")
    public CommonResult<CommonPage<JkStockTransfer>> list(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) { return CommonResult.success(CommonPage.restPage(service.getFrontList(userId(), request, pageParamRequest))); }

    @GetMapping("/{id}/detail")
    @ApiOperation("调拨单详情")
    public CommonResult<JkStockTransferDetailResponse> detail(@PathVariable Long id) { return CommonResult.success(service.getFrontDetail(userId(), id)); }

    @PostMapping("/{id}/voucher")
    @ApiOperation("上传调拨付款凭证")
    public CommonResult<JkStockTransfer> voucher(@PathVariable Long id, @RequestBody @Validated JkPaymentVoucherRequest request) { return CommonResult.success(service.submitVoucher(userId(), id, request)); }

    @GetMapping("/handle/list")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<CommonPage<JkStockTransfer>> handleList(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest) { return CommonResult.success(CommonPage.restPage(service.getAdminList(userId(), request, pageParamRequest))); }

    @GetMapping("/handle/detail/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransferDetailResponse> handleDetail(@PathVariable Long id) { return CommonResult.success(service.getAdminDetail(userId(), id)); }

    @PostMapping("/handle/audit")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> handleAudit(@RequestBody @Validated JkPaymentAuditRequest request) {
        JkStockTransfer transfer = service.audit(userId(), request);
        boolean approved = "AUDIT_APPROVED".equals(transfer.getStatus());
        notificationService.notifyAuditResult(
                "STOCK_TRANSFER",
                transfer.getId(),
                transfer.getTransferNo(),
                transfer.getUserId(),
                "库存调拨申请审核",
                approved ? "已通过" : "已驳回",
                approved ? request.getRemark() : transfer.getRejectReason(),
                detailPage(transfer));
        return CommonResult.success(transfer);
    }

    @PostMapping("/handle/confirm-payment")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> handleConfirmPayment(@RequestBody @Validated JkPaymentAuditRequest request) {
        JkStockTransfer transfer = service.confirmPayment(userId(), request);
        boolean approved = "PAYMENT_APPROVED".equals(transfer.getStatus());
        notificationService.notifyTransferStatus(
                "STOCK_TRANSFER",
                transfer.getId(),
                transfer.getTransferNo(),
                transfer.getUserId(),
                approved ? "付款已确认" : "付款被驳回",
                approved ? request.getRemark() : transfer.getRejectReason(),
                detailPage(transfer));
        return CommonResult.success(transfer);
    }

    @PostMapping("/handle/confirm-transfer")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> handleConfirmTransfer(@RequestBody @Validated JkBusinessActionRequest request) {
        JkStockTransfer transfer = logisticsService.dispatchStockTransfer(userId(), request);
        notificationService.notifyReceiveReminder(
                "STOCK_TRANSFER",
                transfer.getId(),
                transfer.getTransferNo(),
                transfer.getUserId(),
                "调拨商品待收货",
                request.getRemark(),
                detailPage(transfer));
        return CommonResult.success(transfer);
    }

    @PostMapping("/handle/close")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_TRANSFER_CONFIRM, checkDataScope = true)
    public CommonResult<JkStockTransfer> handleClose(@RequestBody @Validated JkBusinessActionRequest request) { return CommonResult.success(service.close(userId(), request)); }

    @PostMapping("/{id}/cancel")
    @ApiOperation("申请人取消未进入审核/付款处理的调拨单")
    @JkBizPermission(value = JkBizPermissionCodes.TRADE_CANCEL_SELF, checkDataScope = true)
    public CommonResult<JkStockTransfer> cancel(@PathVariable Long id, @RequestBody JkBusinessActionRequest request) { request.setBusinessId(id); return CommonResult.success(service.cancel(userId(), request)); }

    @PostMapping("/{id}/receive")
    @ApiOperation("确认调拨收货入库")
    public CommonResult<JkStockTransfer> receive(@PathVariable Long id, @RequestBody JkBusinessActionRequest request) {
        request.setBusinessId(id);
        JkStockTransfer transfer = service.receive(userId(), request);
        notificationService.notifyTransferStatus(
                "STOCK_TRANSFER",
                transfer.getId(),
                transfer.getTransferNo(),
                transfer.getCountyAgentId(),
                "下级已收货",
                request.getRemark(),
                detailPage(transfer));
        return CommonResult.success(transfer);
    }

    private String detailPage(JkStockTransfer transfer) {
        return "pages/jk/trade/detail?businessType=STOCK_TRANSFER&id=" + transfer.getId();
    }
}
