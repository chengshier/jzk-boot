package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkStockCheck;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCountRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCreateRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.stock.JkStockCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/jk/stock-check")
public class JkStockCheckController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkStockCheckService service;
    private Long userId(){return Long.valueOf(token.getUserId());}

    @PostMapping("/create")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_VIEW_SELF,checkDataScope=true)
    public CommonResult<JkStockCheck> create(@RequestBody @Validated JkStockCheckCreateRequest request){return CommonResult.success(service.create(userId(),request,false));}

    @PostMapping("/{id}/count")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_VIEW_SELF,checkDataScope=true)
    public CommonResult<JkStockCheck> count(@PathVariable Long id,@RequestBody @Validated JkStockCheckCountRequest request){return CommonResult.success(service.count(userId(),id,request,false));}

    @PostMapping("/{id}/submit")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_VIEW_SELF,checkDataScope=true)
    public CommonResult<JkStockCheck> submit(@PathVariable Long id,@RequestBody @Validated JkStockCheckActionRequest request){return CommonResult.success(service.submit(userId(),id,request,false));}

    @GetMapping("/list")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_VIEW_SELF,checkDataScope=true)
    public CommonResult<CommonPage<JkStockCheck>> list(@RequestParam(required=false) String status,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.list(userId(),status,page)));}

    @GetMapping("/{id}")
    @JkBizPermission(value=JkBizPermissionCodes.STOCK_VIEW_SELF,checkDataScope=true)
    public CommonResult<JkStockCheck> detail(@PathVariable Long id){return CommonResult.success(service.detail(userId(),id,false));}
}
