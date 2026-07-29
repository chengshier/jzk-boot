package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 每个业务动作唯一的预算占用记录，防止并发重试重复消耗封顶和预算。 */
@Data
@Accessors(chain = true)
@TableName("jk_commission_limit_reservation")
public class JkCommissionLimitReservation implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String actionKey;
    private Long ruleId;
    private Long userId;
    private String periodKey;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private String resultCode;
    private String resultMessage;
    private Date createTime;
}
