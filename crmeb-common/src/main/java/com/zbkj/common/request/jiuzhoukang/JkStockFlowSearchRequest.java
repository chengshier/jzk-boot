package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;

@Data
public class JkStockFlowSearchRequest implements Serializable {
    private Long stockAccountId;
    private Integer productId;
    private Integer skuId;
    private String flowType;
    private String businessNo;
}
