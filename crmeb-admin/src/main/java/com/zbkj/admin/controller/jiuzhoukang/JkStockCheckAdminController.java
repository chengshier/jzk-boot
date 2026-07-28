package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkStockCheck;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckSubmitRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.impl.jiuzhoukang.stock.JkStockCheckService;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
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

@RestController
@RequestMapping("api/admin/jk/stock-check")
public class JkStockCheckAdminController {
    @Autowired private JkStockCheckService service;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_STOCK_CHECK_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_ALL, checkDataScope = true)
    public CommonResult<CommonPage<JkStockCheck>> list(@RequestParam(required = false) Long ownerUserId,
                                                        @RequestParam(required = false) Long stockAccountId,
                                                        @RequestParam(required = false) String status,
                                                        PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.list(ownerUserId, stockAccountId, status, page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_STOCK_CHECK_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_ALL, checkDataScope = true)
    public CommonResult<JkStockCheck> detail(@PathVariable Long id) {
        return CommonResult.success(service.detail(id, operator(), false));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_STOCK_CHECK_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_BATCH_MANAGE, checkDataScope = false)
    public CommonResult<JkStockCheck> create(@RequestBody @Validated JkStockCheckCreateRequest request) {
        return CommonResult.success(service.create(operator(), request, false));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_STOCK_CHECK_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_BATCH_MANAGE, checkDataScope = false)
    public CommonResult<JkStockCheck> submit(@RequestBody @Validated JkStockCheckSubmitRequest request) {
        return CommonResult.success(service.submit(operator(), request, false));
    }

    @PostMapping("/audit")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_STOCK_CHECK_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_BATCH_MANAGE, checkDataScope = false)
    public CommonResult<JkStockCheck> audit(@RequestBody @Validated JkStockCheckAuditRequest request) {
        return CommonResult.success(service.audit(operator(), request));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) return -Long.valueOf(actorService.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
