package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.*;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.commission.*;
import com.zbkj.service.service.jiuzhoukang.event.JkBusinessEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/admin/jk/closure")
public class JkClosureOperationController {
    @Autowired private JkBusinessEventService eventService;
    @Autowired private CommissionAutoSettleService autoSettleService;
    @Autowired private AccountReconcileService reconcileService;
    @Autowired private JkAdminActorService actor;

    @GetMapping("/event/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_BUSINESS_EVENT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.BUSINESS_EVENT_MANAGE)
    public CommonResult<CommonPage<JkBusinessEvent>> eventList(@RequestParam(required = false) String eventType,
                                                                 @RequestParam(required = false) String eventStatus,
                                                                 PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(eventService.list(eventType, eventStatus, page)));
    }

    @PostMapping("/event/retry")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_BUSINESS_EVENT_RETRY + "')")
    @JkBizPermission(value = JkBizPermissionCodes.BUSINESS_EVENT_MANAGE)
    public CommonResult<JkBusinessEvent> retry(@RequestBody @Validated JkBusinessEventRetryRequest request) {
        return CommonResult.success(eventService.retry(request.getEventId(), operator()));
    }

    @PostMapping("/commission/auto-settle")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_SETTLE_AUTO + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_AUTO_SETTLE)
    public CommonResult<Integer> autoSettle(@RequestParam(defaultValue = "500") int limit,
                                             @RequestParam(required = false) String triggerNo) {
        return CommonResult.success(autoSettleService.settleDue(limit, operator(), triggerNo));
    }

    @GetMapping("/account-reconcile/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_ACCOUNT_RECONCILE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.ACCOUNT_RECONCILE_MANAGE)
    public CommonResult<CommonPage<JkAccountReconcileRecord>> reconcileList(@RequestParam(required = false) String batchNo,
                                                                              @RequestParam(required = false) String status,
                                                                              @RequestParam(required = false) Long userId,
                                                                              PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(reconcileService.list(batchNo, status, userId, page)));
    }

    @PostMapping("/account-reconcile/run")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_ACCOUNT_RECONCILE_RUN + "')")
    @JkBizPermission(value = JkBizPermissionCodes.ACCOUNT_RECONCILE_MANAGE)
    public CommonResult<List<JkAccountReconcileRecord>> reconcile(@RequestBody JkAccountReconcileRequest request) {
        return CommonResult.success(reconcileService.reconcile(request.getUserId(), request.getRoleCode(), operator(), request.getRequestNo()));
    }

    private Long operator() {
        Long linked = actor.getLinkedFrontUserId(actor.getCurrentAdmin());
        if (linked != null) return linked;
        if (actor.isPlatformSuperAdmin(actor.getCurrentAdmin())) return -Long.valueOf(actor.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
