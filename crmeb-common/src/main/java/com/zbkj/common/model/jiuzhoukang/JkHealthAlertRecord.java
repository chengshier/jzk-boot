package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 预警记录。每条健康数据对同一规则最多生成一条预警。 */
@Data
@Accessors(chain = true)
@TableName("jk_health_alert_record")
public class JkHealthAlertRecord implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long healthDataId;
    private Long ruleId;
    private Long userId;
    private String dataType;
    private BigDecimal measuredValue;
    private String alertLevel;
    private String status;
    private Long processUserId;
    private Date processTime;
    private String processRemark;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    @TableField(exist = false) private String userName;
    @TableField(exist = false) private String statusText;
}
