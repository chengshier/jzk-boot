package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleAuditRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.offline.JkOfflineSaleService;
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

@RestController
@RequestMapping("api/admin/jk/offline-sale")
@Api(tags = "九州康线下销售管理")
public class JkOfflineSaleAdminController {
    @Autowired private JkOfflineSaleService saleService;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_OFFLINE_SALE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_ALL, checkDataScope = true)
    public CommonResult<CommonPage<JkOfflineSale>> list(@RequestParam(required = false) Long sellerUserId,
                                                         @RequestParam(required = false) String status,
                                                         PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(saleService.list(sellerUserId, status, page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_OFFLINE_SALE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_ALL, checkDataScope = true)
    public CommonResult<JkOfflineSale> detail(@PathVariable Long id) {
        return CommonResult.success(saleService.detail(operator(), id, true));
    }

    @PostMapping("/audit")
    @ApiOperation("审核线下销售并执行真实库存、业绩和收益闭环")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_OFFLINE_SALE_AUDIT + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<JkOfflineSale> audit(@RequestBody @Validated JkOfflineSaleAuditRequest request) {
        return CommonResult.success(saleService.audit(operator(), request));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) return -Long.valueOf(actorService.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
