package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 九州康提现申请。
 *
 * <p>收款信息使用结构化字段提交，由服务端生成不可变快照；不再接受前端自行拼装的
 * 默认银行卡文案作为真实收款账户。</p>
 */
@Data
public class JkWithdrawApplyRequest implements Serializable {
    private BigDecimal amount;
    private String requestNo;
    /** 当前仅支持 BANK。 */
    private String accountType;
    /** 收款人姓名。 */
    private String accountName;
    /** 开户银行。 */
    private String bankName;
    /** 银行卡号。 */
    private String bankAccount;
    private String remark;
    /**
     * 旧版兼容字段。新请求不应传入；Controller 会忽略该字段并由后端重建快照。
     */
    @Deprecated
    private String payeeSnapshotJson;
}
