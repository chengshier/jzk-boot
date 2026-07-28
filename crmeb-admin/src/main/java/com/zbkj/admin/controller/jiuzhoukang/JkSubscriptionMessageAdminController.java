package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkSubscriptionMessageTask;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.impl.jiuzhoukang.message.JkSubscriptionMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin/jk/subscription/task")
public class JkSubscriptionMessageAdminController {
    @Autowired private JkSubscriptionMessageService service;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_SUBSCRIPTION_TASK_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.BUSINESS_EVENT_MANAGE, checkDataScope = false)
    public CommonResult<CommonPage<JkSubscriptionMessageTask>> list(@RequestParam(required = false) String eventType,
                                                                     @RequestParam(required = false) String status,
                                                                     @RequestParam(required = false) Long receiverUserId,
                                                                     PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.list(eventType, status, receiverUserId, page)));
    }

    @PostMapping("/run")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_SUBSCRIPTION_TASK_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.BUSINESS_EVENT_MANAGE, checkDataScope = false)
    public CommonResult<Integer> run(@RequestParam(defaultValue = "20") int limit) {
        return CommonResult.success(service.processDue(limit));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_SUBSCRIPTION_TASK_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.BUSINESS_EVENT_MANAGE, checkDataScope = false)
    public CommonResult<JkSubscriptionMessageTask> retry(@PathVariable Long id) {
        return CommonResult.success(service.retry(id));
    }
}
