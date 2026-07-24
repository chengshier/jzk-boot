package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

@Data
public class JkHealthAuthorizedOwnerResponse {
    private Long ownerUserId;
    private String ownerName;
    private String ownerPhoneMasked;
    private String scopeCodes;
    private Long authorizationId;
    /** 数据所有人是否明确允许当前顾问导出健康数据。 */
    private Boolean allowExport;
}
