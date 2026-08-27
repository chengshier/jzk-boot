package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.model.user.User;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台业务人员仅管理入口可见性，不能借此创建九州康上下级关系。 */
@RestController
@RequestMapping("api/admin/jk/entry-access")
@Api(tags = "九州康业务入口授权")
public class JkEntryAccessAdminController {
    @Autowired private UserService userService;

    @PostMapping("/{userId}")
    @ApiOperation("开通或关闭用户九州康业务入口")
    public CommonResult<Boolean> setAccess(@PathVariable Integer userId, @RequestParam Boolean enabled) {
        User user = userService.getById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        user.setJkEntryAccess(enabled);
        return CommonResult.success(userService.updateById(user));
    }
}
