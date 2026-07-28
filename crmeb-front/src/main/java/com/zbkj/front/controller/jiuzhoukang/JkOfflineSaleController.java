package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleReturnRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.impl.jiuzhoukang.trade.JkOfflineSaleService;
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
    @Autowired private JkOfflineSaleService service;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @PostMapping("/create")
    @ApiOperation("登记并确认或提交审核线下销售")
    @JkBizPermission(value = JkV31PermissionCodes.OFFLINE_SALE_MANAGE_SELF, checkDataScope = true)
    public CommonResult<JkOfflineSale> create(@RequestBody @Validated JkOfflineSaleCreateRequest request) {
        return CommonResult.success(service.create(userId(), request));
    }

    @GetMapping("/list")
    @JkBizPermission(value = JkV31PermissionCodes.OFFLINE_SALE_MANAGE_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkOfflineSale>> list(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String auditStatus,
                                                         PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.list(userId(), status, auditStatus, page)));
    }

    @GetMapping("/{id}")
    @JkBizPermission(value = JkV31PermissionCodes.OFFLINE_SALE_MANAGE_SELF, checkDataScope = true)
    public CommonResult<JkOfflineSale> detail(@PathVariable Long id) {
        return CommonResult.success(service.detailMine(userId(), id));
    }

    @PostMapping("/{id}/cancel")
    @JkBizPermission(value = JkV31PermissionCodes.OFFLINE_SALE_MANAGE_SELF, checkDataScope = true)
    public CommonResult<JkOfflineSale> cancel(@PathVariable Long id, @RequestParam String reason) {
        return CommonResult.success(service.cancel(userId(), id, reason));
    }

    @PostMapping("/{id}/return")
    @JkBizPermission(value = JkV31PermissionCodes.OFFLINE_SALE_MANAGE_SELF, checkDataScope = true)
    public CommonResult<JkOfflineSale> returnSale(@PathVariable Long id,
                                                   @RequestBody @Validated JkOfflineSaleReturnRequest request) {
        return CommonResult.success(service.returnSale(userId(), id, request));
    }
}
