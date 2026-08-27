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
@TableName("jk_promotion_stat")
public class JkPromotionStat implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long sceneId;
    private Date statDate;
    private Integer scanCount;
    private Integer newUserCount;
    private Integer initialBindCount;
    private Integer effectiveBindCount;
    private Integer buyerCount;
    private BigDecimal saleAmount;
    private Date createTime;
    private Date updateTime;
}
