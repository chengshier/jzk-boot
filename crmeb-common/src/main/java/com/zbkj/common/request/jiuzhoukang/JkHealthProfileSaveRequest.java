package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class JkHealthProfileSaveRequest {
    @DecimalMin("50") @DecimalMax("250") private BigDecimal heightCm;
    @DecimalMin("10") @DecimalMax("500") private BigDecimal weightKg;
    @Size(max = 32) private String diabetesType;
    @DecimalMin("0.1") @DecimalMax("40") private BigDecimal glucoseTargetMin;
    @DecimalMin("0.1") @DecimalMax("40") private BigDecimal glucoseTargetMax;
    @Size(max = 1000) private String remark;
}
