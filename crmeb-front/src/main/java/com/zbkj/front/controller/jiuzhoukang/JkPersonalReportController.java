package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.response.jiuzhoukang.JkPersonalReportResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.report.JkPersonalReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.util.Date;

/** App 个人经营中心。用户 ID 始终从前台 token 获取，不能查询他人经营数据。 */
@RestController
@RequestMapping("api/front/jk/report")
public class JkPersonalReportController {
    @Autowired private JkPersonalReportService service;
    @Autowired private FrontTokenComponent frontTokenComponent;

    @GetMapping("/summary")
    @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<JkPersonalReportResponse> summary(
            @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd") Date startDate,
            @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd") Date endDate) {
        return CommonResult.success(service.summary(Long.valueOf(frontTokenComponent.getUserId()),startDate,endDate));
    }
}
