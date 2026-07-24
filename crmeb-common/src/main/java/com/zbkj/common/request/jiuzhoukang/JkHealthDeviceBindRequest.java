package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class JkHealthDeviceBindRequest {
    @NotBlank private String deviceSn;
    @NotBlank private String bindCode;
    private String requestNo;
}
