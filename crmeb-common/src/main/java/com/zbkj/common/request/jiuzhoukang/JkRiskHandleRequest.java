package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class JkRiskHandleRequest {
    @NotNull private Long id;
    @NotBlank private String action;
    @Size(max = 1000) private String remark;
}
