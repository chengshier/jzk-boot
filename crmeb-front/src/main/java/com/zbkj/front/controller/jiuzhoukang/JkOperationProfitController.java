package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.profit.JkOperationProfitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/front/jk/operation-profit")
public class JkOperationProfitController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkOperationProfitService profitService;
    private Long userId() { return Long.valueOf(token.getUserId()); }

    @GetMapping("/summary")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_VIEW_SELF, checkDataScope = true)
    public CommonResult<BigDecimal> summary() { return CommonResult.success(profitService.summary(userId())); }

    @GetMapping("/list")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_VIEW_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkOperationProfitRecord>> list(@RequestParam(required = false) String sourceType,
                                                                   @RequestParam(required = false) String status,
                                                                   PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(profitService.list(userId(), sourceType, status, page)));
    }

    @GetMapping("/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_VIEW_SELF, checkDataScope = true)
    public CommonResult<JkOperationProfitRecord> detail(@PathVariable Long id) {
        for (JkOperationProfitRecord row : profitService.list(userId(), null, null, new PageParamRequest()).getList()) {
            if (id.equals(row.getId())) return CommonResult.success(row);
        }
        throw new IllegalArgumentException("经营收益记录不存在或无权查看");
    }
}
