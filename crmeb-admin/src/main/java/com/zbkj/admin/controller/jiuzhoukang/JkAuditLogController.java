package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkAuditLogSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkAuditLogResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/admin/jk/audit-log")
@Api(tags = "九州康审核日志")
public class JkAuditLogController {

    @Autowired
    private JkAuditLogService auditLogService;

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_AUDIT_LOG_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AUDIT_LOG_VIEW, checkDataScope = true)
    @ApiOperation("审核日志列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<JkAuditLogResponse>> list(@Validated JkAuditLogSearchRequest request,
                                                             @Validated PageParamRequest pageParamRequest) {
        List<JkAuditLogResponse> rows = auditLogService.getAdminList(request, pageParamRequest);
        return CommonResult.success(CommonPage.restPage(rows));
    }
}
