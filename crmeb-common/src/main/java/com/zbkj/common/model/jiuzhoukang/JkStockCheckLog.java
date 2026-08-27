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
@TableName("jk_stock_check_log")
public class JkStockCheckLog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long checkId;
    private String action;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorUserId;
    private String operatorType;
    private String requestNo;
    private String remark;
    private Date createTime;
}
