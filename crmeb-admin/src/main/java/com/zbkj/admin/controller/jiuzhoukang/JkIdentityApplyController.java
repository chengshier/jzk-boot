package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplyAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplySearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.identity.JkIdentityApplyService;
import com.zbkj.service.service.jiuzhoukang.wechat.JkSubscriptionBusinessNotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/admin/jk/identity/apply")
@Api(tags = "九州康身份申请审核")
public class JkIdentityApplyController {

    @Autowired
    private JkIdentityApplyService identityApplyService;
    @Autowired
    private JkSubscriptionBusinessNotificationService notificationService;

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_IDENTITY_APPLY_LIST + "')")
    @JkBizPermission(checkDataScope = true)
    @ApiOperation("身份申请列表")
    @GetMapping("/list")
    public CommonResult<CommonPage<JkIdentityApplyResponse>> list(@Validated JkIdentityApplySearchRequest request,
                                                                   @Validated PageParamRequest pageParamRequest) {
        List<JkIdentityApplyResponse> rows = identityApplyService.getAdminApplyList(request, pageParamRequest);
        return CommonResult.success(CommonPage.restPage(rows));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_IDENTITY_APPLY_AUDIT + "')")
    @JkBizPermission(value = JkBizPermissionCodes.IDENTITY_APPLY_AUDIT, checkDataScope = true)
    @ApiOperation("审核身份申请")
    @PostMapping("/audit")
    public CommonResult<Boolean> audit(@RequestBody @Validated JkIdentityApplyAuditRequest request) {
        Boolean result = identityApplyService.auditApply(request);
        if (Boolean.TRUE.equals(result)) {
            JkIdentityApply apply = identityApplyService.getById(request.getApplyId());
            if (apply != null) {
                boolean approved = JkBizConstants.AUDIT_ACTION_PASS.equals(request.getAuditAction());
                String remark = approved ? request.getAuditRemark() : request.getRejectReason();
                notificationService.notifyAuditResult(
                        JkBizConstants.BUSINESS_TYPE_IDENTITY_APPLY,
                        apply.getId(),
                        apply.getApplyNo(),
                        apply.getUserId(),
                        apply.getApplyRoleCode() + "身份申请",
                        approved ? "已通过" : "已驳回",
                        remark,
                        "pages/jk/identity/applyDetail?id=" + apply.getId());
            }
        }
        return CommonResult.success(result);
    }
}
