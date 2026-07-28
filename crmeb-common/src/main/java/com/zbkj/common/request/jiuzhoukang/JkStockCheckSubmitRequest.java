package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class JkStockCheckSubmitRequest {
    @NotNull private Long checkId;
    @Valid @NotEmpty private List<Item> items;
    private String remark;

    @Data
    public static class Item {
        @NotNull private Long checkItemId;
        @NotNull @Min(0) private Integer actualAvailableQty;
        private String remark;
    }
}
