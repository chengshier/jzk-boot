package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class JkRegionSaveRequest {
    private Long id;
    @NotBlank(message = "区域编码不能为空") private String regionCode;
    @NotBlank(message = "区域名称不能为空") private String regionName;
    private String parentRegionCode;
    /**
     * 兼容旧页面保留字段，服务端会自行计算真实层级。
     */
    private Integer regionLevel;
    private String remark;
    private Boolean status;
}
