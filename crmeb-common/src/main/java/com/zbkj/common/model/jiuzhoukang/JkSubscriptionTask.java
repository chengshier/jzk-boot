package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 微信订阅消息任务；总开关默认关闭，禁止将排队误报为发送成功。 */
@Data
@Accessors(chain = true)
@TableName("jk_subscription_task")
public class JkSubscriptionTask implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String taskNo;
    private String templateCode;
    private String templateId;
    private String businessType;
    private Long businessId;
    private Long receiverUserId;
    private String recipientOpenId;
    private String pagePath;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Date nextRetryTime;
    private Date sentAt;
    private String wechatMessageId;
    private String errorCode;
    private String errorMessage;
    private String requestNo;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false) private String statusText;
}
