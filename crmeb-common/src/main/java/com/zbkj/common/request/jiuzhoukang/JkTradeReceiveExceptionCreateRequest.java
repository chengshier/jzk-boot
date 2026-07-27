package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/** 前台异常收货上报请求。 */
@Data
public class JkTradeReceiveExceptionCreateRequest {
    @NotBlank private String requestNo;
    /** PLATFORM_ORDER 或 STOCK_TRANSFER。 */
    @NotBlank private String businessType;
    @NotNull private Long businessId;
    /** SHORTAGE、DAMAGED、MIXED、OTHER。 */
    @NotBlank private String exceptionType;
    @NotBlank private String exceptionReason;
    private List<String> evidenceUrls;
    @Valid @NotEmpty private List<Item> items;

    @Data
    public static class Item {
        @NotNull private Long businessItemId;
        @NotNull @Min(0) private Integer receivedQty;
        @NotNull @Min(0) private Integer damagedQty;
        private String itemRemark;
    }
}
