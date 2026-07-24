package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 九州康提现收款账户。
 *
 * <p>银行卡号只保存密文、哈希和掩码，接口层不得直接返回密文。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_withdraw_payee_account")
public class JkWithdrawPayeeAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String accountType;
    private String accountName;
    private String bankName;
    private String bankAccountCipher;
    private String bankAccountHash;
    private String bankAccountMask;
    private Boolean isDefault;
    private Boolean status;
    private Boolean isDeleted;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
