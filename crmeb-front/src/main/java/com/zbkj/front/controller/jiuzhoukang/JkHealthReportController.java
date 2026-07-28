package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.impl.jiuzhoukang.health.JkHealthReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/front/jk/health/report")
public class JkHealthReportController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkHealthReportService service;

    @GetMapping
    @JkBizPermission(value = JkV31PermissionCodes.HEALTH_REPORT_VIEW_SELF, checkDataScope = true)
    public CommonResult<Map<String, Object>> report(@RequestParam(defaultValue = "WEEK") String period) {
        return CommonResult.success(service.report(Long.valueOf(token.getUserId()), period));
    }
}
