package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_account_reconcile_record")
public class JkAccountReconcileRecord implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String batchNo;
    private Long userId;
    private String roleCode;
    private BigDecimal expectedPendingAmount;
    private BigDecimal actualPendingAmount;
    private BigDecimal pendingDifference;
    private BigDecimal expectedTotalCommissionAmount;
    private BigDecimal actualTotalCommissionAmount;
    private BigDecimal totalCommissionDifference;
    private BigDecimal expectedReversedAmount;
    private BigDecimal actualReversedAmount;
    private BigDecimal reversedDifference;
    private BigDecimal expectedWithdrawingAmount;
    private BigDecimal actualWithdrawingAmount;
    private BigDecimal withdrawingDifference;
    private BigDecimal expectedWithdrawnAmount;
    private BigDecimal actualWithdrawnAmount;
    private BigDecimal withdrawnDifference;
    private BigDecimal commissionNetBalance;
    private BigDecimal fundNetBalance;
    private BigDecimal crossAccountDifference;
    private String reconcileStatus;
    private String issueSummary;
    private Long operatorId;
    private Date reconcileTime;
    private Date createTime;

    @TableField(exist = false) private String applicantName;
    @TableField(exist = false) private String applicantPhone;
    @TableField(exist = false) private String roleName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
}
