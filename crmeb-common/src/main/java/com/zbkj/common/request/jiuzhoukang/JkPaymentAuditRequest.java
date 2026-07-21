package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class JkPaymentAuditRequest {
    @NotNull private Long businessId;
    @NotNull private Boolean approved;
    private String remark;
}
