package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.response.jiuzhoukang.JkPromotionCodeResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.promotion.JkPromotionCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/front/jk/promotion-code")
@Api(tags = "九州康本人真实微信小程序码")
public class JkPromotionCodeController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkUserContextService contextService;
    @Autowired private JkPromotionCodeService service;

    @GetMapping("/generate")
    @ApiOperation("生成或读取本人真实微信小程序码")
    @JkBizPermission(value = JkBizPermissionCodes.TEAM_VIEW, checkDataScope = false)
    public CommonResult<JkPromotionCodeResponse> generate(@RequestParam(defaultValue = "AGENT_BIND") String sceneCode,
                                                            @RequestParam String requestNo) {
        Long userId = Long.valueOf(token.getUserId());
        JkUserContext context = contextService.getFrontContext(userId);
        if (context == null || Boolean.TRUE.equals(context.getFreezeStatus())) {
            throw new IllegalStateException("当前业务身份不可生成推广码");
        }
        return CommonResult.success(service.generate(userId, context.getPrimaryRoleCode(), sceneCode, requestNo));
    }
}
