package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
public class JkHealthDeviceCallbackRequest {
    @NotBlank private String providerCode;
    @NotBlank private String deviceSn;
    @NotBlank private String externalNo;
    @NotNull private Date measuredAt;
    @NotNull private BigDecimal value;
    private String unit;
    private String period;
    @NotBlank private String timestamp;
    @NotBlank private String sign;
}
