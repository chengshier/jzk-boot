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
    /**
     * 兼容历史内部调用保留。任务服务不会信任此字段，而是按 receiverUserId 从已认证小程序登录关系重新解析 openId。
     * 前端不得提供或覆盖该值。
     */
    private String recipientOpenId;
    private String pagePath;
    private String payloadJson;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    private Integer maxRetryCount;
}
