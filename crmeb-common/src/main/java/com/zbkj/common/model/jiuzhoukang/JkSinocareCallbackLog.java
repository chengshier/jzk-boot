package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import java.util.Date;

@Data @Accessors(chain = true) @TableName("jk_sinocare_callback_log")
public class JkSinocareCallbackLog {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String eventType; private String eventId; private String uniqueId;
    private String payloadCipher; private String signature; private String processStatus;
    private String errorMessage; private Integer retryCount; private Date createTime; private Date updateTime;
}
