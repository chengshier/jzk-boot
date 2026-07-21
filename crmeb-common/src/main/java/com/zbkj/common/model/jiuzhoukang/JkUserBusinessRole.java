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
@TableName("jk_user_business_role")
@ApiModel(value = "JkUserBusinessRole对象", description = "九州康用户业务角色绑定表")
public class JkUserBusinessRole implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
    private String roleCode;
    private Boolean isPrimary;
    private String auditStatus;
    private Boolean freezeStatus;
    private String effectiveStatus;
    private Long applyId;
    private String regionCode;
    private Long belongCountyAgentId;
    private Date effectiveTime;
    private Date expireTime;
    private String freezeReason;
    private String businessNo;
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
