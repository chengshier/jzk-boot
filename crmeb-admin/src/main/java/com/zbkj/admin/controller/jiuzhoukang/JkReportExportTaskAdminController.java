package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkReportExportTask;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkReportExportTaskCreateRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.impl.jiuzhoukang.report.JkMinioReportExportService;
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
@RequestMapping("api/admin/jk/report-export-task")
public class JkReportExportTaskAdminController {
    @Autowired private JkMinioReportExportService service;
    @Autowired private JkAdminActorService actorService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_REPORT_EXPORT_TASK + "')")
    @JkBizPermission(value = JkBizPermissionCodes.REPORT_VIEW, checkDataScope = true)
    public CommonResult<JkReportExportTask> create(@RequestBody @Validated JkReportExportTaskCreateRequest request) {
        return CommonResult.success(service.create(operator(), request));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_REPORT_EXPORT_TASK + "')")
    @JkBizPermission(value = JkBizPermissionCodes.REPORT_VIEW, checkDataScope = true)
    public CommonResult<CommonPage<JkReportExportTask>> list(@RequestParam(required = false) String reportType,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) Long createdBy,
                                                              PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.list(reportType, status, createdBy, page)));
    }

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_REPORT_EXPORT_TASK + "')")
    @JkBizPermission(value = JkBizPermissionCodes.REPORT_VIEW, checkDataScope = false)
    public CommonResult<Integer> run(@RequestParam(defaultValue = "10") int limit) {
        return CommonResult.success(service.runPending(limit));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_REPORT_EXPORT_TASK + "')")
    @JkBizPermission(value = JkBizPermissionCodes.REPORT_VIEW, checkDataScope = false)
    public CommonResult<JkReportExportTask> retry(@PathVariable Long id) {
        return CommonResult.success(service.runOne(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_REPORT_EXPORT_TASK + "')")
    @JkBizPermission(value = JkBizPermissionCodes.REPORT_VIEW, checkDataScope = true)
    public CommonResult<JkReportExportTask> download(@PathVariable Long id) {
        return CommonResult.success(service.download(id, operator(), true));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) return -Long.valueOf(actorService.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
