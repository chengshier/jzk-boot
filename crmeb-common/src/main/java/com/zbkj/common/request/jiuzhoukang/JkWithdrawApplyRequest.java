package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 九州康提现申请。
 *
 * <p>新版本只提交 {@code payeeAccountId}，由服务端读取本人已保存账户并生成不可变快照。
 * 结构化银行卡字段仅用于兼容旧 App，请勿在新页面继续使用。</p>
 */
@Data
public class JkWithdrawApplyRequest implements Serializable {
    private BigDecimal amount;
    private String requestNo;
    /** 本人已保存的提现收款账户 ID。 */
    private Long payeeAccountId;
    /** 当前仅支持 BANK，旧版本兼容字段。 */
    @Deprecated
    private String accountType;
    /** 旧版本兼容字段。 */
    @Deprecated
    private String accountName;
    /** 旧版本兼容字段。 */
    @Deprecated
    private String bankName;
    /** 旧版本兼容字段。 */
    @Deprecated
    private String bankAccount;
    private String remark;
    /** 旧版兼容字段。Controller 始终忽略并由后端重建快照。 */
    @Deprecated
    private String payeeSnapshotJson;
}
