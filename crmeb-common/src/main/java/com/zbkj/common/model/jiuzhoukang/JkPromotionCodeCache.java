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
@TableName("jk_promotion_code_cache")
public class JkPromotionCodeCache implements Serializable {
    @TableId(value="id",type=IdType.AUTO) private Long id;
    private Long sceneId;
    private Long ownerUserId;
    private String sceneValue;
    private Long fileObjectId;
    private String status;
    private String errorMessage;
    private Date generatedAt;
    private Date expireTime;
    private String requestNo;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
