package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class JkPriceRuleSaveRequest implements Serializable {
    private Long id;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private String roleCode;
    private String regionCode;
    private Long userId;
    private String priceType;
    private BigDecimal fixedPrice;
    private BigDecimal discountRate;
    private Integer ruleVersion;
    private Date effectiveTime;
    private Date expireTime;
    private String remark;
}
