package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

/** 小程序跳转三诺 H5 前所需的最小数据，避免暴露授权记录内部字段。 */
@Data
@Accessors(chain = true)
public class JkSinocareAuthorizationPrepareResponse {
    private String uniqueId;
    private String authorizationUrl;
}
