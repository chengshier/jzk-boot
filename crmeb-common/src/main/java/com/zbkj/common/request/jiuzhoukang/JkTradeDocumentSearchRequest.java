package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class JkTradeDocumentSearchRequest {

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("付款状态")
    private String payStatus;

    @ApiModelProperty("审核状态")
    private String auditStatus;

    @ApiModelProperty("关键字，支持单号/申请号")
    private String keywords;
}
