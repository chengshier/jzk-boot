package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkSubscriptionTask;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.wechat.JkSubscriptionTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/subscription-task")
@Api(tags = "九州康微信订阅消息任务")
public class JkSubscriptionTaskAdminController {
    @Autowired private JkSubscriptionTaskService service;

    @GetMapping("/list")
    @ApiOperation("查询订阅消息任务")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_SUBSCRIPTION_TASK_LIST + "')")
    public CommonResult<CommonPage<JkSubscriptionTask>> list(@RequestParam(required = false) String status,
                                                               @RequestParam(required = false) String templateCode,
                                                               @RequestParam(required = false) Long receiverUserId,
                                                               PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(service.list(status, templateCode, receiverUserId, page)));
    }

    @GetMapping("/status")
    @ApiOperation("查询微信订阅能力配置状态")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_SUBSCRIPTION_TASK_LIST + "')")
    public CommonResult<Map<String, Object>> status() {
        return CommonResult.success(service.status());
    }

    @PostMapping("/process")
    @ApiOperation("手动处理到期任务")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_SUBSCRIPTION_TASK_MANAGE + "')")
    public CommonResult<Integer> process(@RequestParam(defaultValue = "20") Integer limit) {
        return CommonResult.success(service.processDue(limit == null ? 20 : limit));
    }

    @PostMapping("/{id}/retry")
    @ApiOperation("将失败或等待配置的任务重新入队")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_SUBSCRIPTION_TASK_MANAGE + "')")
    public CommonResult<JkSubscriptionTask> retry(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "管理员重新入队") String reason) {
        return CommonResult.success(service.retry(id, reason));
    }
}
