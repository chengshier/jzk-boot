package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class JkStockTransferReturnCreateRequest {
    @NotBlank private String requestNo;
    @NotNull private Long originalTransferId;
    @NotBlank private String returnReason;
    @Valid @NotEmpty private List<Item> items;

    @Data
    public static class Item {
        @NotNull private Long originalTransferItemId;
        @NotNull @Min(1) private Integer quantity;
    }
}
