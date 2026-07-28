package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleCreateRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.offline.JkOfflineSaleService;
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
@RequestMapping("api/front/jk/offline-sale")
@Api(tags = "九州康线下终端销售")
public class JkOfflineSaleController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkOfflineSaleService saleService;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @PostMapping("/create")
    @ApiOperation("登记线下终端销售")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    public CommonResult<JkOfflineSale> create(@RequestBody @Validated JkOfflineSaleCreateRequest request) {
        return CommonResult.success(saleService.create(userId(), request));
    }

    @GetMapping("/list")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkOfflineSale>> list(@RequestParam(required = false) String status, PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(saleService.list(userId(), status, page)));
    }

    @GetMapping("/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_SELF, checkDataScope = true)
    public CommonResult<JkOfflineSale> detail(@PathVariable Long id) {
        return CommonResult.success(saleService.detail(userId(), id, false));
    }

    @PostMapping("/{id}/cancel")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    public CommonResult<JkOfflineSale> cancel(@PathVariable Long id, @RequestBody @Validated JkOfflineSaleActionRequest request) {
        return CommonResult.success(saleService.cancel(userId(), id, request));
    }

    @PostMapping("/{id}/return")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    public CommonResult<JkOfflineSale> returnSale(@PathVariable Long id, @RequestBody @Validated JkOfflineSaleActionRequest request) {
        return CommonResult.success(saleService.returnSale(userId(), id, request));
    }
}
