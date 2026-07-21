package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_commission_settle_task")
public class JkCommissionSettleTask implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private String settleType;
    private String status;
    private String requestNo;
    private String idempotencyKey;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String failReason;
    private Long operatorId;
    private Date startTime;
    private Date finishTime;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String statusText;
    @TableField(exist = false)
    private String statusTag;
}
