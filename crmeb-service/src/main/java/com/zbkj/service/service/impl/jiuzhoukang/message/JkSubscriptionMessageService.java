package com.zbkj.service.service.impl.jiuzhoukang.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkSubscriptionMessageLog;
import com.zbkj.common.model.jiuzhoukang.JkSubscriptionMessageTask;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.JkSubscriptionMessageLogDao;
import com.zbkj.service.dao.jiuzhoukang.JkSubscriptionMessageTaskDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 微信订阅消息队列。默认关闭；关闭时保留 SKIPPED_DISABLED 任务证据，不伪装发送成功。
 */
@Service
public class JkSubscriptionMessageService {
    @Autowired private JkSubscriptionMessageTaskDao taskDao;
    @Autowired private JkSubscriptionMessageLogDao logDao;

    @Value("${jk.wechat.subscription.enabled:false}") private boolean enabled;
    @Value("${jk.wechat.subscription.app-id:}") private String appId;
    @Value("${jk.wechat.subscription.secret:}") private String secret;

    @Transactional(rollbackFor = Exception.class)
    public JkSubscriptionMessageTask enqueue(String eventType, String eventKey, Long receiverUserId,
                                               String openid, String templateCode, String templateId,
                                               String pagePath, String payloadJson) {
        if (blank(eventKey) || receiverUserId == null) throw new CrmebException("订阅消息事件和接收人不能为空");
        JkSubscriptionMessageTask old = taskDao.selectOne(new LambdaQueryWrapper<JkSubscriptionMessageTask>()
                .eq(JkSubscriptionMessageTask::getEventKey, eventKey)
                .eq(JkSubscriptionMessageTask::getReceiverUserId, receiverUserId).last("limit 1"));
        if (old != null) return old;
        Date now = new Date();
        JkSubscriptionMessageTask task = new JkSubscriptionMessageTask()
                .setTaskNo("SM" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setEventType(eventType).setEventKey(eventKey).setReceiverUserId(receiverUserId).setOpenid(openid)
                .setTemplateCode(templateCode).setTemplateId(templateId).setPagePath(pagePath).setPayloadJson(payloadJson)
                .setStatus(enabled ? "PENDING" : "SKIPPED_DISABLED").setRetryCount(0).setMaxRetryCount(5)
                .setNextRetryTime(enabled ? now : null).setEnabledSnapshot(enabled).setIsDeleted(false)
                .setCreateTime(now).setUpdateTime(now);
        try { taskDao.insert(task); }
        catch (DuplicateKeyException ignored) {
            return taskDao.selectOne(new LambdaQueryWrapper<JkSubscriptionMessageTask>()
                    .eq(JkSubscriptionMessageTask::getEventKey, eventKey)
                    .eq(JkSubscriptionMessageTask::getReceiverUserId, receiverUserId).last("limit 1"));
        }
        if (!enabled) log(task, 0, null, null, "SKIPPED_DISABLED", "订阅消息总开关关闭");
        return task;
    }

    public PageInfo<JkSubscriptionMessageTask> list(String eventType, String status, Long receiverUserId, PageParamRequest pageParam) {
        Page<JkSubscriptionMessageTask> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkSubscriptionMessageTask> query = new LambdaQueryWrapper<JkSubscriptionMessageTask>()
                .eq(JkSubscriptionMessageTask::getIsDeleted, false).orderByDesc(JkSubscriptionMessageTask::getId);
        if (!blank(eventType)) query.eq(JkSubscriptionMessageTask::getEventType, eventType);
        if (!blank(status)) query.eq(JkSubscriptionMessageTask::getStatus, status);
        if (receiverUserId != null) query.eq(JkSubscriptionMessageTask::getReceiverUserId, receiverUserId);
        List<JkSubscriptionMessageTask> rows = taskDao.selectList(query);
        for (JkSubscriptionMessageTask row : rows) row.setOpenid(mask(row.getOpenid()));
        return CommonPage.copyPageInfo(page, rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public int processDue(int limit) {
        if (!enabled) throw new CrmebException("订阅消息总开关尚未启用");
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<JkSubscriptionMessageTask> tasks = taskDao.selectList(new LambdaQueryWrapper<JkSubscriptionMessageTask>()
                .in(JkSubscriptionMessageTask::getStatus, java.util.Arrays.asList("PENDING", "RETRY"))
                .le(JkSubscriptionMessageTask::getNextRetryTime, new Date()).eq(JkSubscriptionMessageTask::getIsDeleted, false)
                .orderByAsc(JkSubscriptionMessageTask::getId).last("limit " + safeLimit));
        int success = 0;
        for (JkSubscriptionMessageTask task : tasks) {
            try { send(task); success++; }
            catch (Exception ignored) { }
        }
        return success;
    }

    @Transactional(rollbackFor = Exception.class)
    public JkSubscriptionMessageTask retry(Long id) {
        JkSubscriptionMessageTask task = require(id);
        if (!enabled) throw new CrmebException("订阅消息总开关尚未启用");
        if (!("FAILED".equals(task.getStatus()) || "SKIPPED_DISABLED".equals(task.getStatus()) || "RETRY".equals(task.getStatus()))) {
            throw new CrmebException("当前任务不能重试");
        }
        task.setStatus("PENDING").setNextRetryTime(new Date()).setLastError(null).setEnabledSnapshot(true).setUpdateTime(new Date());
        taskDao.updateById(task);
        return task;
    }

    private void send(JkSubscriptionMessageTask task) {
        if (blank(task.getOpenid()) || blank(task.getTemplateId())) fail(task, "接收人 openid 或模板ID缺失", null, null);
        if (blank(appId) || blank(secret)) fail(task, "微信小程序 AppId 或 Secret 未配置", null, null);
        int attempt = nvl(task.getRetryCount()) + 1;
        String tokenRaw = null, responseRaw = null;
        try {
            RestTemplate rest = new RestTemplate();
            tokenRaw = rest.getForObject("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appId}&secret={secret}", String.class, appId, secret);
            JSONObject tokenJson = JSON.parseObject(tokenRaw);
            String token = tokenJson.getString("access_token");
            if (blank(token)) throw new CrmebException("获取微信 access_token 失败：" + tokenJson.getString("errmsg"));
            JSONObject body = new JSONObject();
            body.put("touser", task.getOpenid()); body.put("template_id", task.getTemplateId());
            if (!blank(task.getPagePath())) body.put("page", task.getPagePath());
            body.put("data", blank(task.getPayloadJson()) ? new JSONObject() : JSON.parseObject(task.getPayloadJson()));
            body.put("miniprogram_state", "formal"); body.put("lang", "zh_CN");
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
            responseRaw = rest.postForObject("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token,
                    new HttpEntity<String>(body.toJSONString(), headers), String.class);
            JSONObject response = JSON.parseObject(responseRaw);
            if (response.getIntValue("errcode") != 0) throw new CrmebException(response.getString("errmsg"));
            task.setStatus("SUCCESS").setRetryCount(attempt).setNextRetryTime(null).setLastError(null).setUpdateTime(new Date());
            taskDao.updateById(task);
            log(task, attempt, task.getPayloadJson(), responseRaw, "SUCCESS", null);
        } catch (Exception e) {
            fail(task, safe(e.getMessage()), task.getPayloadJson(), responseRaw);
        }
    }

    private void fail(JkSubscriptionMessageTask task, String error, String request, String response) {
        int attempt = nvl(task.getRetryCount()) + 1;
        boolean dead = attempt >= nvl(task.getMaxRetryCount());
        Calendar next = Calendar.getInstance(); next.add(Calendar.MINUTE, Math.min(60, attempt * 5));
        task.setRetryCount(attempt).setStatus(dead ? "FAILED" : "RETRY")
                .setNextRetryTime(dead ? null : next.getTime()).setLastError(error).setUpdateTime(new Date());
        taskDao.updateById(task);
        log(task, attempt, request, response, task.getStatus(), error);
        throw new CrmebException("订阅消息发送失败：" + error);
    }

    private void log(JkSubscriptionMessageTask task, int attempt, String request, String response, String status, String error) {
        logDao.insert(new JkSubscriptionMessageLog().setTaskId(task.getId()).setAttemptNo(attempt)
                .setRequestJson(request).setResponseJson(response).setStatus(status).setErrorMessage(error).setCreateTime(new Date()));
    }
    private JkSubscriptionMessageTask require(Long id) { JkSubscriptionMessageTask task = taskDao.selectById(id); if (task == null || Boolean.TRUE.equals(task.getIsDeleted())) throw new CrmebException("订阅消息任务不存在"); return task; }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String mask(String value) { if (blank(value)) return null; return value.length() <= 8 ? "****" : value.substring(0, 4) + "****" + value.substring(value.length() - 4); }
    private String safe(String value) { if (value == null) return "未知错误"; String result = value.replace('\n', ' ').replace('\r', ' '); return result.substring(0, Math.min(result.length(), 900)); }
}
