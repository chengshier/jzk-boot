package com.zbkj.front.controller.jiuzhoukang;

import cn.hutool.json.JSONUtil;
import com.zbkj.common.model.jiuzhoukang.JkPromotionEffectEvent;
import com.zbkj.common.request.jiuzhoukang.JkPromotionOpenEventRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.promotion.JkPromotionEffectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/front/jk/promotion-effect")
@Api(tags = "九州康推广效果采集")
public class JkPromotionEffectFrontController {
    @Autowired private JkPromotionEffectService service;

    @PostMapping("/open")
    @ApiOperation("记录小程序推广场景打开；客户端不能上报成交事件")
    public CommonResult<JkPromotionEffectEvent> open(@RequestBody @Validated JkPromotionOpenEventRequest request) {
        return CommonResult.success(service.recordOpen(request.getSceneCode(), null, request.getRequestNo(),
                JSONUtil.toJsonStr(request)));
    }
}
