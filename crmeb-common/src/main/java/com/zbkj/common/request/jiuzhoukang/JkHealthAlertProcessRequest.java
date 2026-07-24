package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class JkHealthAlertProcessRequest {
    @NotNull private Long alertId;
    @NotBlank private String action;
    @Size(max = 500) private String remark;
}
