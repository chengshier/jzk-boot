package com.zbkj.service.service.impl.jiuzhoukang.wechat;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zbkj.common.model.user.UserToken;
import com.zbkj.common.request.jiuzhoukang.JkSubscriptionTaskCreateRequest;
import com.zbkj.service.service.UserTokenService;
import com.zbkj.service.service.jiuzhoukang.wechat.JkSubscriptionBusinessNotificationService;
import com.zbkj.service.service.jiuzhoukang.wechat.JkSubscriptionTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将可信业务结果转换为订阅消息任务。
 *
 * <p>微信模板字段名称因账号模板而异，因此这里保存语义字段，再通过可配置映射生成微信 data。
 * 默认映射只作为开发占位，正式启用订阅总开关前必须按实际模板字段核对并覆盖。</p>
 */
@Service
public class JkSubscriptionBusinessNotificationServiceImpl implements JkSubscriptionBusinessNotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JkSubscriptionBusinessNotificationServiceImpl.class);
    private static final int MINI_PROGRAM_TOKEN_TYPE = 2;

    @Autowired private JkSubscriptionTaskService taskService;
    @Autowired private UserTokenService userTokenService;

    @Value("${jk.wechat.subscribe.audit-field-mapping:businessNo=character_string1,subject=thing2,status=phrase3,remark=thing4,time=time5}")
    private String auditFieldMapping;
    @Value("${jk.wechat.subscribe.transfer-field-mapping:businessNo=character_string1,status=phrase2,remark=thing3,time=time4}")
    private String transferFieldMapping;
    @Value("${jk.wechat.subscribe.receive-field-mapping:businessNo=character_string1,subject=thing2,remark=thing3,time=time4}")
    private String receiveFieldMapping;
    @Value("${jk.wechat.subscribe.withdraw-field-mapping:businessNo=character_string1,amount=amount2,status=phrase3,remark=thing4,time=time5}")
    private String withdrawFieldMapping;

    @Override
    public void notifyAuditResult(String businessType, Long businessId, String businessNo, Long receiverUserId,
                                  String subject, String status, String remark, String pagePath) {
        Map<String, String> semantic = common(businessNo, remark);
        semantic.put("subject", safe(subject));
        semantic.put("status", safe(status));
        enqueueSafely("AUDIT_RESULT", businessType, businessId, receiverUserId, pagePath,
                semantic, auditFieldMapping, status);
    }

    @Override
    public void notifyTransferStatus(String businessType, Long businessId, String businessNo, Long receiverUserId,
                                     String status, String remark, String pagePath) {
        Map<String, String> semantic = common(businessNo, remark);
        semantic.put("status", safe(status));
        enqueueSafely("TRANSFER_STATUS", businessType, businessId, receiverUserId, pagePath,
                semantic, transferFieldMapping, status);
    }

    @Override
    public void notifyReceiveReminder(String businessType, Long businessId, String businessNo, Long receiverUserId,
                                      String subject, String remark, String pagePath) {
        Map<String, String> semantic = common(businessNo, remark);
        semantic.put("subject", safe(subject));
        enqueueSafely("RECEIVE_REMINDER", businessType, businessId, receiverUserId, pagePath,
                semantic, receiveFieldMapping, subject);
    }

    @Override
    public void notifyWithdrawStatus(Long withdrawId, String withdrawNo, Long receiverUserId, BigDecimal amount,
                                     String status, String remark, String pagePath) {
        Map<String, String> semantic = common(withdrawNo, remark);
        semantic.put("amount", (amount == null ? BigDecimal.ZERO : amount).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "元");
        semantic.put("status", safe(status));
        enqueueSafely("WITHDRAW_STATUS", "WITHDRAW_APPLY", withdrawId, receiverUserId, pagePath,
                semantic, withdrawFieldMapping, status);
    }

    private Map<String, String> common(String businessNo, String remark) {
        Map<String, String> semantic = new LinkedHashMap<String, String>();
        semantic.put("businessNo", safe(businessNo));
        semantic.put("remark", StrUtil.blankToDefault(remark, "请进入小程序查看详情"));
        semantic.put("time", DateUtil.formatDateTime(new Date()));
        return semantic;
    }

    private void enqueueSafely(String templateCode, String businessType, Long businessId, Long receiverUserId,
                               String pagePath, Map<String, String> semantic, String fieldMapping, String stateKey) {
        try {
            if (businessId == null || receiverUserId == null) {
                LOGGER.warn("订阅消息缺少业务ID或接收人，template={}, businessType={}, businessId={}, receiver={}",
                        templateCode, businessType, businessId, receiverUserId);
                return;
            }
            JkSubscriptionTaskCreateRequest request = new JkSubscriptionTaskCreateRequest();
            request.setTemplateCode(templateCode);
            request.setBusinessType(businessType);
            request.setBusinessId(businessId);
            request.setReceiverUserId(receiverUserId);
            request.setRecipientOpenId(resolveMiniProgramOpenId(receiverUserId));
            request.setPagePath(normalizePagePath(pagePath));
            request.setPayloadJson(toWechatPayload(semantic, fieldMapping));
            request.setRequestNo(requestNo(templateCode, businessType, businessId, stateKey));
            request.setMaxRetryCount(3);
            taskService.enqueue(request);
        } catch (Exception error) {
            LOGGER.error("九州康业务订阅消息入队失败，template={}, businessType={}, businessId={}, receiver={}",
                    templateCode, businessType, businessId, receiverUserId, error);
        }
    }

    private String resolveMiniProgramOpenId(Long userId) {
        if (userId == null || userId <= 0 || userId > Integer.MAX_VALUE) return null;
        try {
            UserToken token = userTokenService.getTokenByUserId(userId.intValue(), MINI_PROGRAM_TOKEN_TYPE);
            return token == null ? null : StrUtil.trim(token.getToken());
        } catch (Exception error) {
            LOGGER.warn("解析用户小程序openId失败，userId={}，任务将进入等待接收人授权", userId, error);
            return null;
        }
    }

    private String toWechatPayload(Map<String, String> semantic, String mappingText) {
        JSONObject payload = new JSONObject();
        if (StrUtil.isBlank(mappingText)) return payload.toString();
        String[] mappings = mappingText.split(",");
        for (String mapping : mappings) {
            String[] pair = mapping == null ? new String[0] : mapping.trim().split("=", 2);
            if (pair.length != 2) continue;
            String semanticKey = pair[0].trim();
            String wechatField = pair[1].trim();
            if (StrUtil.isBlank(semanticKey) || StrUtil.isBlank(wechatField) || !semantic.containsKey(semanticKey)) continue;
            JSONObject value = new JSONObject();
            value.put("value", limit(wechatField, semantic.get(semanticKey)));
            payload.put(wechatField, value);
        }
        if (payload.isEmpty()) throw new IllegalArgumentException("订阅消息模板字段映射无有效字段");
        return JSONUtil.toJsonStr(payload);
    }

    private String limit(String field, String value) {
        String result = safe(value).replace('\r', ' ').replace('\n', ' ').trim();
        String lower = field == null ? "" : field.toLowerCase();
        int max = 20;
        if (lower.startsWith("phrase")) max = 5;
        else if (lower.startsWith("character_string")) max = 32;
        else if (lower.startsWith("thing")) max = 20;
        else if (lower.startsWith("amount")) max = 20;
        else if (lower.startsWith("time") || lower.startsWith("date")) max = 20;
        else if (lower.startsWith("number")) max = 32;
        return result.length() <= max ? result : result.substring(0, max);
    }

    private String requestNo(String templateCode, String businessType, Long businessId, String stateKey) {
        String stateHash = Integer.toHexString(safe(stateKey).hashCode());
        String value = "SUB:" + safe(templateCode) + ":" + safe(businessType) + ":" + businessId + ":" + stateHash;
        return value.length() <= 96 ? value : value.substring(0, 96);
    }

    private String normalizePagePath(String pagePath) {
        if (StrUtil.isBlank(pagePath)) return null;
        String value = pagePath.trim();
        while (value.startsWith("/")) value = value.substring(1);
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    private String safe(String value) {
        return StrUtil.blankToDefault(value, "--");
    }
}
