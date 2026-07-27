package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 提现收款账户安全展示对象，不包含银行卡密文或完整卡号。 */
@Data
@Accessors(chain = true)
public class JkWithdrawPayeeAccountResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String accountType;
    private String accountName;
    private String bankName;
    private String bankAccountMask;
    private Boolean isDefault;
    private Boolean status;
    private Date createTime;
    private Date updateTime;
}
