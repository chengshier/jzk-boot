package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/** 健康设备主数据。设备绑定码只保存摘要，不保存明文。 */
@Data
@Accessors(chain = true)
@TableName("jk_health_device")
public class JkHealthDevice implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String deviceSn;
    private String providerCode;
    /** 厂商侧设备唯一 ID；deviceSn 仍是九州康对用户展示的编号。 */
    private String externalDeviceId;
    private String deviceType;
    private String deviceModel;
    private String bindCodeHash;
    private String status;
    private Date lastSyncTime;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    @TableField(exist = false) private Long boundUserId;
    @TableField(exist = false) private String boundUserName;
}
