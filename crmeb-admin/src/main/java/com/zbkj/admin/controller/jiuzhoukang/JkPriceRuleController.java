package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkPriceRuleStatusRequest;
import com.zbkj.common.response.jiuzhoukang.JkPriceRuleResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.service.jiuzhoukang.price.JkPriceRuleService;
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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/admin/jk/price-rule")
@Api(tags = "九州康价格规则管理")
public class JkPriceRuleController {

    @Autowired
    private JkPriceRuleService priceRuleService;
    @Autowired
    private JkRegionDao regionDao;

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PRICE_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.PRICE_RULE_CONFIG, checkDataScope = false)
    @GetMapping("/list")
    @ApiOperation("价格规则列表")
    public CommonResult<CommonPage<JkPriceRuleResponse>> list(@Validated JkPriceRuleSearchRequest request,
                                                              @Validated PageParamRequest pageParamRequest) {
        List<JkPriceRuleResponse> rows = priceRuleService.getAdminList(request, pageParamRequest);
        return CommonResult.success(CommonPage.restPage(rows));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PRICE_RULE_SAVE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.PRICE_RULE_CONFIG, checkDataScope = false)
    @PostMapping("/save")
    @ApiOperation("新增或修改价格规则")
    public CommonResult<JkPriceRuleResponse> save(@RequestBody JkPriceRuleSaveRequest request) {
        return CommonResult.success(priceRuleService.saveRule(request));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PRICE_RULE_STATUS + "')")
    @JkBizPermission(value = JkBizPermissionCodes.PRICE_RULE_CONFIG, checkDataScope = false)
    @PostMapping("/updateStatus")
    @ApiOperation("启用或禁用价格规则")
    public CommonResult<Boolean> updateStatus(@RequestBody JkPriceRuleStatusRequest request) {
        return CommonResult.success(priceRuleService.updateRuleStatus(request));
    }

    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_PRICE_RULE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.PRICE_RULE_CONFIG, checkDataScope = false)
    @GetMapping("/region/options")
    @ApiOperation("价格规则区域选项")
    public CommonResult<List<JkRegion>> regionOptions() {
        return CommonResult.success(regionDao.selectList(null).stream()
                .filter(region -> !Boolean.TRUE.equals(region.getIsDeleted()))
                .sorted(Comparator.comparing(JkRegion::getRegionLevel, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(JkRegion::getRegionCode, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList()));
    }
}
