package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class JkStockFlowResponse implements Serializable {
    private Long id;
    private String flowNo;
    private String businessNo;
    private Long stockAccountId;
    private Long stockItemId;
    private String applicantName;
    private String applicantPhone;
    private String userNickname;
    private String roleName;
    private String regionName;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private String productName;
    private String skuName;
    private String skuText;
    private String businessType;
    private String businessTypeText;
    private String flowType;
    private String flowTypeText;
    private String statusTag;
    private Integer changeQty;
    private Integer beforeAvailableQty;
    private Integer afterAvailableQty;
    private Integer beforeFrozenQty;
    private Integer afterFrozenQty;
    private String remark;
    private Date createTime;
}
