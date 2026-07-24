package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/** 批次级库存流水，用于追溯 FIFO 分配和跨账户批次传递。 */
@Data @Accessors(chain = true) @TableName("jk_stock_batch_flow")
public class JkStockBatchFlow implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO) private Long id;
    private String flowNo;
    private String idempotencyKey;
    private Long batchId;
    private Long sourceBatchId;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private Long stockAccountId;
    private Integer productId;
    private Integer skuId;
    private String flowType;
    private Integer changeQty;
    private Integer beforeAvailableQty;
    private Integer afterAvailableQty;
    private Integer beforeFrozenQty;
    private Integer afterFrozenQty;
    private String remark;
    private Boolean isDeleted;
    private Long createUserId;
    private Date createTime;
}
