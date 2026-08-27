package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class JkPromotionOpenEventRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotBlank(message = "推广场景不能为空") private String sceneCode;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    private String entryPage;
    private String channel;
}
