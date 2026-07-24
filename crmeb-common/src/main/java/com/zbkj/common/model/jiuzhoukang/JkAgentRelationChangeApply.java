package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_agent_relation_change_apply")
public class JkAgentRelationChangeApply implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String applyNo;
    private String requestNo;
    private Long userId;
    private Long currentRelationId;
    private Long currentParentUserId;
    private Long targetParentUserId;
    private String applyReason;
    private String status;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private String rejectReason;
    private Long newRelationId;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false) private String userName;
    @TableField(exist = false) private String userPhone;
    @TableField(exist = false) private String currentParentName;
    @TableField(exist = false) private String targetParentName;
    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
}
