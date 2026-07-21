package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jk_stock_account")
@ApiModel(value = "JkStockAccount对象", description = "九州康库存账户表")
public class JkStockAccount implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String accountNo;
    private String accountType;
    private String roleCode;
    private String regionCode;
    private Long ownerUserId;
    private String ownerName;
    private Boolean status;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;
    private String tenantId;
    private Long createDept;
}
