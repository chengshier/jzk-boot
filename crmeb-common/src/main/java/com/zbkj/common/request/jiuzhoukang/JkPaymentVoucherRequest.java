package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class JkPaymentVoucherRequest {
    @NotBlank private String voucherUrl;
}
