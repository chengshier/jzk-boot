package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 商业方案不可变版本行。发布后只能复制新草稿版本，不能覆盖历史业务使用的版本。 */
@Data
@Accessors(chain = true)
@TableName("jk_business_rule_plan")
public class JkBusinessRulePlan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String planCode;
    private String planName;
    private Integer versionNo;
    private String status;
    private String applicableRoleCodes;
    private String applicableRegionCodes;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Integer priority;
    private String publishStatus;
    private Long publishedBy;
    private Date publishedAt;
    private Long disabledBy;
    private Date disabledAt;
    private String changeSummary;
    private String remark;
    private Boolean isDeleted;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
