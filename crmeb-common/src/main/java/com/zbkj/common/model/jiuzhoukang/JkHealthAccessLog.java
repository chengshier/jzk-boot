package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/**
 * 健康数据访问日志。
 * <p>无论允许还是拒绝都记录；日志只记录访问范围和结果，禁止写入血糖值、用药内容等明文。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_health_access_log")
public class JkHealthAccessLog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String requestNo;
    private Long viewerUserId;
    private Long ownerUserId;
    private Long authorizationId;
    private String actionType;
    private String scopeCode;
    private String accessResult;
    private String denyReason;
    private String operateSource;
    /** SELF/AUTHORIZED/EMERGENCY/ANONYMOUS_REPORT。 */
    private String accessType;
    /** 紧急核查或导出的业务原因，不得记录健康明文。 */
    private String accessReason;
    /** 平台后台管理员 ID；普通前台访问为空。 */
    private Integer adminId;
    private String clientIpMask;
    private Date accessTime;
    private Boolean isDeleted;
    private Date createTime;
}
