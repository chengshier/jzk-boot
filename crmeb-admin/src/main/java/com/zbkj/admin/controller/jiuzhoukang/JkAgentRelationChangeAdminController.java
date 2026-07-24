package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.*;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelationChangeApply;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationChangeAuditRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentRelationChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/admin/jk/agent-relation/change")
public class JkAgentRelationChangeAdminController {
    @Autowired private JkAgentRelationChangeService service;
    @Autowired private JkAdminActorService actor;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_AGENT_RELATION_CHANGE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_CHANGE_AUDIT)
    public CommonResult<CommonPage<JkAgentRelationChangeApply>> list(@RequestParam(required = false) String status,
                                                                      @RequestParam(required = false) Long userId,
                                                                      PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.listAdmin(status, userId, page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_AGENT_RELATION_CHANGE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_CHANGE_AUDIT)
    public CommonResult<JkAgentRelationChangeApply> detail(@PathVariable Long id) {
        return CommonResult.success(service.detail(operator(), id, true));
    }

    @PostMapping("/audit")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_AGENT_RELATION_CHANGE_AUDIT + "')")
    @JkBizPermission(value = JkBizPermissionCodes.AGENT_RELATION_CHANGE_AUDIT)
    public CommonResult<JkAgentRelationChangeApply> audit(@RequestBody @Validated JkAgentRelationChangeAuditRequest request) {
        return CommonResult.success(service.audit(operator(), request));
    }

    private Long operator() {
        Long linked = actor.getLinkedFrontUserId(actor.getCurrentAdmin());
        if (linked != null) return linked;
        if (actor.isPlatformSuperAdmin(actor.getCurrentAdmin())) return -Long.valueOf(actor.getCurrentAdmin().getId());
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
