package com.zbkj.service.service.impl.jiuzhoukang.event;

import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessEventDao;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Date;

@Service
public class JkBusinessEventPersistenceService {
    @Autowired private JkBusinessEventDao dao;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long id, Long operatorId, boolean allowDead) {
        UpdateWrapper<JkBusinessEvent> update = new UpdateWrapper<JkBusinessEvent>()
                .eq("id", id)
                .in("event_status", allowDead
                        ? java.util.Arrays.asList("PENDING", "FAILED", "DEAD")
                        : java.util.Arrays.asList("PENDING", "FAILED"))
                .set("event_status", "PROCESSING")
                .set("last_operator_id", operatorId)
                .set("update_time", new Date());
        return dao.update(null, update) == 1;
    }

    /** 服务实例异常退出后，把长时间停留在 PROCESSING 的事件恢复为 FAILED。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int releaseStaleProcessing(int timeoutMinutes) {
        int safeMinutes = timeoutMinutes <= 0 ? 30 : Math.min(timeoutMinutes, 1440);
        Date now = new Date();
        Date threshold = new Date(now.getTime() - safeMinutes * 60_000L);
        return dao.update(null, new UpdateWrapper<JkBusinessEvent>()
                .eq("event_status", "PROCESSING")
                .le("update_time", threshold)
                .set("event_status", "FAILED")
                .set("next_retry_time", now)
                .set("error_message", "事件处理进程超时，已自动恢复为可重试状态")
                .set("update_time", now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(Long id, Long operatorId) {
        JkBusinessEvent event = dao.selectById(id);
        if (event == null) return;
        Date now = new Date();
        event.setEventStatus("SUCCESS").setProcessedTime(now).setNextRetryTime(null).setErrorMessage(null)
                .setLastOperatorId(operatorId).setUpdateTime(now);
        dao.updateById(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(Long id, Throwable error, Long operatorId) {
        JkBusinessEvent event = dao.selectById(id);
        if (event == null) return;
        int retry = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
        int max = event.getMaxRetryCount() == null ? 8 : event.getMaxRetryCount();
        long delayMinutes = Math.min(360L, (long) Math.pow(2, Math.min(retry, 8)) * 5L);
        Date now = new Date();
        event.setRetryCount(retry).setEventStatus(retry >= max ? "DEAD" : "FAILED")
                .setNextRetryTime(retry >= max ? null : new Date(now.getTime() + delayMinutes * 60_000L))
                .setErrorMessage(shortMessage(error)).setLastOperatorId(operatorId).setUpdateTime(now);
        dao.updateById(event);
    }

    private String shortMessage(Throwable error) {
        if (error == null) return "未知错误";
        String text = error.getClass().getSimpleName() + ": " + (error.getMessage() == null ? "" : error.getMessage());
        return text.length() > 1900 ? text.substring(0, 1900) : text;
    }
}
