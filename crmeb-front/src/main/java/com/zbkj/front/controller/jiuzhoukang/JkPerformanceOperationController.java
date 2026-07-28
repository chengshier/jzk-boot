package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkOperationProfitLedgerService;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkPerformanceLedgerService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("api/front/jk")
@Api(tags = "九州康个人业绩与经营收益")
public class JkPerformanceOperationController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkPerformanceLedgerService performanceService;
    @Autowired private JkOperationProfitLedgerService profitService;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @GetMapping("/performance/summary")
    @JkBizPermission(value = JkV31PermissionCodes.PERFORMANCE_VIEW_SELF, checkDataScope = true)
    public CommonResult<Map<String, BigDecimal>> performanceSummary() {
        Map<String, BigDecimal> result = new LinkedHashMap<String, BigDecimal>();
        result.put("validPerformanceAmount", performanceService.validAmount(userId()));
        return CommonResult.success(result);
    }

    @GetMapping("/performance/list")
    @JkBizPermission(value = JkV31PermissionCodes.PERFORMANCE_VIEW_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkPerformanceRecord>> performanceList(@RequestParam(required = false) String performanceType,
                                                                          @RequestParam(required = false) String sourceType,
                                                                          @RequestParam(required = false) String status,
                                                                          PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(performanceService.list(userId(), performanceType, sourceType, status, page)));
    }

    @GetMapping("/operation-profit/summary")
    @JkBizPermission(value = JkV31PermissionCodes.OPERATION_PROFIT_VIEW_SELF, checkDataScope = true)
    public CommonResult<Map<String, Object>> profitSummary() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("incomeNature", "OFFLINE_REALIZED");
        result.put("confirmedProfitAmount", profitService.confirmedProfit(userId()));
        result.put("withdrawable", false);
        result.put("description", "线下已实现，不进入平台提现账户");
        return CommonResult.success(result);
    }

    @GetMapping("/operation-profit/list")
    @JkBizPermission(value = JkV31PermissionCodes.OPERATION_PROFIT_VIEW_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkOperationProfitRecord>> profitList(@RequestParam(required = false) String sourceType,
                                                                         @RequestParam(required = false) String status,
                                                                         PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(profitService.list(userId(), sourceType, status, page)));
    }
}
