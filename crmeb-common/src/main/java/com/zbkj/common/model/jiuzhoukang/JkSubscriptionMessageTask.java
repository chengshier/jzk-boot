package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_subscription_message_task")
public class JkSubscriptionMessageTask implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String taskNo;
    private String eventType;
    private String eventKey;
    private Long receiverUserId;
    private String openid;
    private String templateCode;
    private String templateId;
    private String pagePath;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Date nextRetryTime;
    private String lastError;
    private Boolean enabledSnapshot;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
