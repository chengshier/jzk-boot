package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/**
 * 健康数据授权。
 * <p>授权是健康顾问/管理员查看他人明细的唯一业务依据；代理身份本身不自动获得健康数据访问权。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_health_authorization")
public class JkHealthAuthorization implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String authorizationNo;
    private Long ownerUserId;
    private Long granteeUserId;
    private String granteeRoleCode;
    private String scopeCodes;
    /** 是否允许被授权人导出；查看权限不自动等于导出权限。 */
    private Boolean allowExport;
    private Date effectiveTime;
    private Date expireTime;
    private String status;
    private Date revokeTime;
    private String revokeReason;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    @TableField(exist = false) private String ownerName;
    @TableField(exist = false) private String granteeName;
    @TableField(exist = false) private String statusText;
}
