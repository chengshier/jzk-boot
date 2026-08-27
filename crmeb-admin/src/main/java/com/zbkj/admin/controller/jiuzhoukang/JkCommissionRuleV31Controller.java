package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRulePublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.impl.jiuzhoukang.commission.JkCommissionV31Service;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/commission-rule/v31")
@Api(tags = "九州康 V3.1 佣金规则")
public class JkCommissionRuleV31Controller {
    @Autowired private JkCommissionV31Service service;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/template/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_V31_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<List<JkCommissionRule>> templates(@RequestParam(required = false) String roleCode,
                                                           @RequestParam(required = false) String rewardType) {
        return CommonResult.success(service.templates(roleCode, rewardType));
    }

    @PostMapping("/trial")
    @ApiOperation("按业务快照试算规则，不入账")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_V31_TRIAL + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<List<Map<String, Object>>> trial(@RequestBody @Validated JkCommissionRuleTrialRequest request) {
        return CommonResult.success(service.trial(request));
    }

    @PostMapping("/publish")
    @ApiOperation("试算确认后发布，只影响生效时间之后的新业务")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_V31_PUBLISH + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<JkCommissionRule> publish(@RequestBody @Validated JkCommissionRulePublishRequest request) {
        return CommonResult.success(service.publish(request, operator()));
    }

    @PostMapping("/disable")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_V31_PUBLISH + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<JkCommissionRule> disable(@RequestParam Long ruleId, @RequestParam String reason) {
        return CommonResult.success(service.disable(ruleId, operator(), reason));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) return -Long.valueOf(actorService.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
