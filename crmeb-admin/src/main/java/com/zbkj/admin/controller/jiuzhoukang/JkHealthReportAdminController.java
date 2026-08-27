package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkHealthReport;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin/jk/health/report")
public class JkHealthReportAdminController {
    @Autowired private JkHealthReportService reportService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_HEALTH_REPORT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.REPORT_VIEW, checkDataScope = true)
    public CommonResult<CommonPage<JkHealthReport>> list(@RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) String reportType,
                                                          PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(reportService.list(userId, reportType, page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_HEALTH_REPORT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.REPORT_VIEW, checkDataScope = true)
    public CommonResult<JkHealthReport> detail(@PathVariable Long id) {
        return CommonResult.success(reportService.detail(-1L, id, true));
    }
}
