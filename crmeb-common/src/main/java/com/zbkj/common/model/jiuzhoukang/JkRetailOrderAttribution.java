package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 普通零售订单归属与实付金额分摊快照。订单完成和退款只能读取本快照。 */
@TableName("jk_retail_order_attribution")
public class JkRetailOrderAttribution implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long orderInfoId;
    private Long buyerUserId;
    private Long directParentUserId;
    private Long countyAgentUserId;
    private String regionCode;
    private String attributionType;
    private Long relationId;
    private String relationSource;
    private Long receiverUserId;
    private String receiverRoleCode;
    private BigDecimal itemOriginalAmount;
    private BigDecimal itemDiscountAmount;
    private BigDecimal itemPaidAmount;
    private BigDecimal refundedAmount;
    private BigDecimal commissionBaseAmount;
    private String snapshotJson;
    private String requestNo;
    private String lastRefundRequestNo;
    private String idempotencyKey;
    private Integer version;
    private Boolean status;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public JkRetailOrderAttribution setId(Long id) { this.id = id; return this; }

    public Long getOrderId() { return orderId; }
    public JkRetailOrderAttribution setOrderId(Long orderId) { this.orderId = orderId; return this; }

    public String getOrderNo() { return orderNo; }
    public JkRetailOrderAttribution setOrderNo(String orderNo) { this.orderNo = orderNo; return this; }

    public Long getOrderInfoId() { return orderInfoId; }
    public JkRetailOrderAttribution setOrderInfoId(Long orderInfoId) { this.orderInfoId = orderInfoId; return this; }

    public Long getBuyerUserId() { return buyerUserId; }
    public JkRetailOrderAttribution setBuyerUserId(Long buyerUserId) { this.buyerUserId = buyerUserId; return this; }

    public Long getDirectParentUserId() { return directParentUserId; }
    public JkRetailOrderAttribution setDirectParentUserId(Long directParentUserId) { this.directParentUserId = directParentUserId; return this; }

    public Long getCountyAgentUserId() { return countyAgentUserId; }
    public JkRetailOrderAttribution setCountyAgentUserId(Long countyAgentUserId) { this.countyAgentUserId = countyAgentUserId; return this; }

    public String getRegionCode() { return regionCode; }
    public JkRetailOrderAttribution setRegionCode(String regionCode) { this.regionCode = regionCode; return this; }

    public String getAttributionType() { return attributionType; }
    public JkRetailOrderAttribution setAttributionType(String attributionType) { this.attributionType = attributionType; return this; }

    public Long getRelationId() { return relationId; }
    public JkRetailOrderAttribution setRelationId(Long relationId) { this.relationId = relationId; return this; }

    public String getRelationSource() { return relationSource; }
    public JkRetailOrderAttribution setRelationSource(String relationSource) { this.relationSource = relationSource; return this; }

    public Long getReceiverUserId() { return receiverUserId; }
    public JkRetailOrderAttribution setReceiverUserId(Long receiverUserId) { this.receiverUserId = receiverUserId; return this; }

    public String getReceiverRoleCode() { return receiverRoleCode; }
    public JkRetailOrderAttribution setReceiverRoleCode(String receiverRoleCode) { this.receiverRoleCode = receiverRoleCode; return this; }

    public BigDecimal getItemOriginalAmount() { return itemOriginalAmount; }
    public JkRetailOrderAttribution setItemOriginalAmount(BigDecimal itemOriginalAmount) { this.itemOriginalAmount = itemOriginalAmount; return this; }

    public BigDecimal getItemDiscountAmount() { return itemDiscountAmount; }
    public JkRetailOrderAttribution setItemDiscountAmount(BigDecimal itemDiscountAmount) { this.itemDiscountAmount = itemDiscountAmount; return this; }

    public BigDecimal getItemPaidAmount() { return itemPaidAmount; }
    public JkRetailOrderAttribution setItemPaidAmount(BigDecimal itemPaidAmount) { this.itemPaidAmount = itemPaidAmount; return this; }

    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public JkRetailOrderAttribution setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; return this; }

    public BigDecimal getCommissionBaseAmount() { return commissionBaseAmount; }
    public JkRetailOrderAttribution setCommissionBaseAmount(BigDecimal commissionBaseAmount) { this.commissionBaseAmount = commissionBaseAmount; return this; }

    public String getSnapshotJson() { return snapshotJson; }
    public JkRetailOrderAttribution setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; return this; }

    public String getRequestNo() { return requestNo; }
    public JkRetailOrderAttribution setRequestNo(String requestNo) { this.requestNo = requestNo; return this; }

    public String getLastRefundRequestNo() { return lastRefundRequestNo; }
    public JkRetailOrderAttribution setLastRefundRequestNo(String lastRefundRequestNo) { this.lastRefundRequestNo = lastRefundRequestNo; return this; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public JkRetailOrderAttribution setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }

    public Integer getVersion() { return version; }
    public JkRetailOrderAttribution setVersion(Integer version) { this.version = version; return this; }

    public Boolean getStatus() { return status; }
    public JkRetailOrderAttribution setStatus(Boolean status) { this.status = status; return this; }

    public Boolean getIsDeleted() { return isDeleted; }
    public JkRetailOrderAttribution setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; return this; }

    public Date getCreateTime() { return createTime; }
    public JkRetailOrderAttribution setCreateTime(Date createTime) { this.createTime = createTime; return this; }

    public Date getUpdateTime() { return updateTime; }
    public JkRetailOrderAttribution setUpdateTime(Date updateTime) { this.updateTime = updateTime; return this; }

}