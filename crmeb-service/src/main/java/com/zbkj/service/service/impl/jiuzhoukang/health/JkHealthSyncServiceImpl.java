package com.zbkj.service.service.impl.jiuzhoukang.health;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import com.zbkj.common.model.jiuzhoukang.JkHealthSyncLog;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkHealthDeviceCallbackRequest;
import com.zbkj.service.dao.jiuzhoukang.JkHealthSyncLogDao;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthService;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 设备数据同步日志与补偿实现。
 * <p>验签在 Controller 进入本服务之前完成；本服务只保存通过验签的加密请求快照。</p>
 * <p>同步日志先独立落库，再调用健康数据事务。健康数据事务失败时日志仍可更新为 FAILED，供定时或人工重试。</p>
 */
@Service
public class JkHealthSyncServiceImpl implements JkHealthSyncService {
    @Autowired private JkHealthSyncLogDao syncLogDao;
    @Autowired private JkHealthService healthService;
    @Autowired private JkHealthSensitiveCodec codec;

    @Override
    public JkHealthData receive(JkHealthDeviceCallbackRequest request) {
        JkHealthSyncLog log = find(request.getProviderCode(), request.getExternalNo());
        if (log == null) {
            Date now = new Date();
            log = new JkHealthSyncLog()
                    .setSyncNo("HSYNC" + IdWorker.getIdStr())
                    .setProviderCode(request.getProviderCode())
                    .setDeviceSn(request.getDeviceSn())
                    .setExternalNo(request.getExternalNo())
                    .setPayloadCipher(codec.encode(JSON.toJSONString(request)))
                    .setSyncStatus("PENDING").setRetryCount(0).setIsDeleted(false)
                    .setCreateTime(now).setUpdateTime(now);
            try {
                syncLogDao.insert(log);
            } catch (DuplicateKeyException duplicate) {
                // 两个节点同时收到同一回调时，唯一键负责最终兜底；重新读取已有同步日志继续幂等处理。
                log = find(request.getProviderCode(), request.getExternalNo());
                if (log == null) throw duplicate;
            }
        }
        if ("SUCCESS".equals(log.getSyncStatus())) {
            // 重复回调继续交给健康数据幂等逻辑返回原记录，不重复写入。
            return healthService.ingestDeviceData(request);
        }
        return execute(log, request);
    }

    @Override
    public JkHealthData retry(Long syncLogId, Long operatorId) {
        JkHealthSyncLog log = syncLogDao.selectById(syncLogId);
        if (log == null || Boolean.TRUE.equals(log.getIsDeleted())) throw new CrmebException("同步日志不存在");
        if ("SUCCESS".equals(log.getSyncStatus())) {
            JkHealthDeviceCallbackRequest request = decode(log);
            return healthService.ingestDeviceData(request);
        }
        return execute(log, decode(log));
    }

    @Override
    public int retryDue(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        // 服务处理到一半宕机时，PROCESSING 可能永久卡住；超过十分钟自动恢复为 FAILED。
        Calendar stale = Calendar.getInstance();
        stale.add(Calendar.MINUTE, -10);
        syncLogDao.update(null, new UpdateWrapper<JkHealthSyncLog>()
                .eq("sync_status", "PROCESSING").lt("update_time", stale.getTime()).eq("is_deleted", 0)
                .set("sync_status", "FAILED").set("next_retry_time", new Date())
                .set("error_message", "处理超时，已恢复为待重试").set("update_time", new Date()));
        List<JkHealthSyncLog> rows = syncLogDao.selectList(new LambdaQueryWrapper<JkHealthSyncLog>()
                .eq(JkHealthSyncLog::getSyncStatus, "FAILED")
                .le(JkHealthSyncLog::getNextRetryTime, new Date())
                .eq(JkHealthSyncLog::getIsDeleted, false)
                .orderByAsc(JkHealthSyncLog::getId).last("limit " + safeLimit));
        int success = 0;
        for (JkHealthSyncLog row : rows) {
            try { retry(row.getId(), 0L); success++; } catch (Exception ignored) { /* 失败状态已在 execute 中落库 */ }
        }
        return success;
    }

    @Override
    public PageInfo<JkHealthSyncLog> list(String providerCode, String syncStatus, PageParamRequest pageParam) {
        Page<JkHealthSyncLog> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkHealthSyncLog> q = new LambdaQueryWrapper<JkHealthSyncLog>()
                .eq(JkHealthSyncLog::getIsDeleted, false).orderByDesc(JkHealthSyncLog::getId);
        if (StrUtil.isNotBlank(providerCode)) q.eq(JkHealthSyncLog::getProviderCode, providerCode);
        if (StrUtil.isNotBlank(syncStatus)) q.eq(JkHealthSyncLog::getSyncStatus, syncStatus);
        List<JkHealthSyncLog> rows = syncLogDao.selectList(q);
        for (JkHealthSyncLog row : rows) {
            row.setPayloadCipher(null);
            row.setStatusText(statusText(row.getSyncStatus()));
        }
        return CommonPage.copyPageInfo(page, rows);
    }

    private JkHealthData execute(JkHealthSyncLog log, JkHealthDeviceCallbackRequest request) {
        Date now = new Date();
        // 用状态条件原子抢占，避免重复回调、人工重试和定时任务同时处理同一条记录。
        int claimed = syncLogDao.update(null, new UpdateWrapper<JkHealthSyncLog>()
                .eq("id", log.getId()).in("sync_status", java.util.Arrays.asList("PENDING", "FAILED", "DEAD"))
                .set("sync_status", "PROCESSING").set("last_retry_time", now).set("update_time", now));
        if (claimed != 1) {
            JkHealthSyncLog current = syncLogDao.selectById(log.getId());
            if (current != null && "SUCCESS".equals(current.getSyncStatus())) return healthService.ingestDeviceData(request);
            throw new CrmebException("该同步记录正在处理中，请勿重复操作");
        }
        log.setSyncStatus("PROCESSING").setLastRetryTime(now).setUpdateTime(now);
        try {
            JkHealthData data = healthService.ingestDeviceData(request);
            log.setSyncStatus("SUCCESS").setHealthDataId(data.getId()).setErrorMessage(null)
                    .setNextRetryTime(null).setUpdateTime(new Date());
            syncLogDao.updateById(log);
            return data;
        } catch (Exception ex) {
            int retry = log.getRetryCount() == null ? 1 : log.getRetryCount() + 1;
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, Math.min(60, 5 * retry));
            log.setSyncStatus(retry >= 10 ? "DEAD" : "FAILED").setRetryCount(retry)
                    .setNextRetryTime(retry >= 10 ? null : calendar.getTime())
                    .setErrorMessage(limit(ex.getMessage(), 500)).setUpdateTime(new Date());
            syncLogDao.updateById(log);
            if (ex instanceof CrmebException) throw (CrmebException) ex;
            throw new CrmebException("健康设备数据同步失败");
        }
    }

    private JkHealthDeviceCallbackRequest decode(JkHealthSyncLog log) {
        try { return JSON.parseObject(codec.decode(log.getPayloadCipher()), JkHealthDeviceCallbackRequest.class); }
        catch (Exception e) { throw new CrmebException("同步请求快照无法解密或解析"); }
    }

    private JkHealthSyncLog find(String providerCode, String externalNo) {
        return syncLogDao.selectOne(new LambdaQueryWrapper<JkHealthSyncLog>()
                .eq(JkHealthSyncLog::getProviderCode, providerCode)
                .eq(JkHealthSyncLog::getExternalNo, externalNo)
                .eq(JkHealthSyncLog::getIsDeleted, false).last("limit 1"));
    }

    private String limit(String value, int max) {
        if (value == null) return "未知同步错误";
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private String statusText(String value) {
        if ("PENDING".equals(value)) return "待处理";
        if ("PROCESSING".equals(value)) return "处理中";
        if ("SUCCESS".equals(value)) return "成功";
        if ("FAILED".equals(value)) return "待重试";
        if ("DEAD".equals(value)) return "已终止";
        return value;
    }
}
