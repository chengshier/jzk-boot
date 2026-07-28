package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRuleItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleItemSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRulePublishRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.response.jiuzhoukang.JkCommissionRuleTrialResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
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

@RestController
@RequestMapping("api/admin/jk/commission/rule")
@Api(tags = "九州康收益奖励规则")
public class JkCommissionRuleController {
    @Autowired private CommissionRuleService commissionRuleService;
    @Autowired private CommissionScenarioService scenarioService;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<List<JkCommissionRule>> list(@RequestParam(required = false) String sourceType,
                                                      @RequestParam(required = false) String receiverRoleCode,
                                                      @RequestParam(required = false) Boolean status) {
        return CommonResult.success(commissionRuleService.listRules(sourceType, receiverRoleCode, status));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_SAVE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("保存佣金规则草稿；不会直接启用")
    public CommonResult<JkCommissionRule> save(@RequestBody @Validated JkCommissionRuleSaveRequest request) {
        return CommonResult.success(commissionRuleService.saveRule(request));
    }

    @PostMapping("/trial")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_TRIAL + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("使用模拟业务快照试算已发布规则")
    public CommonResult<List<JkCommissionRuleTrialResponse>> trial(@RequestBody @Validated JkCommissionRuleTrialRequest request) {
        return CommonResult.success(scenarioService.trial(request));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_PUBLISH + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("审核并发布佣金规则")
    public CommonResult<JkCommissionRule> publish(@RequestBody @Validated JkCommissionRulePublishRequest request) {
        return CommonResult.success(commissionRuleService.publish(request, operator()));
    }

    @PostMapping("/disable")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_PUBLISH + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("停用已发布规则；只影响停用后的新业务")
    public CommonResult<JkCommissionRule> disable(@RequestParam Long id, @RequestParam String reason) {
        return CommonResult.success(commissionRuleService.disable(id, reason, operator()));
    }

    @GetMapping("/item/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<List<JkCommissionRuleItem>> itemList(@RequestParam Long ruleId) {
        return CommonResult.success(commissionRuleService.listItems(ruleId));
    }

    @PostMapping("/item/save")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_SAVE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<JkCommissionRuleItem> saveItem(@RequestBody @Validated JkCommissionRuleItemSaveRequest request) {
        return CommonResult.success(commissionRuleService.saveItem(request));
    }

    @PostMapping("/item/updateStatus")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_STATUS + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<Boolean> updateItemStatus(@RequestParam Long id, @RequestParam boolean status) {
        return CommonResult.success(commissionRuleService.updateItemStatus(id, status));
    }

    @PostMapping("/updateStatus")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_STATUS + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("兼容旧停用接口；草稿不能通过此接口启用")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam boolean status) {
        return CommonResult.success(commissionRuleService.updateStatus(id, status));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) return -Long.valueOf(actorService.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
