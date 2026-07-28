package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkPromotionSceneSaveRequest {
    private Long id;
    @NotBlank(message="场景编码不能为空") private String sceneCode;
    @NotBlank(message="场景名称不能为空") private String sceneName;
    @NotBlank(message="小程序页面不能为空") private String pagePath;
    private String roleCodes;
    @NotBlank(message="scene模板不能为空") private String sceneTemplate;
    @NotNull(message="版本不能为空") private Integer versionNo;
    private Boolean status;
    private String remark;
}
