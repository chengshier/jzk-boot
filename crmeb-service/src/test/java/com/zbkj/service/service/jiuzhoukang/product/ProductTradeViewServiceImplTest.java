package com.zbkj.service.service.jiuzhoukang.product;

import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.impl.jiuzhoukang.product.ProductTradeViewServiceImpl;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.price.PriceCalculateService;
import com.zbkj.service.service.jiuzhoukang.stock.StockVisibilityService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Collections;

public class ProductTradeViewServiceImplTest {

    @Test
    public void returnsProductAndSkuDisplayFieldsForTradeView() {
        ProductTradeViewServiceImpl service = new ProductTradeViewServiceImpl();
        ReflectionTestUtils.setField(service, "storeProductService", proxy(StoreProductService.class, (method, args) -> {
            if ("getById".equals(method.getName())) {
                return new StoreProduct().setId(101).setStoreName("灵芝孢子粉").setImage("p.png").setPrice(BigDecimal.TEN).setOtPrice(BigDecimal.valueOf(12)).setUnitName("盒");
            }
            return null;
        }));
        ReflectionTestUtils.setField(service, "productAttrValueService", proxy(StoreProductAttrValueService.class, (method, args) -> {
            if ("getListByProductIdAndType".equals(method.getName())) {
                return Collections.singletonList(new StoreProductAttrValue()
                        .setId(202)
                        .setUnique("SKU202")
                        .setSuk("大瓶装")
                        .setAttrValue("规格:大瓶装")
                        .setPrice(BigDecimal.valueOf(88))
                        .setOtPrice(BigDecimal.valueOf(99))
                        .setStock(20));
            }
            return Collections.emptyList();
        }));
        ReflectionTestUtils.setField(service, "priceCalculateService", proxy(PriceCalculateService.class, (method, args) -> new JkProductTradeViewResponse.PriceInfo()));
        ReflectionTestUtils.setField(service, "stockVisibilityService", proxy(StockVisibilityService.class, (method, args) -> new JkProductTradeViewResponse.StockInfo()));

        JkProductTradeViewResponse response = service.getTradeView(101, "202", new JkUserContext());

        Assert.assertNotNull(response.getProduct());
        Assert.assertEquals("灵芝孢子粉", response.getProduct().getStoreName());
        Assert.assertEquals("大瓶装", response.getProduct().getSkuName());
        Assert.assertEquals("规格:大瓶装", response.getProduct().getSkuText());
        Assert.assertEquals("SKU202", response.getProduct().getSkuCode());
    }

    private <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) return type.getSimpleName() + "Proxy";
                if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                if ("equals".equals(method.getName())) return proxy == args[0];
            }
            return invocation.apply(method, args);
        }));
    }

    private interface Invocation {
        Object apply(Method method, Object[] args) throws Throwable;
    }
}
