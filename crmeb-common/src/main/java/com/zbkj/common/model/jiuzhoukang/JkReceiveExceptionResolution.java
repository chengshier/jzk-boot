package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/** 异常收货 V2 处理方案。 */
@Data
@Accessors(chain = true)
@TableName("jk_receive_exception_resolution")
public class JkReceiveExceptionResolution implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long exceptionId;
    private String resolutionNo;
    private String resolutionType;
    private String resolutionStatus;
    private Integer acceptedQuantity;
    private Integer reshipQuantity;
    private BigDecimal refundAmount;
    private BigDecimal claimAmount;
    private String responsibilityParty;
    private String evidenceUrls;
    private String resolutionJson;
    private Long operatorUserId;
    private Date completedAt;
    private String requestNo;
    private String remark;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false) private String resolutionTypeText;
    @TableField(exist = false) private String resolutionStatusText;
    @TableField(exist = false) private List<JkReceiveExceptionResolutionItem> items;
}
