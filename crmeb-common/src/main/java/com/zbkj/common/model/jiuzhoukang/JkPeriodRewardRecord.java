package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 周期关闭时的阶梯奖励结果；佣金记录通过 commission_record_id 追溯。 */
@Data
@Accessors(chain = true)
@TableName("jk_period_reward_record")
public class JkPeriodRewardRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String rewardNo;
    private Long periodId;
    private Long ownerUserId;
    private Long tierRuleId;
    private Long tierRuleItemId;
    private BigDecimal performanceAmount;
    private BigDecimal rawRewardAmount;
    private BigDecimal approvedRewardAmount;
    private String status;
    private Long commissionRecordId;
    private String calculationSnapshotJson;
    private String requestNo;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
