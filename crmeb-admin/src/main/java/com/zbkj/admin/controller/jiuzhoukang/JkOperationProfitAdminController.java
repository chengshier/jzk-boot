package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkV31PermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin/jk/operation-profit")
public class JkOperationProfitAdminController {
    @Autowired private JkOperationProfitService profitService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('" + JkV31PermissionCodes.ADMIN_OPERATION_PROFIT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.REPORT_VIEW, checkDataScope = true)
    public CommonResult<CommonPage<JkOperationProfitRecord>> list(@RequestParam(required = false) Long userId,
                                                                   @RequestParam(required = false) String sourceType,
                                                                   @RequestParam(required = false) String status,
                                                                   PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(profitService.list(userId, sourceType, status, page)));
    }
}
