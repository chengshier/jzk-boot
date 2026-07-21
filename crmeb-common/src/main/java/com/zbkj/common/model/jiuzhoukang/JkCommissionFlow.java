package com.zbkj.common.model.jiuzhoukang;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import lombok.experimental.Accessors; import java.io.Serializable; import java.math.BigDecimal; import java.util.Date;
@Data @Accessors(chain=true) @TableName("jk_commission_flow") public class JkCommissionFlow implements Serializable {
 @TableId(value="id",type=IdType.AUTO) private Long id; private String flowNo; private Long accountId; private Long commissionRecordId; private String flowType; private BigDecimal changeAmount; private BigDecimal beforeAmount; private BigDecimal afterAmount; private String sourceType; private Long sourceId; private String requestNo; private String idempotencyKey; private String remark; private Date createTime;
}
