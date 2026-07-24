package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/**
 * 健康设备厂商接入配置。
 * <p>同一厂商可以配置 CALLBACK、PULL 或 HYBRID，认证密钥和扩展映射以密文保存。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_health_provider")
public class JkHealthProvider implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String providerCode;
    private String providerName;
    private String adapterType;
    private String syncMode;
    private String authType;
    private String baseUrl;
    private String callbackPath;
    private String credentialCipher;
    private String configCipher;
    private String pullCursor;
    private Date lastPullTime;
    private Date nextPullTime;
    private String lastPullStatus;
    private String lastErrorMessage;
    private Integer retryCount;
    private Boolean enabled;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
    @TableField(exist = false) private Boolean callbackSupported;
    @TableField(exist = false) private Boolean pullSupported;
    @TableField(exist = false) private Boolean credentialConfigured;
    @TableField(exist = false) private String statusText;
}
