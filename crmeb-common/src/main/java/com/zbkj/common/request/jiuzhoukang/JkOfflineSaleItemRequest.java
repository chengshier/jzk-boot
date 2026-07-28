package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class JkOfflineSaleItemRequest {
    @NotNull(message = "商品不能为空") private Integer productId;
    @NotNull(message = "SKU不能为空") private Integer skuId;
    @NotNull(message = "销售数量不能为空") @Min(value = 1, message = "销售数量必须大于0") private Integer quantity;
    @NotNull(message = "成交单价不能为空") @DecimalMin(value = "0.01", message = "成交单价必须大于0") private BigDecimal unitPrice;
}
