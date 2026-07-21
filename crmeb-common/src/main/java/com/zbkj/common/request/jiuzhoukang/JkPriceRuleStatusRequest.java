package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;

@Data
public class JkPriceRuleStatusRequest implements Serializable {
    private Long id;
    private Boolean status;
}
