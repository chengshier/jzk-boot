package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 普通零售订单退款发生额。
 * <p>归属快照表保存累计退款，本表保存每一次退款的发生日期和分摊金额，
 * 用于“退款发生月份记负数”的第六阶段报表，避免通过累计值或 update_time 反推当期退款。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_retail_refund_adjustment")
public class JkRetailRefundAdjustment implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String adjustmentNo;
    private String requestNo;
    private Long orderId;
    private String orderNo;
    private Long orderInfoId;
    private Long attributionId;
    private Long buyerUserId;
    private Long receiverUserId;
    private Long countyAgentUserId;
    private String regionCode;
    /** 正数保存退款发生额，报表展示时记为负数。 */
    private BigDecimal adjustmentAmount;
    private Date occurredTime;
    private Date originalBusinessTime;
    private String idempotencyKey;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
