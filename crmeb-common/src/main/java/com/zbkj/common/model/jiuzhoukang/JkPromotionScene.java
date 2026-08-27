package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** V3.1 微信小程序码推广场景映射。sceneValue 为随机不透明值，不暴露业务用户 ID。 */
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

    /* 旧展示模型兼容字段，不写入 Phase3 最终表。 */
    @TableField(exist = false) private String sceneName;
    @TableField(exist = false) private String roleCodes;
    @TableField(exist = false) private String sceneTemplate;
    @TableField(exist = false) private Integer versionNo;
    @TableField(exist = false) private String remark;
}
