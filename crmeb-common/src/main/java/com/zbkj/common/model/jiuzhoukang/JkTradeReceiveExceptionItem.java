package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 异常收货商品差异明细。 */
@Data
@Accessors(chain = true)
@TableName("jk_trade_receive_exception_item")
public class JkTradeReceiveExceptionItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long exceptionId;
    private Long businessItemId;
    private Integer productId;
    private Integer skuId;
    private String productName;
    private String skuName;
    private String skuCode;
    private Integer expectedQty;
    private Integer receivedQty;
    private Integer damagedQty;
    private Integer shortageQty;
    private String itemRemark;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
