package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.impl.jiuzhoukang.health.JkHealthReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/health/report")
public class JkHealthReportAdminController {
    @Autowired private JkHealthReportService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_HEALTH_REPORT + "')")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_EMERGENCY_VIEW, checkDataScope = true)
    public CommonResult<Map<String, Object>> report(@RequestParam Long userId,
                                                     @RequestParam(defaultValue = "WEEK") String period) {
        return CommonResult.success(service.report(userId, period));
    }
}
