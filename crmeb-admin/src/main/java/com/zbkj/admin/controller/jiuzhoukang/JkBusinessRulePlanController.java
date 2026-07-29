package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRulePlan;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRulePlanPublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRulePlanSaveRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.business.JkBusinessRulePlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/business-plan")
@Api(tags = "九州康商业方案与版本")
public class JkBusinessRulePlanController {
    @Autowired private JkBusinessRulePlanService service;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_BUSINESS_PLAN_LIST + "')")
    public CommonResult<List<JkBusinessRulePlan>> list(@RequestParam(required = false) String planCode,
                                                        @RequestParam(required = false) String publishStatus) {
        return CommonResult.success(service.list(planCode, publishStatus));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_BUSINESS_PLAN_LIST + "')")
    public CommonResult<Map<String, Object>> detail(@PathVariable Long id) {
        return CommonResult.success(service.detail(id));
    }

    @GetMapping("/role-cards")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_BUSINESS_PLAN_LIST + "')")
    @ApiOperation("按业务目的展示创客、合伙人和区县代理方案卡片")
    public CommonResult<List<Map<String, Object>>> roleCards() {
        return CommonResult.success(service.roleCards());
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_BUSINESS_PLAN_EDIT + "')")
    @ApiOperation("保存方案草稿；不会自动发布或启用任何奖励模板")
    public CommonResult<JkBusinessRulePlan> save(@RequestBody @Validated JkBusinessRulePlanSaveRequest request) {
        return CommonResult.success(service.saveDraft(request));
    }

    @PostMapping("/{id}/copy")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_BUSINESS_PLAN_EDIT + "')")
    public CommonResult<JkBusinessRulePlan> copy(@PathVariable Long id, @RequestParam(required = false) String changeSummary) {
        return CommonResult.success(service.copyVersion(id, changeSummary));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_BUSINESS_PLAN_PUBLISH + "')")
    public CommonResult<JkBusinessRulePlan> publish(@RequestBody @Validated JkBusinessRulePlanPublishRequest request) {
        return CommonResult.success(service.publish(request, operator()));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_BUSINESS_PLAN_DISABLE + "')")
    public CommonResult<JkBusinessRulePlan> disable(@PathVariable Long id, @RequestParam String reason) {
        return CommonResult.success(service.disable(id, reason, operator()));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) return -Long.valueOf(actorService.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
