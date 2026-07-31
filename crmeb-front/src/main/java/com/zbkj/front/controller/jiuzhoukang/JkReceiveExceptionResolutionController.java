package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkReceiveExceptionResolution;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.trade.JkReceiveExceptionResolutionService;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeReceiveExceptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 收货人查看异常收货 V2 处理进展。 */
@RestController
@RequestMapping("api/front/jk/receive-exception-resolution")
@Api(tags = "九州康异常收货V2进展")
public class JkReceiveExceptionResolutionController {
    @Autowired private JkReceiveExceptionResolutionService service;
    @Autowired private JkTradeReceiveExceptionService exceptionService;
    @Autowired private FrontTokenComponent token;

    @GetMapping("/list")
    @ApiOperation("查询本人异常单处理方案")
    public CommonResult<List<JkReceiveExceptionResolution>> list(@RequestParam Long exceptionId) {
        exceptionService.detailMine(Long.valueOf(token.getUserId()), exceptionId);
        return CommonResult.success(service.list(exceptionId));
    }
}
