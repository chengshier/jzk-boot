package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkPerformancePeriod;
import com.zbkj.common.request.jiuzhoukang.JkPerformancePeriodBuildRequest;
import com.zbkj.common.request.jiuzhoukang.JkPerformancePeriodCloseRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformancePeriodService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/performance-period")
@Api(tags = "九州康周期业绩与阶梯奖励")
public class JkPerformancePeriodAdminController {
    @Autowired private JkPerformancePeriodService service;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PERFORMANCE_PERIOD_LIST + "')")
    public CommonResult<List<JkPerformancePeriod>> list(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String periodType) {
        return CommonResult.success(service.list(status, periodType));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PERFORMANCE_PERIOD_LIST + "')")
    public CommonResult<Map<String, Object>> detail(@PathVariable Long id) {
        return CommonResult.success(service.detail(id));
    }

    @PostMapping("/build")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PERFORMANCE_PERIOD_BUILD + "')")
    @ApiOperation("仅从有效线上零售和经核验线下终端销售构建周期业绩")
    public CommonResult<JkPerformancePeriod> build(@RequestBody @Validated JkPerformancePeriodBuildRequest request) {
        return CommonResult.success(service.build(request, operator()));
    }

    @PostMapping("/{id}/trial")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PERFORMANCE_PERIOD_TRIAL + "')")
    @ApiOperation("试算周期奖励，不写佣金")
    public CommonResult<Map<String, Object>> trial(@PathVariable Long id) {
        return CommonResult.success(service.trial(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PERFORMANCE_PERIOD_CLOSE + "')")
    @ApiOperation("审核关闭周期并按已发布规则生成真实佣金；关闭后不可直接重算")
    public CommonResult<JkPerformancePeriod> close(@PathVariable Long id,
                                                    @RequestBody @Validated JkPerformancePeriodCloseRequest request) {
        return CommonResult.success(service.close(id, request, operator()));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) return -Long.valueOf(actorService.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
