package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;

@Data
public class JkStockAccountResponse implements Serializable {
    private Long id;
    private String accountNo;
    private String accountType;
    private String accountTypeText;
    private String roleCode;
    private String roleName;
    private String regionCode;
    private String regionName;
    private Long ownerUserId;
    private String ownerName;
    private String userNickname;
    private String applicantPhone;
    private Boolean status;
    private String statusText;
    private String statusTag;
}
