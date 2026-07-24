package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.service.service.jiuzhoukang.event.JkBusinessEventService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

/** 主事务提交后执行事件，并把失败持久化为可重试事件。 */
@Component
public class JkAfterCommitExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JkAfterCommitExecutor.class);
    @Autowired private JkBusinessEventService eventService;

    /** 兼容非可靠的轻量回调。关键业务应使用带业务元数据的重载。 */
    public void execute(String eventName, Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) { runSafely(eventName, action); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { runSafely(eventName, action); }
        });
    }

    public void execute(String eventType, Long businessId, String businessNo, String payloadJson, Runnable action) {
        String eventKey = eventType + ":" + businessId;
        JkBusinessEvent event = eventService.prepare(eventKey, eventType, businessId, businessNo, payloadJson);
        if (event == null || "SUCCESS".equals(event.getEventStatus())) return;
        Runnable reliable = () -> eventService.executePrepared(event.getId(), action, null);
        if (!TransactionSynchronizationManager.isActualTransactionActive()) { reliable.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { reliable.run(); }
        });
    }

    private void runSafely(String eventName, Runnable action) {
        try { action.run(); }
        catch (Exception e) { LOGGER.error("九州康事务后事件执行失败，event={}", eventName, e); }
    }
}
