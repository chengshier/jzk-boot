package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttributionAdjustment;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRetailAttributionResolveRequest;
import com.zbkj.common.response.jiuzhoukang.JkOptionResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.order.JkRetailAttributionAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/retail-attribution")
@Api(tags = "九州康零售订单归属")
public class JkRetailAttributionAdminController {
    @Autowired private JkRetailAttributionAdminService service;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_LIST + "')")
    public CommonResult<CommonPage<JkRetailOrderAttribution>> list(@RequestParam(required = false) String orderNo,
                                                                   @RequestParam(required = false) Long buyerUserId,
                                                                   @RequestParam(required = false) String regionSourceType,
                                                                   @RequestParam(required = false) String attributionStatus,
                                                                   PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.list(orderNo, buyerUserId, regionSourceType, attributionStatus, page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_DETAIL + "')")
    @ApiOperation("归属详情、候选解释和调整历史")
    public CommonResult<Map<String, Object>> detail(@PathVariable Long id) {
        return CommonResult.success(service.detail(id));
    }

    @GetMapping("/{id}/overview")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_DETAIL + "')")
    @ApiOperation("抽屉上下文概览")
    public CommonResult<Map<String, Object>> overview(@PathVariable Long id) {
        return CommonResult.success(service.overview(id));
    }

    @GetMapping("/{id}/adjustments")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_DETAIL + "')")
    public CommonResult<List<JkRetailOrderAttributionAdjustment>> adjustments(@PathVariable Long id) {
        return CommonResult.success(service.audit(id));
    }

    @GetMapping("/options/regions")
    @PreAuthorize("hasAnyAuthority('" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_RESOLVE + "','" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_ADJUST + "')")
    @ApiOperation("零售归属调整的最终区域选项")
    public CommonResult<List<JkOptionResponse>> regionOptions(@RequestParam(required = false) String keyword) {
        return CommonResult.success(service.listRegionOptions(keyword));
    }

    @GetMapping("/options/county-agents")
    @PreAuthorize("hasAnyAuthority('" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_RESOLVE + "','" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_ADJUST + "')")
    @ApiOperation("零售归属调整的区县代理选项")
    public CommonResult<List<JkOptionResponse>> countyAgentOptions(@RequestParam String regionCode,
                                                                    @RequestParam(required = false) String keyword) {
        return CommonResult.success(service.listCountyAgentOptions(regionCode, keyword));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_RESOLVE + "')")
    @ApiOperation("仅处理未锁定的待人工/冲突归属")
    public CommonResult<JkRetailOrderAttribution> resolve(@PathVariable Long id,
                                                           @RequestBody @Validated JkRetailAttributionResolveRequest request) {
        return CommonResult.success(service.resolve(id, adminId(), request));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_RETAIL_ATTRIBUTION_ADJUST + "')")
    @ApiOperation("已锁定记录只创建冲正与补偿任务，不直接覆盖原快照")
    public CommonResult<JkRetailOrderAttributionAdjustment> adjust(@PathVariable Long id,
                                                                    @RequestBody @Validated JkRetailAttributionResolveRequest request) {
        return CommonResult.success(service.adjust(id, adminId(), request));
    }

    private Long adminId() {
        SystemAdmin admin = actorService.getCurrentAdmin();
        return admin == null || admin.getId() == null ? -1L : admin.getId().longValue();
    }
}
