package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import java.util.Date;

@Data @Accessors(chain = true) @TableName("jk_sinocare_authorization")
public class JkSinocareAuthorization {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String uniqueId; private Long userId; private String status;
    private Date authorizedAt; private Date revokedAt; private String sourceEventId;
    private Date createTime; private Date updateTime;
}
