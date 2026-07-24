package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class JkStockTransferReturnShipRequest {
    @NotBlank private String requestNo;
    private String logisticsCompany;
    private String logisticsNo;
    private String remark;
}
