package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.service.dao.jiuzhoukang.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SinocareCallbackProcessor {
    @Autowired
    private JkSinocareCallbackLogDao logs;
    @Autowired
    private JkSinocareAuthorizationDao authorizations;
    @Autowired
    private JkHealthSensitiveCodec secure;
    @Autowired
    private SinocareRsaCodec rsa;
    @Autowired
    private SinocareCgmIngestionService cgmIngestionService;
    @Autowired
    private SinocarePayloadStructureLogger payloadStructureLogger;
    @Autowired
    private JkSinocareDeviceSessionDao sessions;
    @Autowired
    private JkSinocareReportDao reports;

    @Async
    public void process(Long id) {
        JkSinocareCallbackLog log = logs.selectById(id);
        if (log == null) return;
        try {
            log.setProcessStatus("PROCESSING").setUpdateTime(new Date());
            logs.updateById(log);
            JSONObject body = JSON.parseObject(rsa.verifyAndDecrypt(secure.decode(log.getPayloadCipher()), log.getSignature()));
            payloadStructureLogger.log(log.getEventType(), body);
            String uniqueId = body.getString("uniqueId");
            String providerEventId = body.getString("id");
            if (providerEventId == null)
                providerEventId = body.getString("deviceSn") + ":" + body.getString("detectionDate");
            log.setUniqueId(uniqueId);
            JkSinocareAuthorization auth = authorizations.selectOne(new LambdaQueryWrapper<JkSinocareAuthorization>().eq(JkSinocareAuthorization::getUniqueId, uniqueId).last("limit 1"));
            if (auth == null) {
                log.setProcessStatus("UNMATCHED").setErrorMessage("未找到本平台签发的 uniqueId");
            } else if (!"1001".equals(log.getEventType()) && !"AUTHORIZED".equals(auth.getStatus())) {
                log.setProcessStatus("UNMATCHED").setErrorMessage("三诺授权未生效，忽略回调数据");
            } else {
                applyAuthorization(log, body, auth);
                applyDevice(log, body);
                applyCgm(log, body, auth);
                applyReport(log, body, providerEventId);
                log.setProcessStatus("SUCCESS").setErrorMessage(null);
            }
        } catch (Exception e) {
            log.setProcessStatus("FAILED").setErrorMessage(e.getMessage() == null ? "三诺回调处理失败" : e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
        } finally {
            log.setUpdateTime(new Date());
            logs.updateById(log);
        }
    }

    private void applyAuthorization(JkSinocareCallbackLog log, JSONObject body, JkSinocareAuthorization auth) {
        if (!"1001".equals(log.getEventType())) return;
        Integer status = body.getInteger("status");
        Date now = new Date();
        if (Integer.valueOf(1).equals(status))
            auth.setStatus("AUTHORIZED").setAuthorizedAt(safeDate(body, "bindTime") == null ? now : safeDate(body, "bindTime")).setSourceEventId(log.getEventId());
        else if (Integer.valueOf(0).equals(status))
            auth.setStatus("REVOKED").setRevokedAt(safeDate(body, "unbindTime") == null ? now : safeDate(body, "unbindTime")).setSourceEventId(log.getEventId());
        else throw new IllegalArgumentException("三诺授权状态非法");
        auth.setUpdateTime(now);
        authorizations.updateById(auth);
    }

    private void applyDevice(JkSinocareCallbackLog log, JSONObject body) {
        if (!"1002".equals(log.getEventType())) return;
        String sn = body.getString("deviceSn");
        if (sn == null) throw new IllegalArgumentException("设备回调缺少 deviceSn");
        JkSinocareDeviceSession row = sessions.selectOne(new LambdaQueryWrapper<JkSinocareDeviceSession>().eq(JkSinocareDeviceSession::getUniqueId, log.getUniqueId()).eq(JkSinocareDeviceSession::getDeviceSn, sn).last("limit 1"));
        Date now = new Date();
        if (row == null)
            row = new JkSinocareDeviceSession().setUniqueId(log.getUniqueId()).setDeviceSn(sn).setStatus(1).setCreateTime(now);
        Integer status = body.getInteger("status");
        if (status != null) row.setStatus(status);
        String productName = body.getString("productName");
        if (productName != null) row.setProductName(productName);
        Date startAt = safeDate(body, "detectionStartTime");
        if (startAt != null) row.setDetectionStartTime(startAt);
        Date endAt = safeDate(body, "detectionEndTime");
        if (endAt != null) row.setDetectionEndTime(endAt);
        row.setUpdateTime(now);
        if (row.getId() == null) sessions.insert(row);
        else sessions.updateById(row);
    }

    private void applyCgm(JkSinocareCallbackLog log, JSONObject body, JkSinocareAuthorization auth) {
        if (!"1003".equals(log.getEventType())) return;
        com.alibaba.fastjson.JSONArray points = body.getJSONArray("data");
        if (points == null || points.isEmpty()) return;
        cgmIngestionService.ingest(auth.getUserId(), body);
        markSessionHasData(body, log.getUniqueId());
    }

    private void markSessionHasData(JSONObject body, String uniqueId) {
        String sn = body.getString("deviceSn");
        if (sn == null) return;
        JkSinocareDeviceSession row = sessions.selectOne(new LambdaQueryWrapper<JkSinocareDeviceSession>().eq(JkSinocareDeviceSession::getUniqueId, uniqueId).eq(JkSinocareDeviceSession::getDeviceSn, sn).last("limit 1"));
        Date now = new Date();
        if (row == null)
            row = new JkSinocareDeviceSession().setUniqueId(uniqueId).setDeviceSn(sn).setStatus(1).setCreateTime(now);
        Date last = null;
        for (Object raw : body.getJSONArray("data")) {
            Date value = safeDate((JSONObject) raw, "time");
            if (value != null && (last == null || value.after(last))) last = value;
        }
        Date receivedAt = last == null ? now : last;
        row.setLastDataAt(receivedAt).setUpdateTime(now);
        if (row.getDetectionEndTime() == null || receivedAt.after(row.getDetectionEndTime())) {
            row.setStatus(1).setDetectionEndTime(null);
        }
        if (row.getId() == null) sessions.insert(row);
        else sessions.updateById(row);
    }

    private Date safeDate(JSONObject body, String field) {
        Long value = body.getLong(field);
        return value == null || value <= 0 ? null : new Date(value);
    }

    private void applyReport(JkSinocareCallbackLog log, JSONObject body, String providerEventId) {
        if (!"1004".equals(log.getEventType()) && !"1005".equals(log.getEventType())) return;
        if (reports.selectCount(new LambdaQueryWrapper<JkSinocareReport>().eq(JkSinocareReport::getReportType, "1004".equals(log.getEventType()) ? "DIGITAL" : "PDF").eq(JkSinocareReport::getEventId, providerEventId)) > 0)
            return;
        Date now = new Date();
        reports.insert(new JkSinocareReport().setEventId(providerEventId).setUniqueId(log.getUniqueId()).setDeviceSn(body.getString("deviceSn")).setReportType("1004".equals(log.getEventType()) ? "DIGITAL" : "PDF").setPayloadCipher(secure.encode(body.toJSONString())).setCreateTime(now).setUpdateTime(now));
    }
}
