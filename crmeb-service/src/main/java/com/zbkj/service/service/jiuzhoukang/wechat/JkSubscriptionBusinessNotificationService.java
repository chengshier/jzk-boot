package com.zbkj.service.service.jiuzhoukang.wechat;

import java.math.BigDecimal;

/**
 * 九州康业务节点订阅消息入口。
 *
 * <p>调用方只传递可信业务结果，不接收前端 openId。实现层负责从已认证的小程序登录关系解析接收人，
 * 并将任务写入订阅消息队列。通知失败不得影响已经完成的主业务。</p>
 */
public interface JkSubscriptionBusinessNotificationService {

    void notifyAuditResult(String businessType, Long businessId, String businessNo, Long receiverUserId,
                           String subject, String status, String remark, String pagePath);

    void notifyTransferStatus(String businessType, Long businessId, String businessNo, Long receiverUserId,
                              String status, String remark, String pagePath);

    void notifyReceiveReminder(String businessType, Long businessId, String businessNo, Long receiverUserId,
                               String subject, String remark, String pagePath);

    void notifyWithdrawStatus(Long withdrawId, String withdrawNo, Long receiverUserId, BigDecimal amount,
                              String status, String remark, String pagePath);
}
