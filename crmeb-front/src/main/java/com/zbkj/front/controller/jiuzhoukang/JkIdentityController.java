package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkIdentityApplyRequest;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyResponse;
import com.zbkj.common.response.jiuzhoukang.JkIdentityApplyDetailResponse;
import com.zbkj.common.response.jiuzhoukang.JkPermissionContextResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.identity.JkIdentityApplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/front/jk/identity")
@Api(tags = "九州康身份申请")
public class JkIdentityController {

    @Autowired
    private JkIdentityApplyService identityApplyService;
    @Autowired
    private FrontTokenComponent frontTokenComponent;
    @Autowired
    private JkUserContextService userContextService;
    @Autowired
    private JkRegionDao regionDao;

    @ApiOperation("提交身份申请")
    @PostMapping("/apply")
    @JkBizPermission(value = JkBizPermissionCodes.IDENTITY_APPLY_SUBMIT, checkDataScope = false)
    public CommonResult<JkIdentityApplyResponse> apply(@RequestBody @Validated JkIdentityApplyRequest request) {
        Long userId = Long.valueOf(frontTokenComponent.getUserId());
        return CommonResult.success(identityApplyService.submitApply(userId, request));
    }

    @ApiOperation("我的申请记录")
    @GetMapping("/apply/list")
    public CommonResult<List<JkIdentityApplyResponse>> applyList(@Validated PageParamRequest pageParamRequest) {
        Long userId = Long.valueOf(frontTokenComponent.getUserId());
        return CommonResult.success(identityApplyService.getMyApplyList(userId, pageParamRequest));
    }

    @ApiOperation("我的申请详情")
    @GetMapping("/apply/{applyId}")
    public CommonResult<JkIdentityApplyDetailResponse> applyDetail(@PathVariable("applyId") Long applyId) {
        Long userId = Long.valueOf(frontTokenComponent.getUserId());
        return CommonResult.success(identityApplyService.getMyApplyDetail(userId, applyId));
    }

    @ApiOperation("身份申请区域选项")
    @GetMapping("/region/options")
    public CommonResult<List<JkRegion>> regionOptions() {
        return CommonResult.success(regionDao.selectList(null).stream()
                .filter(region -> !Boolean.TRUE.equals(region.getIsDeleted()))
                .filter(region -> Boolean.TRUE.equals(region.getStatus()))
                .sorted(Comparator.comparing(JkRegion::getRegionLevel, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(JkRegion::getRegionCode, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList()));
    }

    @ApiOperation("当前身份状态")
    @GetMapping("/status")
    public CommonResult<JkPermissionContextResponse> status() {
        Long userId = Long.valueOf(frontTokenComponent.getUserId());
        JkUserContext context = userContextService.getFrontContext(userId);
        JkPermissionContextResponse response = new JkPermissionContextResponse();
        response.setUserId(context.getUserId());
        response.setPrimaryRoleCode(context.getPrimaryRoleCode());
        response.setPrimaryRoleName(context.getPrimaryRoleName());
        response.setRoles(context.getRoles());
        response.setAuditStatus(context.getAuditStatus());
        response.setFreezeStatus(context.getFreezeStatus());
        response.setCanApplyRoles(context.getCanApplyRoles());
        response.setDisableReason(context.getFreezeReason());
        response.setDisabledReasonText(context.getFreezeReason());
        return CommonResult.success(response);
    }
}
