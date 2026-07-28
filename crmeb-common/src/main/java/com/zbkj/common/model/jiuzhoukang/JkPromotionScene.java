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
@TableName("jk_promotion_scene")
public class JkPromotionScene implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String sceneCode;
    private Long promoterUserId;
    private String promoterRoleCode;
    private String regionCode;
    private String pagePath;
    private String sceneValue;
    private String objectKey;
    private String status;
    private String disabledReason;
    private Date expireTime;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
