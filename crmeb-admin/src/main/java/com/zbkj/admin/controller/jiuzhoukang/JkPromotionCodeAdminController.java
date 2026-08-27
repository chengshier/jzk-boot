package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkPromotionScene;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkPromotionSceneSaveRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.promotion.JkPromotionCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/promotion-code")
@Api(tags = "九州康真实微信推广码")
public class JkPromotionCodeAdminController {
    @Autowired private JkPromotionCodeService service;
    @Autowired private JkAdminActorService actor;

    @GetMapping("/scene/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PROMOTION_SCENE_MANAGE + "')")
    public CommonResult<CommonPage<JkPromotionScene>> list(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Boolean status,
                                                            PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.listScenes(keyword, status, page)));
    }

    @PostMapping("/scene/save")
    @ApiOperation("保存推广场景；默认关闭，启用前必须确认微信与私有存储配置")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PROMOTION_SCENE_MANAGE + "')")
    public CommonResult<JkPromotionScene> save(@RequestBody @Validated JkPromotionSceneSaveRequest request) {
        Long linked = actor.getLinkedFrontUserId(actor.getCurrentAdmin());
        Long operator = linked != null ? linked : -Long.valueOf(actor.getCurrentAdmin().getId());
        return CommonResult.success(service.saveScene(request, operator));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_PROMOTION_SCENE_MANAGE + "')")
    public CommonResult<Map<String, Object>> status() {
        return CommonResult.success(service.status());
    }
}
