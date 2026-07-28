package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 上下级直属人数限制规则。
 *
 * <p>规则支持按上级角色、下级角色和区域收窄。空值表示通配，优先级越大越先匹配。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_relation_limit_rule")
public class JkRelationLimitRule implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String ruleCode;
    private String ruleName;
    private Long planId;
    private String versionNo;
    private String parentRoleCode;
    private String childRoleCode;
    private String regionCode;
    private Integer maxDirectChildren;
    private Integer warningThreshold;
    private String overflowPolicy;
    private Integer priority;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Boolean status;
    private String remark;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private String tenantId;
    private Long createDept;

    /** 当前规则是否在有效期内，仅用于响应展示。 */
    @TableField(exist = false)
    private Boolean effective;
}
