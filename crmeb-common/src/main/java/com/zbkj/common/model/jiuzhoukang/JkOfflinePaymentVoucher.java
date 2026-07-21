package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_offline_payment_voucher")
public class JkOfflinePaymentVoucher {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String voucherNo; private String businessType; private Long businessId; private String voucherUrl; private Long submitUserId;
    private String auditStatus; private Long auditUserId; private Date auditTime; private String rejectReason; private Boolean isCurrent; private Boolean isDeleted;
    private Date createTime; private Date updateTime; private Integer version;
    @TableField(exist = false) private String voucherStatusText;
    @TableField(exist = false) private String voucherStatusTag;
}
