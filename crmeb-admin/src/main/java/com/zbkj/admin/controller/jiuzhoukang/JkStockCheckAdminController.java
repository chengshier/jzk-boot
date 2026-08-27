package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkStockCheck;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCountRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCreateRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.stock.JkStockCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/jk/stock-check")
public class JkStockCheckAdminController {
    @Autowired private JkStockCheckService service;
    @Autowired private JkAdminActorService actor;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('"+JkV31PermissionCodes.ADMIN_STOCK_CHECK_LIST+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_BATCH_MANAGE,checkDataScope=false)
    public CommonResult<JkStockCheck> create(@RequestBody @Validated JkStockCheckCreateRequest request){return CommonResult.success(service.create(operator(),request,true));}

    @PostMapping("/{id}/count")
    @PreAuthorize("hasAuthority('"+JkV31PermissionCodes.ADMIN_STOCK_CHECK_LIST+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_BATCH_MANAGE,checkDataScope=false)
    public CommonResult<JkStockCheck> count(@PathVariable Long id,@RequestBody @Validated JkStockCheckCountRequest request){return CommonResult.success(service.count(operator(),id,request,true));}

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('"+JkV31PermissionCodes.ADMIN_STOCK_CHECK_LIST+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_BATCH_MANAGE,checkDataScope=false)
    public CommonResult<JkStockCheck> submit(@PathVariable Long id,@RequestBody @Validated JkStockCheckActionRequest request){return CommonResult.success(service.submit(operator(),id,request,true));}

    @PostMapping("/audit")
    @PreAuthorize("hasAuthority('"+JkV31PermissionCodes.ADMIN_STOCK_CHECK_AUDIT+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_BATCH_MANAGE,checkDataScope=false)
    public CommonResult<JkStockCheck> audit(@RequestBody @Validated JkStockCheckAuditRequest request){return CommonResult.success(service.audit(operator(),request));}

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('"+JkV31PermissionCodes.ADMIN_STOCK_CHECK_LIST+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_BATCH_VIEW,checkDataScope=true)
    public CommonResult<CommonPage<JkStockCheck>> list(@RequestParam(required=false) Long ownerUserId,@RequestParam(required=false) String status,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.list(ownerUserId,status,page)));}

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('"+JkV31PermissionCodes.ADMIN_STOCK_CHECK_LIST+"')")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_BATCH_VIEW,checkDataScope=true)
    public CommonResult<JkStockCheck> detail(@PathVariable Long id){return CommonResult.success(service.detail(operator(),id,true));}

    private Long operator(){Long linked=actor.getLinkedFrontUserId(actor.getCurrentAdmin());if(linked!=null)return linked;if(actor.isPlatformSuperAdmin(actor.getCurrentAdmin()))return -Long.valueOf(actor.getCurrentAdmin().getId());throw new IllegalStateException("后台管理员未绑定业务用户");}
}
