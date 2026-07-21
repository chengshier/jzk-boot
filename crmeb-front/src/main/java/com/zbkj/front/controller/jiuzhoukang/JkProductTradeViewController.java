package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
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

    @ApiOperation("商品交易视图")
    @GetMapping("/trade-view/{productId}")
    @JkBizPermission(value = JkBizConstants.PERMISSION_PRODUCT_TRADE_VIEW, checkDataScope = false)
    public CommonResult<JkProductTradeViewResponse> tradeView(@PathVariable Integer productId,
                                                              @RequestParam(value = "skuId", required = false) String skuId) {
        Integer userId = frontTokenComponent.getUserId();
        JkUserContext context = userId == null ? userContextService.getAnonymousContext() : userContextService.getFrontContext(Long.valueOf(userId));
        return CommonResult.success(productTradeViewService.getTradeView(productId, skuId, context));
    }
}
