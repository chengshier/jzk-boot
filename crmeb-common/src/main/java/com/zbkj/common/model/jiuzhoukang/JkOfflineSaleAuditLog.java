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
@TableName("jk_offline_sale_audit_log")
public class JkOfflineSaleAuditLog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long saleId;
    private String action;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorUserId;
    private String operatorType;
    private String remark;
    private String requestNo;
    private Date createTime;
}
