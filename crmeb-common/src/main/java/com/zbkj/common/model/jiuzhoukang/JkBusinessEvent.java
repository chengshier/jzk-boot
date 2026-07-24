package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/** 关键业务事务提交后的可靠事件记录。 */
@Data
@Accessors(chain = true)
@TableName("jk_business_event")
public class JkBusinessEvent implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String eventKey;
    private String eventType;
    private Long businessId;
    private String businessNo;
    private String payloadJson;
    private String eventStatus;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Date nextRetryTime;
    private String errorMessage;
    private Date occurredTime;
    private Date processedTime;
    private Long lastOperatorId;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false) private String eventStatusText;
    @TableField(exist = false) private String statusTag;
}
