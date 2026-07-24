package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 第六阶段风险扫描规则。
 * <p>规则只负责发现并生成风险事件，不允许直接修改库存、佣金、资金或健康数据。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_risk_rule")
public class JkRiskRule implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String ruleCode;
    private String ruleName;
    private String scannerType;
    private String riskType;
    private String riskLevel;
    private BigDecimal thresholdValue;
    private Integer windowHours;
    private String configJson;
    private Boolean enabled;
    private Date lastScanTime;
    private String lastScanStatus;
    private String lastErrorMessage;
    private String remark;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
