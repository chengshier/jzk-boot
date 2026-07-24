package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jk_region_agent")
public class JkRegionAgent implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String regionCode;
    private Long countyAgentUserId;
    private String bindStatus;
    private Date effectiveTime;
    private Date expireTime;
    private String changeReason;
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
