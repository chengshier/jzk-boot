package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 上级直属人数额度使用快照。真实有效关系仍以 jk_agent_relation 为准。 */
@Data
@Accessors(chain = true)
@TableName("jk_relation_quota_usage")
public class JkRelationQuotaUsage implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long parentUserId;
    private Integer usedCount;
    private Integer reservedCount;
    private Integer version;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private String tenantId;
    private Long createDept;
}
