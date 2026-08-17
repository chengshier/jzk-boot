package com.zbkj.common.response.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkSinocareCallbackLog;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 后台三诺回调日志视图，刻意不含密文与签名。 */
@Data
@Accessors(chain = true)
public class JkSinocareCallbackLogResponse {
    private Long id;
    private String eventType;
    private String eventId;
    private String uniqueId;
    private String processStatus;
    private String errorMessage;
    private Integer retryCount;
    private Date receivedAt;
    private Date processedAt;

    public static JkSinocareCallbackLogResponse from(JkSinocareCallbackLog source) {
        return new JkSinocareCallbackLogResponse()
                .setId(source.getId()).setEventType(source.getEventType()).setEventId(source.getEventId())
                .setUniqueId(source.getUniqueId()).setProcessStatus(source.getProcessStatus())
                .setErrorMessage(source.getErrorMessage()).setRetryCount(source.getRetryCount())
                .setReceivedAt(source.getCreateTime()).setProcessedAt(source.getUpdateTime());
    }
}
