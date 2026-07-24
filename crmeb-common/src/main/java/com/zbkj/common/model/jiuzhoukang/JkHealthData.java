package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 统一健康数据记录。
 * <p>GLUCOSE 使用 numericValue；饮食、运动、用药等扩展信息放入 detailCipher。</p>
 * <p>externalNo 是第三方同步幂等键，同一来源重复回调不得重复入库。</p>
 */
@Data
@Accessors(chain = true)
@TableName("jk_health_data")
public class JkHealthData implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String externalNo;
    private Long userId;
    private Long deviceId;
    private String dataType;
    private BigDecimal numericValue;
    private String unit;
    private String periodCode;
    private Date measuredAt;
    private String detailCipher;
    private String sourceType;
    private String riskLevel;
    private String status;
    private Boolean isDeleted;
    private Long createUserId;
    private Long updateUserId;
    private Date createTime;
    private Date updateTime;
    private Integer version;

    @TableField(exist = false) private String detail;
    @TableField(exist = false) private String dataTypeText;
    @TableField(exist = false) private String riskLevelText;
}
