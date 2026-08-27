package com.zbkj.common.model.jiuzhoukang;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import lombok.experimental.Accessors; import java.util.Date;
@Data @Accessors(chain=true) @TableName("jk_sinocare_device_session") public class JkSinocareDeviceSession { @TableId(value="id",type=IdType.AUTO) private Long id; private String uniqueId; private String deviceSn; private Integer status; private String productName; private Date detectionStartTime; private Date detectionEndTime; private Date lastDataAt; private Date createTime; private Date updateTime; }
