package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/** 厂商配置保存请求。credentialJson/configJson 入库前由服务层加密。 */
@Data
public class JkHealthProviderSaveRequest {
    private Long id;
    @NotBlank private String providerCode;
    @NotBlank private String providerName;
    private String adapterType;
    @NotBlank private String syncMode;
    private String authType;
    private String baseUrl;
    private String callbackPath;
    private String credentialJson;
    private String configJson;
    private Boolean enabled;
}
