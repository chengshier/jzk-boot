package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.context.JkBusinessContextOverviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/context")
@Api(tags = "九州康统一业务上下文")
public class JkBusinessContextOverviewController {
    @Autowired private JkBusinessContextOverviewService service;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_BUSINESS_CONTEXT_VIEW + "')")
    @ApiOperation("统一抽屉只读概览；不执行审核、退款、冲正或余额修改")
    public CommonResult<Map<String, Object>> overview(@RequestParam String businessType,
                                                       @RequestParam Long businessId) {
        return CommonResult.success(service.overview(businessType, businessId));
    }
}
