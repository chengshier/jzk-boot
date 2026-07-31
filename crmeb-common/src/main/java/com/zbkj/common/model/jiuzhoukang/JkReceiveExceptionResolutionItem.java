package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 异常收货 V2 分 SKU 处理明细。 */
@Data
@Accessors(chain = true)
@TableName("jk_receive_exception_resolution_item")
public class JkReceiveExceptionResolutionItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long resolutionId;
    private Long exceptionItemId;
    private Long businessItemId;
    private Integer productId;
    private Integer skuId;
    private Integer acceptedQty;
    private Integer reshipQty;
    private Integer refundQty;
    private Integer returnQty;
    private String logisticsCompany;
    private String logisticsNo;
    private String itemRemark;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
