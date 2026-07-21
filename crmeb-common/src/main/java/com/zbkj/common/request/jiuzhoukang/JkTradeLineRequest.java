package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class JkTradeLineRequest {
    @NotNull private Integer productId;
    private Integer skuId;
    @NotNull @Min(1) private Integer quantity;
}
