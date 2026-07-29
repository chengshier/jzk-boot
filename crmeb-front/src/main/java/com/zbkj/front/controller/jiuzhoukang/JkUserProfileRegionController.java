package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.request.jiuzhoukang.JkUserProfileRegionSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkRegionOptionResponse;
import com.zbkj.common.response.jiuzhoukang.JkUserProfileRegionResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.profile.JkUserProfileRegionService;
import com.zbkj.service.service.jiuzhoukang.region.JkRegionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 普通用户可直接使用，不要求创建代理身份。 */
@RestController
@RequestMapping("api/front/jk")
@Api(tags = "九州康用户个人资料区域")
public class JkUserProfileRegionController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkUserProfileRegionService profileRegionService;
    @Autowired private JkRegionService regionService;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @GetMapping("/user-profile/region")
    @ApiOperation("读取现有个人资料中的标准所在地区")
    public CommonResult<JkUserProfileRegionResponse> get() {
        return CommonResult.success(profileRegionService.get(userId()));
    }

    @PostMapping("/user-profile/region")
    @ApiOperation("保存现有个人资料中的标准所在地区；不修改收货地址、不创建代理身份")
    public CommonResult<JkUserProfileRegionResponse> save(@RequestBody @Validated JkUserProfileRegionSaveRequest request) {
        return CommonResult.success(profileRegionService.saveByUser(userId(), request));
    }

    @GetMapping("/region/options")
    @ApiOperation("个人资料区域级联选项")
    public CommonResult<List<JkRegionOptionResponse>> options(@RequestParam(required = false) String parentRegionCode,
                                                               @RequestParam(required = false) Integer targetLevel,
                                                               @RequestParam(required = false) String keyword) {
        return CommonResult.success(regionService.listRegionOptions(parentRegionCode, targetLevel, true, keyword));
    }
}
