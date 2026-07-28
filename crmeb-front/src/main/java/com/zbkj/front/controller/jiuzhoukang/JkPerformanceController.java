package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("api/front/jk/performance")
public class JkPerformanceController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkPerformanceService performanceService;
    private Long userId() { return Long.valueOf(token.getUserId()); }

    @GetMapping("/summary")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_VIEW_SELF, checkDataScope = true)
    public CommonResult<Map<String, BigDecimal>> summary() {
        Map<String, BigDecimal> result = new LinkedHashMap<String, BigDecimal>();
        result.put("total", performanceService.summary(userId(), null));
        result.put("onlineRetail", performanceService.summary(userId(), "RETAIL_ONLINE"));
        result.put("offlineRetail", performanceService.summary(userId(), "RETAIL_OFFLINE"));
        result.put("platformPurchase", performanceService.summary(userId(), "PLATFORM_PURCHASE"));
        result.put("stockTransfer", performanceService.summary(userId(), "STOCK_TRANSFER"));
        return CommonResult.success(result);
    }

    @GetMapping("/list")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_VIEW_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkPerformanceRecord>> list(@RequestParam(required = false) String performanceType,
                                                               @RequestParam(required = false) String sourceType,
                                                               @RequestParam(required = false) String status,
                                                               PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(performanceService.list(userId(), performanceType, sourceType, status, page)));
    }
}
