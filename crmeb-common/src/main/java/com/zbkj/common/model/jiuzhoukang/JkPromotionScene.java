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
    @TableId(value="id",type=IdType.AUTO) private Long id;
    private String sceneCode;
    private String sceneName;
    private String pagePath;
    private String roleCodes;
    private String sceneTemplate;
    private Integer versionNo;
    private Boolean status;
    private String remark;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
