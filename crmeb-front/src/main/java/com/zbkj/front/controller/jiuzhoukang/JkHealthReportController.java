package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkHealthReport;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkHealthReportGenerateRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/front/jk/health/report")
@Api(tags = "九州康健康周报月报")
public class JkHealthReportController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkHealthReportService reportService;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @PostMapping("/generate")
    @ApiOperation("基于本人真实健康记录生成周报或月报")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF, checkDataScope = true)
    public CommonResult<JkHealthReport> generate(@RequestBody @Validated JkHealthReportGenerateRequest request) {
        return CommonResult.success(reportService.generate(userId(), request));
    }

    @GetMapping("/list")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkHealthReport>> list(@RequestParam(required = false) String reportType,
                                                         PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(reportService.list(userId(), reportType, page)));
    }

    @GetMapping("/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF, checkDataScope = true)
    public CommonResult<JkHealthReport> detail(@PathVariable Long id) {
        return CommonResult.success(reportService.detail(userId(), id, false));
    }
}
