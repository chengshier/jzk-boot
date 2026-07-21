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
@TableName("jk_admin_user_mapping")
@ApiModel(value = "JkAdminUserMapping对象", description = "后台管理员与前台业务用户映射表")
public class JkAdminUserMapping implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer systemAdminId;
    private Long frontUserId;
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
