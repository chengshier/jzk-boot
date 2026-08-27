package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkReceiveExceptionConfirmRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeReceiveExceptionCreateRequest;
import com.zbkj.common.response.jiuzhoukang.JkTradeReceiveExceptionDetailResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.impl.jiuzhoukang.trade.JkReceiveExceptionV2Service;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeReceiveExceptionService;
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

/** 前台订货/调拨异常收货上报及 V2 方案确认。 */
@RestController
@RequestMapping("api/front/jk/receive-exception")
@Api(tags = "九州康异常收货")
public class JkTradeReceiveExceptionController {
    @Autowired private JkTradeReceiveExceptionService service;
    @Autowired private JkReceiveExceptionV2Service v2Service;
    @Autowired private FrontTokenComponent token;

    @PostMapping("/report")
    @ApiOperation("上报异常收货并阻断正常入库")
    public CommonResult<JkTradeReceiveExceptionDetailResponse> report(@RequestBody @Validated JkTradeReceiveExceptionCreateRequest request) {
        return CommonResult.success(service.create(userId(), request));
    }

    @GetMapping("/list")
    @ApiOperation("我的异常收货记录")
    public CommonResult<CommonPage<JkTradeReceiveException>> list(@RequestParam(required = false) String status, PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.listMine(userId(), status, page)));
    }

    @GetMapping("/{id}")
    @ApiOperation("我的异常收货详情")
    public CommonResult<JkTradeReceiveExceptionDetailResponse> detail(@PathVariable Long id) {
        return CommonResult.success(service.detailMine(userId(), id));
    }

    @GetMapping("/business")
    @ApiOperation("按业务单查询最新异常收货记录")
    public CommonResult<JkTradeReceiveExceptionDetailResponse> detailByBusiness(@RequestParam String businessType,
                                                                                 @RequestParam Long businessId) {
        return CommonResult.success(service.detailByBusiness(userId(), businessType, businessId));
    }

    @PostMapping("/resolution/receiver-confirm")
    @ApiOperation("收货方确认或拒绝 V2 处理方案")
    public CommonResult<JkTradeReceiveExceptionDetailResponse> confirmReceiver(@RequestBody @Validated JkReceiveExceptionConfirmRequest request) {
        return CommonResult.success(v2Service.confirmReceiver(userId(), request));
    }

    @PostMapping("/resolution/sender-confirm")
    @ApiOperation("调拨发货方确认或拒绝 V2 处理方案")
    public CommonResult<JkTradeReceiveExceptionDetailResponse> confirmSender(@RequestBody @Validated JkReceiveExceptionConfirmRequest request) {
        return CommonResult.success(v2Service.confirmSender(userId(), request));
    }

    private Long userId() { return Long.valueOf(token.getUserId()); }
}
