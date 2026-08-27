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
import com.zbkj.common.request.jiuzhoukang.JkCommissionSourceTrialRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionTemplateSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkCommissionRuleTrialResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import com.zbkj.service.service.jiuzhoukang.commission.JkCommissionSourceTrialService;
import com.zbkj.service.service.jiuzhoukang.commission.JkCommissionTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/commission/rule")
@Api(tags = "九州康收益奖励规则")
public class JkCommissionRuleController {
    @Autowired private CommissionRuleService commissionRuleService;
    @Autowired private CommissionScenarioService scenarioService;
    @Autowired private JkCommissionTemplateService templateService;
    @Autowired private JkCommissionSourceTrialService sourceTrialService;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    public CommonResult<List<JkCommissionRule>> list(@RequestParam(required = false) String sourceType,
                                                       @RequestParam(required = false) String receiverRoleCode,
                                                       @RequestParam(required = false) Boolean status) {
        return CommonResult.success(commissionRuleService.listRules(sourceType, receiverRoleCode, status));
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_LIST + "')")
    @ApiOperation("运营业务奖励模板；技术枚举只在高级详情中展示")
    public CommonResult<List<Map<String, Object>>> templates(@RequestParam(required = false) String receiverRoleCode) {
        return CommonResult.success(templateService.templates(receiverRoleCode));
    }

    @PostMapping("/template/save")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_SAVE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("从业务模板保存规则草稿；新规则始终默认关闭")
    public CommonResult<JkCommissionRule> saveTemplate(@RequestBody @Validated JkCommissionTemplateSaveRequest request) {
        return CommonResult.success(templateService.saveDraft(request));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_ADVANCED + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("高级规则管理员保存底层技术规则草稿；普通运营不授权")
    public CommonResult<JkCommissionRule> save(@RequestBody @Validated JkCommissionRuleSaveRequest request) {
        return CommonResult.success(commissionRuleService.saveRule(request));
    }

    @PostMapping("/trial/source")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_TRIAL + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("按真实订单、线下销售、订货、调拨或业绩记录加载快照试算")
    public CommonResult<Map<String, Object>> trialSource(@RequestBody @Validated JkCommissionSourceTrialRequest request) {
        return CommonResult.success(sourceTrialService.trial(request));
    }

    @PostMapping("/trial")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_ADVANCED + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("高级规则管理员手工快照试算；普通运营页面不调用")
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
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_ADVANCED + "')")
    public CommonResult<List<JkCommissionRuleItem>> itemList(@RequestParam Long ruleId) {
        return CommonResult.success(commissionRuleService.listItems(ruleId));
    }

    @PostMapping("/item/save")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_ADVANCED + "')")
    public CommonResult<JkCommissionRuleItem> saveItem(@RequestBody @Validated JkCommissionRuleItemSaveRequest request) {
        return CommonResult.success(commissionRuleService.saveItem(request));
    }

    @PostMapping("/item/updateStatus")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_ADVANCED + "')")
    public CommonResult<Boolean> updateItemStatus(@RequestParam Long id, @RequestParam boolean status) {
        return CommonResult.success(commissionRuleService.updateItemStatus(id, status));
    }

    @PostMapping("/updateStatus")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_COMMISSION_RULE_PUBLISH + "')")
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
