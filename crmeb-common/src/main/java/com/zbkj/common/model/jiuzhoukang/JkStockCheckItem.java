package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_stock_check_item")
public class JkStockCheckItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long checkId;
    private Long stockItemId;
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private Integer bookAvailableQty;
    private Integer bookFrozenQty;
    private Integer actualAvailableQty;
    private Integer differenceQty;
    private String differenceType;
    private String remark;
    private Integer versionSnapshot;
    private Boolean adjusted;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
