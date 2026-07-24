package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/**
 * 第三方健康数据同步日志。
 * <p>payloadCipher 保存通过验签后的加密请求快照，用于处理失败后的安全重试；后台接口永不回显该字段。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_health_sync_log")
public class JkHealthSyncLog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String syncNo;
    private String providerCode;
    private String deviceSn;
    private String externalNo;
    private String payloadCipher;
    private String syncStatus;
    private Integer retryCount;
    private Date nextRetryTime;
    private Date lastRetryTime;
    private Long healthDataId;
    private String errorMessage;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
    @TableField(exist = false) private String statusText;
}
