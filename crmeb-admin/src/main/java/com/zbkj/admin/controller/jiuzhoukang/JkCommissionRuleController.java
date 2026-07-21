package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRuleItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleItemSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleSaveRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/admin/jk/commission/rule")
@Api(tags = "九州康分佣规则管理")
public class JkCommissionRuleController {
    @Autowired
    private CommissionRuleService commissionRuleService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("分佣规则列表")
    public CommonResult<List<JkCommissionRule>> list(@RequestParam(required = false) String sourceType,
                                                     @RequestParam(required = false) String receiverRoleCode,
                                                     @RequestParam(required = false) Boolean status) {
        return CommonResult.success(commissionRuleService.listRules(sourceType, receiverRoleCode, status));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_SAVE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("新增或编辑分佣规则")
    public CommonResult<JkCommissionRule> save(@RequestBody JkCommissionRuleSaveRequest request) {
        return CommonResult.success(commissionRuleService.saveRule(request));
    }

    @GetMapping("/item/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("分佣规则明细列表")
    public CommonResult<List<JkCommissionRuleItem>> itemList(@RequestParam Long ruleId) {
        return CommonResult.success(commissionRuleService.listItems(ruleId));
    }

    @PostMapping("/item/save")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_SAVE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("新增或编辑分佣规则明细")
    public CommonResult<JkCommissionRuleItem> saveItem(@RequestBody JkCommissionRuleItemSaveRequest request) {
        return CommonResult.success(commissionRuleService.saveItem(request));
    }

    @PostMapping("/item/updateStatus")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_STATUS + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("启用或禁用分佣规则明细")
    public CommonResult<Boolean> updateItemStatus(@RequestParam Long id, @RequestParam boolean status) {
        return CommonResult.success(commissionRuleService.updateItemStatus(id, status));
    }

    @PostMapping("/updateStatus")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RULE_STATUS + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RULE_MANAGE, checkDataScope = false)
    @ApiOperation("启用或禁用分佣规则")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam boolean status) {
        return CommonResult.success(commissionRuleService.updateStatus(id, status));
    }
}
