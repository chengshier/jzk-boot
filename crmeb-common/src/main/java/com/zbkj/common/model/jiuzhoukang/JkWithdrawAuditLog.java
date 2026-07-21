package com.zbkj.common.model.jiuzhoukang;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import lombok.experimental.Accessors; import java.io.Serializable; import java.util.Date;
@Data @Accessors(chain=true) @TableName("jk_withdraw_audit_log") public class JkWithdrawAuditLog implements Serializable {
 @TableId(value="id",type=IdType.AUTO) private Long id; private Long withdrawApplyId; private String withdrawNo; private String action; private String beforeStatus; private String afterStatus; private Long operatorId; private String remark; private String requestNo; private String idempotencyKey; private Date createTime;
}
