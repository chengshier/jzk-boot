package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class JkPromotionCodeResponse {
    private Long cacheId;
    private String sceneCode;
    private String sceneName;
    private String sceneValue;
    private String pagePath;
    private Long fileObjectId;
    private String downloadPath;
    private String status;
    private Date generatedAt;
    private String errorMessage;
}
