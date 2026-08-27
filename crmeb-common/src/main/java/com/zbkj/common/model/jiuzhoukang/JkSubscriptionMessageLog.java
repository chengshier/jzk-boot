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
@TableName("jk_subscription_message_log")
public class JkSubscriptionMessageLog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long taskId;
    private Integer attemptNo;
    private String requestJson;
    private String responseJson;
    private String status;
    private String errorMessage;
    private Date createTime;
}
