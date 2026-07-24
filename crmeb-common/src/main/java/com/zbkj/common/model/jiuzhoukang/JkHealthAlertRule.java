package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 健康预警规则。ownerUserId 为空表示平台默认规则，个人规则优先于平台规则。 */
@Data
@Accessors(chain = true)
@TableName("jk_health_alert_rule")
public class JkHealthAlertRule implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String ruleName;
    private Long ownerUserId;
    private String dataType;
    private String periodCode;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String alertLevel;
    private Boolean enabled;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
