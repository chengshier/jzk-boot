package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRulePublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.response.jiuzhoukang.JkCommissionRuleTrialResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import com.zbkj.service.service.jiuzhoukang.commission.JkCommissionTemplateService;
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
    @Autowired private JkCommissionTemplateService templateService;
    @Autowired private CommissionScenarioService scenarioService;
    @Autowired private CommissionRuleService ruleService;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/template/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<List<Map<String, Object>>> templates(@RequestParam(required = false) String roleCode,
                                                              @RequestParam(required = false) String rewardType) {
        // 正式模板服务按受益身份返回业务模板；rewardType 为旧接口兼容参数，不再参与底层规则筛选。
        return CommonResult.success(templateService.templates(roleCode));
    }

    @PostMapping("/trial")
    @ApiOperation("按业务快照试算规则，不入账")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_TRIAL + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<List<JkCommissionRuleTrialResponse>> trial(@RequestBody @Validated JkCommissionRuleTrialRequest request) {
        return CommonResult.success(scenarioService.trial(request));
    }

    @PostMapping("/publish")
    @ApiOperation("试算确认后发布，只影响生效时间之后的新业务")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_PUBLISH + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<JkCommissionRule> publish(@RequestBody @Validated JkCommissionRulePublishRequest request) {
        return CommonResult.success(ruleService.publish(request, operator()));
    }

    @PostMapping("/disable")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_PUBLISH + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<JkCommissionRule> disable(@RequestParam Long ruleId, @RequestParam String reason) {
        return CommonResult.success(ruleService.disable(ruleId, reason, operator()));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) return -Long.valueOf(actorService.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
