package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

/** 后台只展示配置是否就绪，不返回任何密钥明文。 */
@Data
public class JkHealthIntegrationStatusResponse {
    private Boolean callbackEnabled;
    private Boolean callbackSecretConfigured;
    private Boolean encryptionKeyConfigured;
    private Boolean plaintextAllowed;
    private Boolean syncAutoRetryEnabled;
    private Integer retentionDays;
    private Boolean archiveEnabled;
    private String callbackPath;
    private String securityTip;
}
