package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkStockCheck;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckSubmitRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.impl.jiuzhoukang.stock.JkStockCheckService;
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
@RequestMapping("api/front/jk/stock-check")
public class JkStockCheckController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkStockCheckService service;
    private Long userId() { return Long.valueOf(token.getUserId()); }

    @PostMapping("/create")
    @JkBizPermission(value = JkV31PermissionCodes.STOCK_CHECK_SELF, checkDataScope = true)
    public CommonResult<JkStockCheck> create(@RequestBody @Validated JkStockCheckCreateRequest request) {
        return CommonResult.success(service.create(userId(), request, true));
    }

    @GetMapping("/list")
    @JkBizPermission(value = JkV31PermissionCodes.STOCK_CHECK_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkStockCheck>> list(@RequestParam(required = false) String status, PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.list(userId(), null, status, page)));
    }

    @GetMapping("/{id}")
    @JkBizPermission(value = JkV31PermissionCodes.STOCK_CHECK_SELF, checkDataScope = true)
    public CommonResult<JkStockCheck> detail(@PathVariable Long id) {
        return CommonResult.success(service.detail(id, userId(), true));
    }

    @PostMapping("/submit")
    @JkBizPermission(value = JkV31PermissionCodes.STOCK_CHECK_SELF, checkDataScope = true)
    public CommonResult<JkStockCheck> submit(@RequestBody @Validated JkStockCheckSubmitRequest request) {
        return CommonResult.success(service.submit(userId(), request, true));
    }
}
