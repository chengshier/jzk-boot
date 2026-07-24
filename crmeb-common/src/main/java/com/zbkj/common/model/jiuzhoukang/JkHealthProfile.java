package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户健康档案。
 * <p>这里只保存健康域需要的最小资料，不复制 CRMEB 用户表中的手机号、昵称等通用信息。</p>
 * <p>remarkCipher 可能包含敏感说明，必须通过健康敏感数据组件读写，禁止直接打印到日志。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_health_profile")
public class JkHealthProfile implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long userId;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String diabetesType;
    private BigDecimal glucoseTargetMin;
    private BigDecimal glucoseTargetMax;
    private String remarkCipher;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    @TableField(exist = false) private String remark;
}
