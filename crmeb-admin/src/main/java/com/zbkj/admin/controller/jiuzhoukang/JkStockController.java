package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockAccountSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockFlowSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockItemSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockAccountResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockFlowResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.stock.StockAccountService;
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
@RequestMapping("api/admin/jk/stock")
@Api(tags = "九州康库存底座")
public class JkStockController {

    @Autowired
    private StockAccountService stockAccountService;

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_ACCOUNT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_ALL, checkDataScope = false)
    @GetMapping("/account/list")
    @ApiOperation("库存账户列表")
    public CommonResult<CommonPage<JkStockAccountResponse>> accountList(@Validated JkStockAccountSearchRequest request,
                                                                        @Validated PageParamRequest pageParamRequest) {
        List<JkStockAccountResponse> rows = stockAccountService.getAdminAccountList(request, pageParamRequest);
        return CommonResult.success(CommonPage.restPage(rows));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_ITEM_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_ALL, checkDataScope = false)
    @GetMapping("/item/list")
    @ApiOperation("库存明细列表")
    public CommonResult<CommonPage<JkStockItemResponse>> itemList(@Validated JkStockItemSearchRequest request,
                                                                  @Validated PageParamRequest pageParamRequest) {
        List<JkStockItemResponse> rows = stockAccountService.getAdminItemList(request, pageParamRequest);
        return CommonResult.success(CommonPage.restPage(rows));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_STOCK_FLOW_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_ALL, checkDataScope = false)
    @GetMapping("/flow/list")
    @ApiOperation("库存流水列表")
    public CommonResult<CommonPage<JkStockFlowResponse>> flowList(@Validated JkStockFlowSearchRequest request,
                                                                  @Validated PageParamRequest pageParamRequest) {
        List<JkStockFlowResponse> rows = stockAccountService.getAdminFlowList(request, pageParamRequest);
        return CommonResult.success(CommonPage.restPage(rows));
    }
}
