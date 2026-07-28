package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkRelationPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkRelationLimitRule;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationForceAdjustRequest;
import com.zbkj.common.request.jiuzhoukang.JkRelationLimitRuleSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;
import com.zbkj.common.response.jiuzhoukang.JkRelationQuotaResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentRelationService;
import com.zbkj.service.service.jiuzhoukang.region.JkRelationQuotaService;
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

/** 关系人数规则、额度查询和管理员强制调整。 */
@RestController
@RequestMapping("api/admin/jk/relation-quota")
@Api(tags = "九州康关系人数与强制调整")
public class JkRelationQuotaAdminController {

    @Autowired private JkRelationQuotaService quotaService;
    @Autowired private JkAgentRelationService relationService;
    @Autowired private JkAdminActorService actorService;

    @GetMapping("/rule/list")
    @ApiOperation("关系人数规则列表")
    @PreAuthorize("hasAuthority('" + JkRelationPermissionCodes.ADMIN_RELATION_LIMIT_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_MANAGE, checkDataScope = false)
    public CommonResult<CommonPage<JkRelationLimitRule>> ruleList(@RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) Boolean status,
                                                                   PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(quotaService.listRules(keyword, status, page)));
    }

    @PostMapping("/rule/save")
    @ApiOperation("保存关系人数规则")
    @PreAuthorize("hasAuthority('" + JkRelationPermissionCodes.ADMIN_RELATION_LIMIT_RULE_SAVE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_MANAGE, checkDataScope = false)
    public CommonResult<JkRelationLimitRule> saveRule(@RequestBody @Validated JkRelationLimitRuleSaveRequest request) {
        return CommonResult.success(quotaService.saveRule(request, operator()));
    }

    @PostMapping("/rule/status")
    @ApiOperation("启停关系人数规则")
    @PreAuthorize("hasAuthority('" + JkRelationPermissionCodes.ADMIN_RELATION_LIMIT_RULE_STATUS + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_MANAGE, checkDataScope = false)
    public CommonResult<JkRelationLimitRule> ruleStatus(@RequestParam Long id, @RequestParam Boolean status) {
        return CommonResult.success(quotaService.updateRuleStatus(id, status, operator()));
    }

    @GetMapping("/usage")
    @ApiOperation("查询指定上级当前额度")
    @PreAuthorize("hasAuthority('" + JkRelationPermissionCodes.ADMIN_RELATION_LIMIT_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_MANAGE, checkDataScope = true)
    public CommonResult<JkRelationQuotaResponse> usage(@RequestParam Long parentUserId,
                                                        @RequestParam(required = false) Long childUserId) {
        return CommonResult.success(quotaService.quota(parentUserId, childUserId));
    }

    @PostMapping("/force-adjust")
    @ApiOperation("管理员强制调整上下级关系")
    @PreAuthorize("hasAuthority('" + JkRelationPermissionCodes.ADMIN_AGENT_RELATION_FORCE_ADJUST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_MANAGE, checkDataScope = false)
    public CommonResult<JkAgentRelationResponse> forceAdjust(@RequestBody @Validated JkAgentRelationForceAdjustRequest request) {
        return CommonResult.success(relationService.forceAdjust(request, operator()));
    }

    private Long operator() {
        Long linked = actorService.getLinkedFrontUserId(actorService.getCurrentAdmin());
        if (linked != null) return linked;
        if (actorService.isPlatformSuperAdmin(actorService.getCurrentAdmin())) {
            return -Long.valueOf(actorService.getCurrentAdmin().getId());
        }
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
