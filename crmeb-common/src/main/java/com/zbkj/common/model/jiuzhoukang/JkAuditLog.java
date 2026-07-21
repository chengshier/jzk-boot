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
@TableName("jk_audit_log")
@ApiModel(value = "JkAuditLog对象", description = "九州康审核日志表")
public class JkAuditLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private String requestNo;
    private Long auditUserId;
    private String auditUserName;
    private String auditUserType;
    private String auditAction;
    private String beforeStatus;
    private String afterStatus;
    private String rejectReason;
    private String auditRemark;
    private String operateSource;
    private Boolean status;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private String tenantId;
    private Long createDept;
}
