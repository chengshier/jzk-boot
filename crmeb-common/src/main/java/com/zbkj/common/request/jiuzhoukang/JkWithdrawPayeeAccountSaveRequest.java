package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;

/** 提现收款账户新增或编辑请求。 */
@Data
public class JkWithdrawPayeeAccountSaveRequest implements Serializable {
    private Long id;
    private String accountType;
    private String accountName;
    private String bankName;
    private String bankAccount;
    private Boolean setDefault;
}
