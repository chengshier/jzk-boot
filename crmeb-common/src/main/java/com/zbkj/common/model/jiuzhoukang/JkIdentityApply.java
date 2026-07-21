package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jk_identity_apply")
@ApiModel(value = "JkIdentityApply对象", description = "九州康身份申请表")
public class JkIdentityApply implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String requestNo;
    private String applyNo;
    private String businessNo;
    private Long userId;
    private String applyRoleCode;
    private String realName;
    private String mobile;
    private String regionCode;
    private Long belongCountyAgentId;
    private Long parentUserId;
    private String applyReason;
    private String certificateFiles;
    private String auditStatus;
    private Boolean freezeStatus;
    private Integer currentAuditLevel;
    private String rejectReason;
    private Date effectiveTime;
    private Integer version;
    private Boolean status;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private String tenantId;
    private Long createDept;
}
