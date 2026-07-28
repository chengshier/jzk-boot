package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_operation_profit_record")
public class JkOperationProfitRecord implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String profitNo;
    private Long userId;
    private String roleCode;
    private String incomeNature;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long sourceItemId;
    private Integer productId;
    private Integer skuId;
    private Integer quantity;
    private BigDecimal revenueAmount;
    private BigDecimal costAmount;
    private BigDecimal profitAmount;
    private BigDecimal reversedAmount;
    private String status;
    private String costSnapshotJson;
    private String relationSnapshotJson;
    private String requestNo;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
