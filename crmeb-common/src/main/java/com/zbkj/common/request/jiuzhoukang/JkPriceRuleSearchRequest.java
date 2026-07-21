package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;

@Data
public class JkPriceRuleSearchRequest implements Serializable {
    private Integer productId;
    private String roleCode;
    private String regionCode;
    private Long userId;
    private Boolean status;
}
