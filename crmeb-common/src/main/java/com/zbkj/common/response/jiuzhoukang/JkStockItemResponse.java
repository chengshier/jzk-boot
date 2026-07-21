package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;

@Data
public class JkStockItemResponse implements Serializable {
    private Long id;
    private Long stockAccountId;
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
    private Integer availableQty;
    private Integer frozenQty;
    private Integer totalInQty;
    private Integer totalOutQty;
    private Integer version;
}
