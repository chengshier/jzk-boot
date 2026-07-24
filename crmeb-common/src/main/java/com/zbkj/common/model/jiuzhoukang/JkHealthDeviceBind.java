package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/** 设备与用户的绑定历史。解除绑定时保留原记录，不覆盖历史。 */
@Data
@Accessors(chain = true)
@TableName("jk_health_device_bind")
public class JkHealthDeviceBind implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String bindNo;
    private Long deviceId;
    private Long userId;
    private String status;
    private String bindSource;
    private Date bindTime;
    private Date unbindTime;
    private String unbindReason;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    @TableField(exist = false) private String userName;
    @TableField(exist = false) private String deviceSn;
    @TableField(exist = false) private String deviceType;
    @TableField(exist = false) private String deviceModel;
}
