package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class JkStockActionRequest {
    private String businessType;
    private Long businessId;
    private String businessNo;
    private Long stockAccountId;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private Integer quantity;
    private Long operatorUserId;
    private String remark;
}
