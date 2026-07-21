package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class JkPriceRuleResponse implements Serializable {
    private Long id;
    private String ruleNo;
    private Integer productId;
    private String productName;
    private Integer skuId;
    private String skuName;
    private String skuText;
    private String skuCode;
    private String roleCode;
    private String roleName;
    private String regionCode;
    private String regionName;
    private Long userId;
    private String applicantName;
    private String applicantPhone;
    private String userNickname;
    private String priceType;
    private BigDecimal fixedPrice;
    private BigDecimal discountRate;
    private Integer ruleVersion;
    private Date effectiveTime;
    private Date expireTime;
    private Boolean status;
    private String statusText;
    private String statusTag;
    private String remark;
}
