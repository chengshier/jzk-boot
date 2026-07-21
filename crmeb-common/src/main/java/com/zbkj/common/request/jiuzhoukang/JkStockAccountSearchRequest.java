package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;

@Data
public class JkStockAccountSearchRequest implements Serializable {
    private String accountType;
    private String roleCode;
    private String regionCode;
    private Long ownerUserId;
    private Boolean status;
}
