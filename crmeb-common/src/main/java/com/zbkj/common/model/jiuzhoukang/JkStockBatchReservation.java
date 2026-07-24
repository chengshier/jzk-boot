package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/**
 * 业务单据对具体批次的冻结分配。
 * <p>释放和冻结出库必须读取该记录，避免按当前 FIFO 重新分配导致批次错乱。</p>
 */
@Data @Accessors(chain = true) @TableName("jk_stock_batch_reservation")
public class JkStockBatchReservation implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO) private Long id;
    private String reservationNo;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private Long batchId;
    private Long stockAccountId;
    private Integer productId;
    private Integer skuId;
    private Integer frozenQty;
    private Integer releasedQty;
    private Integer outboundQty;
    private String status;
    private Boolean isDeleted;
    private Long createUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
