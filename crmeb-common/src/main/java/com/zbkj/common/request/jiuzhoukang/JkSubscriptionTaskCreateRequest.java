package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 可信业务服务创建订阅消息任务时使用。 */
@Data
public class JkSubscriptionTaskCreateRequest {
    @NotBlank(message = "模板编码不能为空") private String templateCode;
    private String businessType;
    private Long businessId;
    @NotNull(message = "接收用户不能为空") private Long receiverUserId;
    /** 只能来自已认证微信登录上下文；为空时任务进入 WAIT_RECIPIENT，不允许前端任意覆盖。 */
    private String recipientOpenId;
    private String pagePath;
    private String payloadJson;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    private Integer maxRetryCount;
}
