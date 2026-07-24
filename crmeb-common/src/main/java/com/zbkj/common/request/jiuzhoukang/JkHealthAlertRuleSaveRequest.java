package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class JkHealthAlertRuleSaveRequest {
    private Long id;
    @NotBlank private String ruleName;
    private Long ownerUserId;
    @NotBlank private String dataType;
    private String periodCode;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    @NotBlank private String alertLevel;
    private Boolean enabled;
}
