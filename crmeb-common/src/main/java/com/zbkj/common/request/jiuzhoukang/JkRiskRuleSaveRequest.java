package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class JkRiskRuleSaveRequest {
    private Long id;
    @NotBlank private String ruleCode;
    @NotBlank private String ruleName;
    @NotBlank private String scannerType;
    @NotBlank private String riskType;
    @NotBlank private String riskLevel;
    private BigDecimal thresholdValue;
    private Integer windowHours;
    private String configJson;
    private Boolean enabled;
    private String remark;
}
