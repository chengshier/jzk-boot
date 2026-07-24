package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zbkj.common.response.jiuzhoukang.JkRelationChangeBlockerResponse;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

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
    /** 当前实时阻断检查是否全部通过。 */
    @TableField(exist = false) private Boolean blockerPassed;
    /** 当前实时阻断项明细，仅详情接口填充。 */
    @TableField(exist = false) private List<JkRelationChangeBlockerResponse> blockerItems;
    @TableField(exist = false) private Date blockerCheckTime;
}
