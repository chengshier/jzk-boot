package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class JkOfflineSaleReturnRequest {
    @NotNull private String requestNo;
    private String reason;
    @Valid @NotEmpty private List<Item> items;

    @Data
    public static class Item {
        @NotNull private Long saleItemId;
        @NotNull @Min(1) private Integer quantity;
    }
}
