package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class JkStockCheckCountRequest {
    @NotNull(message = "盘点明细不能为空") private Long itemId;
    @NotNull(message = "实盘数量不能为空") @Min(value = 0, message = "实盘数量不能小于0") private Integer actualQuantity;
    private String remark;
}
