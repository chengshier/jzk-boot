package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 换绑申请占用的目标上级额度，避免审核期间并发超额。 */
@Data
@Accessors(chain = true)
@TableName("jk_relation_quota_reservation")
public class JkRelationQuotaReservation implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String reservationNo;
    private String requestNo;
    private String scene;
    private Long parentUserId;
    private Long childUserId;
    private Long ruleId;
    private String status;
    private Date expireTime;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private String tenantId;
    private Long createDept;
}
