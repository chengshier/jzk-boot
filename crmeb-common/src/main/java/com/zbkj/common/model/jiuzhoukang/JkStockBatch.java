package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 库存批次。可用量和冻结量必须与库存明细汇总保持一致。 */
@Data @Accessors(chain = true) @TableName("jk_stock_batch")
public class JkStockBatch implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO) private Long id;
    private String batchNo;
    private String rootBatchNo;
    private Long sourceBatchId;
    private Long stockAccountId;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private Integer inboundQty;
    private Integer availableQty;
    private Integer frozenQty;
    private Integer outboundQty;
    private BigDecimal unitCost;
    private Date productionDate;
    private Date expireTime;
    private Date inboundTime;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private String status;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
    @TableField(exist=false) private Integer ageDays;
    @TableField(exist=false) private String ageLevel;
    @TableField(exist=false) private String productName;
    @TableField(exist=false) private String skuName;
    @TableField(exist=false) private String accountName;
}
