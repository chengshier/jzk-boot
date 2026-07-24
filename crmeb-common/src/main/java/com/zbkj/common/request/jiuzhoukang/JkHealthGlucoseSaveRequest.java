package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class JkHealthGlucoseSaveRequest {
    @NotNull @DecimalMin("0.1") @DecimalMax("40.0") private BigDecimal value;
    private String period;
    private Date measuredAt;
    @Size(max = 500) private String remark;
    private String requestNo;
}
