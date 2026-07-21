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
@TableName("jk_business_permission")
@ApiModel(value = "JkBusinessPermission对象", description = "九州康业务权限点表")
public class JkBusinessPermission implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String moduleCode;
    private String permissionType;
    private Boolean enabled;
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
