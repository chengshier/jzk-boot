package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.response.jiuzhoukang.JkPermissionContextResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.support.JkDictLabelHelper;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import com.zbkj.service.service.jiuzhoukang.support.JkPermissionContextSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/front/jk/permission")
@Api(tags = "九州康权限上下文")
public class JkPermissionController {

    @Autowired
    private JkUserContextService userContextService;
    @Autowired
    private FrontTokenComponent frontTokenComponent;
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @ApiOperation("获取业务权限上下文")
    @GetMapping("/context")
    public CommonResult<JkPermissionContextResponse> context(HttpServletRequest request) {
        Integer userId = frontTokenComponent.getUserId();
        JkUserContext context = userContextService.getFrontContext(Long.valueOf(userId));
        JkPermissionContextSupport.IdentityVisualState visualState =
                JkPermissionContextSupport.resolveIdentityVisualState(context.getPrimaryRoleName(), context.getAuditStatus(),
                        Boolean.TRUE.equals(context.getFreezeStatus()), context.getFreezeReason());
        JkPermissionContextResponse response = new JkPermissionContextResponse();
        response.setUserId(context.getUserId());
        response.setEntryAccess(context.getEntryAccess());
        response.setPrimaryRoleCode(context.getPrimaryRoleCode());
        response.setPrimaryRoleName(context.getPrimaryRoleName());
        response.setRoles(context.getRoles());
        response.setAuditStatus(context.getAuditStatus());
        response.setAuditStatusText(JkDictLabelHelper.label("audit_status", context.getAuditStatus()));
        response.setFreezeStatus(context.getFreezeStatus());
        response.setRegionCode(context.getRegionCode());
        response.setRegionName(displayEnrichmentSupport.resolveRegionNameForDisplay(context.getRegionCode()));
        response.setBelongCountyAgentId(context.getBelongCountyAgentId());
        response.setCanApplyRoles(context.getCanApplyRoles());
        response.setPermissions(context.getPermissions());
        response.setMenus(JkPermissionContextSupport.resolveMenus(context.getPermissions()));
        response.setIdentityStatusText(visualState.getIdentityStatusText());
        response.setDisableReason(visualState.getDisableReason());
        response.setDisabledReasonText(visualState.getDisableReason());
        response.setCacheVersion(context.getCacheVersion());
        response.setDataScopes(context.getDataScopes().stream().map(item -> {
            JkPermissionContextResponse.DataScopeItem dataScopeItem = new JkPermissionContextResponse.DataScopeItem();
            dataScopeItem.setScopeType(item.getScopeType());
            dataScopeItem.setScopeTypeText(JkPermissionContextSupport.scopeTypeText(item.getScopeType()));
            dataScopeItem.setRegionCode(item.getRegionCode());
            dataScopeItem.setRegionName(displayEnrichmentSupport.resolveRegionNameForDisplay(item.getRegionCode()));
            dataScopeItem.setCountyAgentId(item.getCountyAgentId());
            dataScopeItem.setTeamRootUserId(item.getTeamRootUserId());
            return dataScopeItem;
        }).collect(Collectors.toList()));
        return CommonResult.success(response);
    }
}
