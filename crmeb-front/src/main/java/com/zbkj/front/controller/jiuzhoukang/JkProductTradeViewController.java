package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.service.service.StoreProductService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.product.ProductTradeViewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/front/jk/product")
@Api(tags = "九州康商品交易视图")
public class JkProductTradeViewController {

    @Autowired
    private ProductTradeViewService productTradeViewService;
    @Autowired
    private JkUserContextService userContextService;
    @Autowired
    private FrontTokenComponent frontTokenComponent;
    @Autowired
    private StoreProductService storeProductService;

    @ApiOperation("当前身份可订货商品目录")
    @GetMapping("/catalog")
    @JkBizPermission(value = JkBizPermissionCodes.PRODUCT_TRADE_VIEW, checkDataScope = false)
    public CommonResult<List<JkProductTradeViewResponse>> catalog(@RequestParam(value = "keyword", required = false) String keyword) {
        Integer userId = frontTokenComponent.getUserId();
        JkUserContext context = userId == null ? userContextService.getAnonymousContext() : userContextService.getFrontContext(Long.valueOf(userId));
        LambdaQueryWrapper<StoreProduct> query = new LambdaQueryWrapper<>();
        query.eq(StoreProduct::getIsShow, true).eq(StoreProduct::getIsDel, false);
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(item -> item.like(StoreProduct::getStoreName, keyword.trim()).or().like(StoreProduct::getStoreInfo, keyword.trim()));
        }
        query.orderByDesc(StoreProduct::getSort).orderByDesc(StoreProduct::getId).last("limit 100");
        List<JkProductTradeViewResponse> result = new ArrayList<>();
        for (StoreProduct product : storeProductService.list(query)) {
            JkProductTradeViewResponse view = productTradeViewService.getTradeView(product.getId(), null, context);
            if (view.getActions() != null && Boolean.TRUE.equals(view.getActions().getCanOrderFromPlatform())) {
                result.add(view);
            }
        }
        return CommonResult.success(result);
    }

    @ApiOperation("商品交易视图")
    @GetMapping("/trade-view/{productId}")
    @JkBizPermission(value = JkBizPermissionCodes.PRODUCT_TRADE_VIEW, checkDataScope = false)
    public CommonResult<JkProductTradeViewResponse> tradeView(@PathVariable Integer productId,
                                                              @RequestParam(value = "skuId", required = false) String skuId) {
        Integer userId = frontTokenComponent.getUserId();
        JkUserContext context = userId == null ? userContextService.getAnonymousContext() : userContextService.getFrontContext(Long.valueOf(userId));
        return CommonResult.success(productTradeViewService.getTradeView(productId, skuId, context));
    }
}
