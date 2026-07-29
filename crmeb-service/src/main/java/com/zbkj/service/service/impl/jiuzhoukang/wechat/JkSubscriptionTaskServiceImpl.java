package com.zbkj.service.service.impl.jiuzhoukang.wechat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkSubscriptionTask;
import com.zbkj.common.model.user.UserToken;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkSubscriptionTaskCreateRequest;
import com.zbkj.service.dao.jiuzhoukang.JkSubscriptionTaskDao;
import com.zbkj.service.service.UserTokenService;
import com.zbkj.service.service.jiuzhoukang.wechat.JkSubscriptionTaskService;
import com.zbkj.service.service.jiuzhoukang.wechat.JkWechatAccessTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信订阅消息任务队列。
 *
 * <p>总开关默认关闭。排队不代表发送成功；缺少微信凭据、模板 ID 或可信 openId 时分别进入
 * WAIT_CONFIG / WAIT_RECIPIENT，不进行无意义的周期重试。只有微信接口明确返回 errcode=0 才标记 SENT。</p>
 */
@Service
public class JkSubscriptionTaskServiceImpl implements JkSubscriptionTaskService {
    private static final String PENDING = "PENDING";
    private static final String PROCESSING = "PROCESSING";
    private static final String RETRY_WAIT = "RETRY_WAIT";
    private static final String WAIT_CONFIG = "WAIT_CONFIG";
    private static final String WAIT_RECIPIENT = "WAIT_RECIPIENT";
    private static final String SENT = "SENT";
    private static final String FAILED = "FAILED";
    private static final int MINI_PROGRAM_TOKEN_TYPE = 2;

    @Autowired private JkSubscriptionTaskDao taskDao;
    @Autowired private JkWechatAccessTokenService tokenService;
    @Autowired private UserTokenService userTokenService;

    @Value("${jk.wechat.subscribe-enabled:false}") private boolean subscribeEnabled;
    @Value("${jk.wechat.subscribe.audit-template-id:}") private String auditTemplateId;
    @Value("${jk.wechat.subscribe.transfer-template-id:}") private String transferTemplateId;
    @Value("${jk.wechat.subscribe.receive-template-id:}") private String receiveTemplateId;
    @Value("${jk.wechat.subscribe.withdraw-template-id:}") private String withdrawTemplateId;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkSubscriptionTask enqueue(JkSubscriptionTaskCreateRequest request) {
        JkSubscriptionTask old = taskDao.selectOne(new LambdaQueryWrapper<JkSubscriptionTask>()
                .eq(JkSubscriptionTask::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) return enrich(old);
        Date now = new Date();
        String templateId = templateId(request.getTemplateCode());
        String trustedOpenId = trustedOpenId(request.getReceiverUserId());
        String initialStatus = resolveReadyStatus(templateId, trustedOpenId);
        String initialError = initialError(initialStatus);
        JkSubscriptionTask task = new JkSubscriptionTask().setTaskNo("SM" + IdWorker.getIdStr())
                .setTemplateCode(normalizeTemplateCode(request.getTemplateCode())).setTemplateId(templateId)
                .setBusinessType(request.getBusinessType()).setBusinessId(request.getBusinessId())
                .setReceiverUserId(request.getReceiverUserId()).setRecipientOpenId(trustedOpenId)
                .setPagePath(request.getPagePath()).setPayloadJson(normalizePayload(request.getPayloadJson()))
                .setStatus(initialStatus).setRetryCount(0)
                .setMaxRetryCount(request.getMaxRetryCount() == null ? 3 : Math.max(0, Math.min(10, request.getMaxRetryCount())))
                .setNextRetryTime(PENDING.equals(initialStatus) ? now : null).setErrorMessage(initialError)
                .setRequestNo(request.getRequestNo()).setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        try {
            taskDao.insert(task);
            return enrich(task);
        } catch (DuplicateKeyException duplicate) {
            JkSubscriptionTask duplicateValue = taskDao.selectOne(new LambdaQueryWrapper<JkSubscriptionTask>()
                    .eq(JkSubscriptionTask::getRequestNo, request.getRequestNo()).last("limit 1"));
            if (duplicateValue != null) return enrich(duplicateValue);
            throw new CrmebException("订阅消息任务正在创建，请勿重复提交");
        }
    }

    @Override
    public int processDue(int limit) {
        int size = Math.max(1, Math.min(100, limit));
        Date now = new Date();
        Date staleBefore = addMinutes(now, -10);
        List<JkSubscriptionTask> tasks = taskDao.selectList(new LambdaQueryWrapper<JkSubscriptionTask>()
                .and(q -> q.in(JkSubscriptionTask::getStatus, Arrays.asList(PENDING, RETRY_WAIT))
                        .and(due -> due.isNull(JkSubscriptionTask::getNextRetryTime)
                                .or().le(JkSubscriptionTask::getNextRetryTime, now))
                        .or(stale -> stale.eq(JkSubscriptionTask::getStatus, PROCESSING)
                                .le(JkSubscriptionTask::getUpdateTime, staleBefore)))
                .eq(JkSubscriptionTask::getIsDeleted, false)
                .orderByAsc(JkSubscriptionTask::getId)
                .last("limit " + size));
        int sent = 0;
        for (JkSubscriptionTask task : tasks) {
            if (!claim(task, now, staleBefore)) continue;
            JkSubscriptionTask claimed = require(task.getId());
            if (processOne(claimed)) sent++;
        }
        return sent;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkSubscriptionTask retry(Long taskId, String reason) {
        JkSubscriptionTask task = require(taskId);
        if (SENT.equals(task.getStatus())) throw new CrmebException("已发送任务不能重试");
        String templateId = templateId(task.getTemplateCode());
        String trustedOpenId = trustedOpenId(task.getReceiverUserId());
        String readyStatus = resolveReadyStatus(templateId, trustedOpenId);
        task.setTemplateId(templateId).setRecipientOpenId(trustedOpenId).setRetryCount(0)
                .setNextRetryTime(PENDING.equals(readyStatus) ? new Date() : null)
                .setErrorCode(null).setErrorMessage(PENDING.equals(readyStatus)
                        ? StrUtil.blankToDefault(reason, "管理员重新入队") : initialError(readyStatus))
                .setStatus(readyStatus).setUpdateTime(new Date());
        taskDao.updateById(task);
        return enrich(task);
    }

    @Override
    public PageInfo<JkSubscriptionTask> list(String status, String templateCode, Long receiverUserId, PageParamRequest pageParam) {
        Page<JkSubscriptionTask> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkSubscriptionTask> query = new LambdaQueryWrapper<JkSubscriptionTask>()
                .eq(JkSubscriptionTask::getIsDeleted, false).orderByDesc(JkSubscriptionTask::getId);
        if (StrUtil.isNotBlank(status)) query.eq(JkSubscriptionTask::getStatus, status.trim().toUpperCase());
        if (StrUtil.isNotBlank(templateCode)) query.eq(JkSubscriptionTask::getTemplateCode, normalizeTemplateCode(templateCode));
        if (receiverUserId != null) query.eq(JkSubscriptionTask::getReceiverUserId, receiverUserId);
        List<JkSubscriptionTask> rows = taskDao.selectList(query);
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("enabled", subscribeEnabled);
        result.put("wechat", tokenService.status());
        result.put("auditTemplateConfigured", StrUtil.isNotBlank(auditTemplateId));
        result.put("transferTemplateConfigured", StrUtil.isNotBlank(transferTemplateId));
        result.put("receiveTemplateConfigured", StrUtil.isNotBlank(receiveTemplateId));
        result.put("withdrawTemplateConfigured", StrUtil.isNotBlank(withdrawTemplateId));
        result.put("pending", count(PENDING) + count(RETRY_WAIT));
        result.put("processing", count(PROCESSING));
        result.put("waitConfig", count(WAIT_CONFIG));
        result.put("waitRecipient", count(WAIT_RECIPIENT));
        result.put("failed", count(FAILED));
        result.put("ready", subscribeEnabled && Boolean.TRUE.equals(tokenService.status().get("ready")));
        return result;
    }

    private boolean claim(JkSubscriptionTask task, Date now, Date staleBefore) {
        LambdaUpdateWrapper<JkSubscriptionTask> update = new LambdaUpdateWrapper<JkSubscriptionTask>()
                .eq(JkSubscriptionTask::getId, task.getId())
                .eq(JkSubscriptionTask::getIsDeleted, false)
                .set(JkSubscriptionTask::getStatus, PROCESSING)
                .set(JkSubscriptionTask::getUpdateTime, now);
        if (PROCESSING.equals(task.getStatus())) {
            update.eq(JkSubscriptionTask::getStatus, PROCESSING)
                    .le(JkSubscriptionTask::getUpdateTime, staleBefore);
        } else {
            update.eq(JkSubscriptionTask::getStatus, task.getStatus());
        }
        return taskDao.update(null, update) == 1;
    }

    private boolean processOne(JkSubscriptionTask task) {
        String templateId = templateId(task.getTemplateCode());
        if (!subscribeEnabled || StrUtil.isBlank(templateId)) {
            updateWait(task, WAIT_CONFIG, !subscribeEnabled ? "SUBSCRIBE_DISABLED" : "TEMPLATE_NOT_CONFIGURED",
                    !subscribeEnabled ? "订阅消息总开关未启用" : "模板ID未配置");
            return false;
        }
        String trustedOpenId = trustedOpenId(task.getReceiverUserId());
        if (StrUtil.isBlank(trustedOpenId)) {
            task.setRecipientOpenId(null);
            updateWait(task, WAIT_RECIPIENT, "RECIPIENT_OPENID_MISSING", "缺少来自可信微信登录上下文的openId");
            return false;
        }
        task.setRecipientOpenId(trustedOpenId);
        try {
            JSONObject body = new JSONObject();
            body.put("touser", trustedOpenId);
            body.put("template_id", templateId);
            if (StrUtil.isNotBlank(task.getPagePath())) body.put("page", task.getPagePath());
            body.put("miniprogram_state", "formal");
            body.put("lang", "zh_CN");
            body.put("data", JSONUtil.parseObj(normalizePayload(task.getPayloadJson())));
            HttpResponse response = HttpRequest.post("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + tokenService.token())
                    .header("Content-Type", "application/json").body(body.toString()).timeout(15000).execute();
            JSONObject result = JSONUtil.parseObj(response.body());
            int errcode = result.getInt("errcode", -1);
            if (errcode != 0) throw new WechatSendException(String.valueOf(errcode), safe(result.getStr("errmsg")));
            task.setTemplateId(templateId).setStatus(SENT).setSentAt(new Date()).setNextRetryTime(null)
                    .setWechatMessageId(result.getStr("msgid")).setErrorCode(null).setErrorMessage(null).setUpdateTime(new Date());
            taskDao.updateById(task);
            return true;
        } catch (WechatSendException error) {
            failOrRetry(task, error.code, error.getMessage());
            return false;
        } catch (Exception error) {
            failOrRetry(task, "SEND_EXCEPTION", safe(error.getMessage()));
            return false;
        }
    }

    private void failOrRetry(JkSubscriptionTask task, String code, String message) {
        int retry = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        int max = task.getMaxRetryCount() == null ? 3 : task.getMaxRetryCount();
        task.setRetryCount(retry).setErrorCode(code).setErrorMessage(message).setUpdateTime(new Date());
        if (retry > max) {
            task.setStatus(FAILED).setNextRetryTime(null);
        } else {
            task.setStatus(RETRY_WAIT).setNextRetryTime(addMinutes(new Date(), Math.min(60, (int) Math.pow(2, retry))));
        }
        taskDao.updateById(task);
    }

    private void updateWait(JkSubscriptionTask task, String status, String code, String message) {
        task.setStatus(status).setNextRetryTime(null).setErrorCode(code).setErrorMessage(message).setUpdateTime(new Date());
        taskDao.updateById(task);
    }

    private String trustedOpenId(Long userId) {
        if (userId == null || userId <= 0 || userId > Integer.MAX_VALUE) return null;
        try {
            UserToken token = userTokenService.getTokenByUserId(userId.intValue(), MINI_PROGRAM_TOKEN_TYPE);
            return token == null ? null : StrUtil.trim(token.getToken());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String initialError(String status) {
        if (WAIT_CONFIG.equals(status)) return !subscribeEnabled ? "订阅消息总开关未启用" : "模板ID未配置";
        if (WAIT_RECIPIENT.equals(status)) return "缺少来自可信微信登录上下文的openId";
        return null;
    }

    private String resolveReadyStatus(String templateId, String openId) {
        if (!subscribeEnabled || StrUtil.isBlank(templateId)) return WAIT_CONFIG;
        if (StrUtil.isBlank(openId)) return WAIT_RECIPIENT;
        return PENDING;
    }

    private String templateId(String code) {
        String normalized = normalizeTemplateCode(code);
        if ("AUDIT_RESULT".equals(normalized)) return auditTemplateId;
        if ("TRANSFER_STATUS".equals(normalized)) return transferTemplateId;
        if ("RECEIVE_REMINDER".equals(normalized)) return receiveTemplateId;
        if ("WITHDRAW_STATUS".equals(normalized)) return withdrawTemplateId;
        return null;
    }

    private String normalizeTemplateCode(String code) {
        String value = StrUtil.blankToDefault(code, "").trim().toUpperCase();
        if (!Arrays.asList("AUDIT_RESULT", "TRANSFER_STATUS", "RECEIVE_REMINDER", "WITHDRAW_STATUS").contains(value)) {
            throw new CrmebException("不支持的订阅消息模板编码");
        }
        return value;
    }

    private String normalizePayload(String payload) {
        if (StrUtil.isBlank(payload)) return "{}";
        try { return JSONUtil.parseObj(payload).toString(); }
        catch (Exception error) { throw new CrmebException("订阅消息payloadJson必须是JSON对象"); }
    }

    private JkSubscriptionTask require(Long id) {
        JkSubscriptionTask task = taskDao.selectById(id);
        if (task == null || Boolean.TRUE.equals(task.getIsDeleted())) throw new CrmebException("订阅消息任务不存在");
        return task;
    }

    private JkSubscriptionTask enrich(JkSubscriptionTask task) {
        task.setStatusText(statusText(task.getStatus()));
        return task;
    }

    private int count(String status) {
        Integer value = taskDao.selectCount(new LambdaQueryWrapper<JkSubscriptionTask>()
                .eq(JkSubscriptionTask::getStatus, status).eq(JkSubscriptionTask::getIsDeleted, false));
        return value == null ? 0 : value;
    }

    private String statusText(String status) {
        if (PENDING.equals(status)) return "待发送";
        if (PROCESSING.equals(status)) return "发送处理中";
        if (RETRY_WAIT.equals(status)) return "等待重试";
        if (WAIT_CONFIG.equals(status)) return "等待配置";
        if (WAIT_RECIPIENT.equals(status)) return "等待接收人授权";
        if (SENT.equals(status)) return "发送成功";
        if (FAILED.equals(status)) return "发送失败";
        return status;
    }

    private Date addMinutes(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    private String safe(String value) {
        if (value == null) return "未知错误";
        String result = value.replace('\r', ' ').replace('\n', ' ');
        return result.length() > 450 ? result.substring(0, 450) : result;
    }

    private static class WechatSendException extends RuntimeException {
        private final String code;
        private WechatSendException(String code, String message) { super(message); this.code = code; }
    }
}
