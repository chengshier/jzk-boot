package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.impl.jiuzhoukang.promotion.JkPromotionSceneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/front/jk/promotion/v31")
public class JkPromotionV31Controller {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkPromotionSceneService service;

    @GetMapping("/qrcode")
    @JkBizPermission(value = JkBizPermissionCodes.TEAM_VIEW, checkDataScope = false)
    public CommonResult<Map<String, Object>> qrcode(@RequestParam(defaultValue = "false") boolean forceRefresh) {
        return CommonResult.success(service.qrcode(Long.valueOf(token.getUserId()), forceRefresh));
    }

    /** 小程序启动时解析随机 scene，并记录扫码；不直接暴露用户ID。 */
    @PostMapping("/scene/resolve")
    public CommonResult<Map<String, Object>> resolve(@RequestParam String scene) {
        return CommonResult.success(service.resolve(scene));
    }
}
