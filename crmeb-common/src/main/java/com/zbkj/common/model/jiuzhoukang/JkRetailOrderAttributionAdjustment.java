package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_retail_order_attribution_adjustment")
public class JkRetailOrderAttributionAdjustment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long attributionId;
    private String beforeSnapshotJson;
    private String afterSnapshotJson;
    private String adjustReason;
    private String adjustType;
    private Long operatorUserId;
    private Long auditUserId;
    private String status;
    private String requestNo;
    private Date createTime;
    private Date updateTime;
}
