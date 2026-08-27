package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.model.user.User;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.promotion.JkPromotionSceneSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/front/jk/entry-access")
@Api(tags = "九州康业务入口开通")
public class JkEntryAccessController {
    @Autowired private FrontTokenComponent token;
    @Autowired private UserService userService;

    @PostMapping("/activate")
    @ApiOperation("固定入口码开通业务入口")
    public CommonResult<Boolean> activate(@RequestParam String scene) {
        JkPromotionSceneSupport.SceneEntry entry = JkPromotionSceneSupport.parse(scene);
        User user = userService.getById(token.getUserId());
        if (user == null) throw new IllegalArgumentException("用户不存在");
        // 固定入口码和九州康推广码都只开入口；推广码的上下级关系仍须等身份审核通过。
        user.setJkEntryAccess(true);
        return CommonResult.success(userService.updateById(user));
    }
}
