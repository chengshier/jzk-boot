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

/** 订货/调拨异常收货主记录。V2 处理方案必须双向确认后才允许落库存和账本。 */
@Data
@Accessors(chain = true)
@TableName("jk_trade_receive_exception")
public class JkTradeReceiveException implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String exceptionNo;
    private String requestNo;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private Long receiverUserId;
    private String status;
    private String exceptionType;
    private Integer expectedTotalQty;
    private Integer receivedTotalQty;
    private Integer shortageTotalQty;
    private Integer damagedTotalQty;
    private String exceptionReason;
    private String evidenceJson;
    private String handleAction;
    private String handleRemark;
    private Long handleUserId;
    private Date handleTime;

    private String resolutionType;
    private Integer normalReceivedQty;
    private Integer exceptionQty;
    private BigDecimal refundAmount;
    private BigDecimal compensationAmount;
    private String receiverConfirmStatus;
    private String senderConfirmStatus;
    private String resolutionSnapshotJson;

    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    @TableField(exist = false) private String statusText;
    @TableField(exist = false) private String statusTag;
    @TableField(exist = false) private String businessTypeText;
    @TableField(exist = false) private String exceptionTypeText;
    @TableField(exist = false) private String receiverName;
    @TableField(exist = false) private String receiverPhone;
}
