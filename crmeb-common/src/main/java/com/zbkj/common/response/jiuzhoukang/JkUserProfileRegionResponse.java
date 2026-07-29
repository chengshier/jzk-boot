package com.zbkj.common.response.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(value = "JkUserProfileRegionResponse", description = "用户个人资料标准区域；与收货地址相互独立")
public class JkUserProfileRegionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String regionCode;
    private String regionName;
    private String regionPathName;
    private String regionSource;
    private Date regionUpdateTime;

    @ApiModelProperty(value = "原有自由文本详细地址，只用于资料展示，不作为正式区域编码")
    private String detailAddress;

    @ApiModelProperty(value = "固定提示：实际配送仍以每笔订单收货地址为准")
    private String notice;
}
