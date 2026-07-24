package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.*;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturn;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkStockTransferReturnDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.trade.StockTransferReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/jk/stock-transfer-return")
public class JkStockTransferReturnAdminController {
    @Autowired private StockTransferReturnService service;
    @Autowired private JkAdminActorService actor;
    private Long user() {
        Long id = actor.getLinkedFrontUserId(actor.getCurrentAdmin());
        if (id != null) return id;
        if (actor.isPlatformSuperAdmin(actor.getCurrentAdmin())) return -Long.valueOf(actor.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_STOCK_TRANSFER_RETURN_LIST+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT,checkDataScope=true)
    public CommonResult<CommonPage<JkStockTransferReturn>> list(@RequestParam(required=false)String status,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.getHandleList(user(),status,page)));}

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_STOCK_TRANSFER_RETURN_LIST+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT,checkDataScope=true)
    public CommonResult<JkStockTransferReturnDetailResponse> detail(@PathVariable Long id){return CommonResult.success(service.getHandleDetail(user(),id));}

    @PostMapping("/audit")
    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_STOCK_TRANSFER_RETURN_AUDIT+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT,checkDataScope=true)
    public CommonResult<JkStockTransferReturn> audit(@RequestBody @Validated JkPaymentAuditRequest request){return CommonResult.success(service.audit(user(),request));}

    @PostMapping("/receive")
    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_STOCK_TRANSFER_RETURN_RECEIVE+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT,checkDataScope=true)
    public CommonResult<JkStockTransferReturn> receive(@RequestBody @Validated JkBusinessActionRequest request){return CommonResult.success(service.receive(user(),request));}

    @PostMapping("/refund")
    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_STOCK_TRANSFER_RETURN_REFUND+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT,checkDataScope=true)
    public CommonResult<JkStockTransferReturn> refund(@RequestBody @Validated JkStockTransferReturnRefundRequest request){return CommonResult.success(service.confirmRefund(user(),request));}

    @PostMapping("/close")
    @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_STOCK_TRANSFER_RETURN_CLOSE+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_TRANSFER_RETURN_AUDIT,checkDataScope=true)
    public CommonResult<JkStockTransferReturn> close(@RequestBody @Validated JkBusinessActionRequest request){return CommonResult.success(service.close(user(),request));}
}
