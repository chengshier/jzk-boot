package com.zbkj.front.controller.jiuzhoukang;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkOfflineSaleCreateRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.offline.JkOfflineSaleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("api/front/jk/offline-sale")
@Api(tags = "九州康线下终端销售")
public class JkOfflineSaleController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkOfflineSaleService saleService;
    @Autowired private UserService userService;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @GetMapping("/customer/resolve")
    @ApiOperation("按完整手机号精确解析已注册客户；不支持模糊枚举")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    public CommonResult<Map<String, Object>> resolveCustomer(@RequestParam String phone) {
        String value = phone == null ? "" : phone.trim();
        if (!value.matches("^1\\d{10}$")) throw new IllegalArgumentException("请输入完整的11位手机号");
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, value).last("limit 1"));
        if (user == null) throw new IllegalArgumentException("未找到对应的已注册客户");
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("userId", Long.valueOf(user.getUid()));
        result.put("name", maskName(StrUtil.blankToDefault(user.getRealName(), user.getNickname())));
        result.put("phone", value.substring(0, 3) + "****" + value.substring(7));
        return CommonResult.success(result);
    }

    @PostMapping("/create")
    @ApiOperation("登记线下终端销售")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    public CommonResult<JkOfflineSale> create(@RequestBody @Validated JkOfflineSaleCreateRequest request) {
        return CommonResult.success(saleService.create(userId(), request));
    }

    @GetMapping("/list")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_SELF, checkDataScope = true)
    public CommonResult<CommonPage<JkOfflineSale>> list(@RequestParam(required = false) String status, PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(saleService.list(userId(), status, page)));
    }

    @GetMapping("/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_SELF, checkDataScope = true)
    public CommonResult<JkOfflineSale> detail(@PathVariable Long id) {
        return CommonResult.success(saleService.detail(userId(), id, false));
    }

    @PostMapping("/{id}/cancel")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    public CommonResult<JkOfflineSale> cancel(@PathVariable Long id, @RequestBody @Validated JkOfflineSaleActionRequest request) {
        return CommonResult.success(saleService.cancel(userId(), id, request));
    }

    @PostMapping("/{id}/return")
    @JkBizPermission(value = JkBizPermissionCodes.STOCK_APPLY, checkDataScope = true)
    public CommonResult<JkOfflineSale> returnSale(@PathVariable Long id, @RequestBody @Validated JkOfflineSaleActionRequest request) {
        return CommonResult.success(saleService.returnSale(userId(), id, request));
    }

    private String maskName(String value) { return StrUtil.isBlank(value) ? "已注册客户" : value.substring(0, 1) + "**"; }
}
