package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkStockTransferReturnRefundRequest {
    @NotNull private Long returnId;
    @NotBlank private String requestNo;
    private String refundVoucherUrl;
    private String remark;
}
