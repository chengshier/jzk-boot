package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkPromotionEffectEvent;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.promotion.JkPromotionEffectService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/promotion-effect")
@Api(tags = "九州康推广效果统计")
public class JkPromotionEffectAdminController {
    @Autowired private JkPromotionEffectService service;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PROMOTION_EFFECT_VIEW + "')")
    public CommonResult<Map<String, Object>> summary(@RequestParam(required = false) String sceneCode,
                                                      @RequestParam(required = false) Long promoterUserId,
                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        return CommonResult.success(service.summary(sceneCode, promoterUserId, startTime, endTime));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PROMOTION_EFFECT_VIEW + "')")
    public CommonResult<List<JkPromotionEffectEvent>> list(@RequestParam(required = false) String sceneCode,
                                                            @RequestParam(required = false) Long promoterUserId,
                                                            @RequestParam(required = false) String eventType,
                                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        return CommonResult.success(service.list(sceneCode, promoterUserId, eventType, startTime, endTime));
    }
}
