package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jk_business_role")
@ApiModel(value = "JkBusinessRole对象", description = "九州康业务角色表")
public class JkBusinessRole implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @ApiModelProperty(value = "业务角色编码")
    private String roleCode;
    @ApiModelProperty(value = "业务角色名称")
    private String roleName;
    private String roleType;
    private Integer roleLevel;
    private Boolean needAudit;
    private Boolean isSystem;
    private Boolean enabled;
    @ApiModelProperty(value = "是否允许前台申请")
    private Boolean allowFrontApply;
    private Integer sort;
    private String remark;
    private Boolean status;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private String tenantId;
    private Long createDept;
}
