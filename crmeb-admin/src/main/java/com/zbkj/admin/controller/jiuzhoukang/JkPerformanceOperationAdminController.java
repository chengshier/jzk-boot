package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkOperationProfitLedgerService;
import com.zbkj.service.service.impl.jiuzhoukang.performance.JkPerformanceLedgerService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin/jk")
@Api(tags = "九州康业绩与经营收益管理")
public class JkPerformanceOperationAdminController {
    @Autowired private JkPerformanceLedgerService performanceService;
    @Autowired private JkOperationProfitLedgerService profitService;

    @GetMapping("/performance/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PERFORMANCE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_ACCOUNT_VIEW, checkDataScope = true)
    public CommonResult<CommonPage<JkPerformanceRecord>> performance(@RequestParam(required = false) Long ownerUserId,
                                                                      @RequestParam(required = false) String performanceType,
                                                                      @RequestParam(required = false) String sourceType,
                                                                      @RequestParam(required = false) String status,
                                                                      PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(performanceService.list(ownerUserId, performanceType, sourceType, status, page)));
    }

    @GetMapping("/operation-profit/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_OPERATION_PROFIT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_ACCOUNT_VIEW, checkDataScope = true)
    public CommonResult<CommonPage<JkOperationProfitRecord>> profit(@RequestParam(required = false) Long userId,
                                                                     @RequestParam(required = false) String sourceType,
                                                                     @RequestParam(required = false) String status,
                                                                     PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(profitService.list(userId, sourceType, status, page)));
    }
}
