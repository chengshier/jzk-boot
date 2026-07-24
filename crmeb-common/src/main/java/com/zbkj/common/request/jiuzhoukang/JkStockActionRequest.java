package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import java.util.Date;

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
    /** 可选批次信息；为空时按来源单号生成批次。 */
    private String batchNo;
    private Date productionDate;
    private Date expireTime;
    private BigDecimal unitCost;
}
