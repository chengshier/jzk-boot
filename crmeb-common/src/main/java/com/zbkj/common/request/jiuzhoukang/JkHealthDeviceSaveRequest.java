package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class JkHealthDeviceSaveRequest {
    private Long id;
    @NotBlank private String deviceSn;
    @NotBlank private String providerCode;
    private String externalDeviceId;
    @NotBlank private String deviceType;
    private String deviceModel;
    @Size(min = 4, max = 32) private String bindCode;
    private String status;
}
