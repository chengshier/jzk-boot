package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 普通零售订单逐明细不可变归属与实付分摊快照。
 * 支付、完成、退款只能读取本记录；已锁定记录不得直接改受益人、关系或最终区域。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jk_retail_order_attribution")
public class JkRetailOrderAttribution implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String attributionNo;
    private Long orderId;
    private String orderNo;
    private Long orderInfoId;
    private Long buyerUserId;
    private Long productId;
    private Long skuId;
    private Integer quantity;

    private Long directParentUserId;
    private String directParentRoleCode;
    private Long countyAgentUserId;
    private Long receiverUserId;
    private String receiverRoleCode;

    /** 兼容旧查询，始终与 finalRegionCode 同步。 */
    private String regionCode;
    private String profileRegionCode;
    private String shippingRegionCode;
    private String finalRegionCode;
    private String finalRegionNameSnapshot;
    private String regionSourceType;

    /** 兼容旧记录：DIRECT_PARENT/REGION_AGENT/PLATFORM。 */
    private String attributionType;
    private Long relationId;
    private String relationSource;
    private Long shippingAddressId;

    private BigDecimal itemOriginalAmount;
    private BigDecimal itemDiscountAmount;
    private BigDecimal itemPaidAmount;
    private BigDecimal freightAllocatedAmount;
    private BigDecimal refundedAmount;
    private BigDecimal refundAmount;
    private BigDecimal commissionBaseAmount;

    /** 旧聚合快照，保留兼容；新逻辑同时写入拆分快照。 */
    private String snapshotJson;
    private String relationSnapshotJson;
    private String profileSnapshotJson;
    private String shippingAddressSnapshotJson;
    private String regionResolutionSnapshotJson;
    private String priceSnapshotJson;
    private String ruleContextSnapshotJson;

    private String attributionStatus;
    private String lockStatus;
    private String requestNo;
    private String lastRefundRequestNo;
    private String idempotencyKey;
    private Integer version;
    private Boolean status;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
