package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelationChangeApply;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationChangeApplyRequest;
import com.zbkj.common.response.jiuzhoukang.JkPromotionQrcodeResponse;
import com.zbkj.common.response.jiuzhoukang.JkOptionResponse;
import java.util.List;
import com.zbkj.common.response.jiuzhoukang.JkTeamSummaryResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentRelationChangeService;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentTeamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/front/jk/team")
@Api(tags = "九州康团队与关系")
public class JkAgentTeamController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkAgentTeamService teamService;
    @Autowired private JkAgentRelationChangeService changeService;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @GetMapping("/summary")
    @JkBizPermission(value = JkBizPermissionCodes.TEAM_VIEW, checkDataScope = true)
    @ApiOperation("本人上级、直属团队和关系历史")
    public CommonResult<JkTeamSummaryResponse> summary() { return CommonResult.success(teamService.summary(userId())); }

    @GetMapping("/qrcode")
    @JkBizPermission(value = JkBizPermissionCodes.TEAM_VIEW, checkDataScope = false)
    @ApiOperation("本人推广二维码")
    public CommonResult<JkPromotionQrcodeResponse> qrcode() { return CommonResult.success(teamService.promotionQrcode(userId())); }


    @GetMapping("/relation-change/parent-options")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_CHANGE_APPLY, checkDataScope = true)
    @ApiOperation("搜索同区域可换绑上级")
    public CommonResult<List<JkOptionResponse>> parentOptions(@RequestParam String keyword,
                                                               @RequestParam(defaultValue = "20") int limit) {
        return CommonResult.success(teamService.eligibleParentOptions(userId(), keyword, limit));
    }

    @PostMapping("/relation-change/apply")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_CHANGE_APPLY, checkDataScope = true)
    @ApiOperation("提交换绑申请")
    public CommonResult<JkAgentRelationChangeApply> apply(@RequestBody @Validated JkAgentRelationChangeApplyRequest request) {
        return CommonResult.success(changeService.apply(userId(), request));
    }

    @GetMapping("/relation-change/list")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_CHANGE_APPLY, checkDataScope = true)
    public CommonResult<CommonPage<JkAgentRelationChangeApply>> list(PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(changeService.listMine(userId(), page)));
    }

    @GetMapping("/relation-change/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_CHANGE_APPLY, checkDataScope = true)
    public CommonResult<JkAgentRelationChangeApply> detail(@PathVariable Long id) {
        return CommonResult.success(changeService.detail(userId(), id, false));
    }

    @PostMapping("/relation-change/{id}/cancel")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_CHANGE_APPLY, checkDataScope = true)
    public CommonResult<JkAgentRelationChangeApply> cancel(@PathVariable Long id, @RequestParam String requestNo,
                                                            @RequestParam(required = false) String reason) {
        return CommonResult.success(changeService.cancel(userId(), id, requestNo, reason));
    }
}
