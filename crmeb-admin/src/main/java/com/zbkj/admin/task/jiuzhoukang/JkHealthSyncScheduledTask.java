package com.zbkj.admin.task.jiuzhoukang;

import com.zbkj.service.service.jiuzhoukang.health.JkHealthSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第五阶段健康设备同步失败自动重试。
 * <p>默认关闭，完成真实设备回调和失败场景验收后再通过配置开启；每次最多处理有限条数，避免压垮第三方或数据库。</p>
 */
@Component
@ConditionalOnProperty(prefix = "jk.health", name = "sync-auto-retry-enabled", havingValue = "true")
public class JkHealthSyncScheduledTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(JkHealthSyncScheduledTask.class);
    @Autowired private JkHealthSyncService syncService;

    @Scheduled(cron = "${jk.health.sync-retry-cron:0 */5 * * * ?}")
    public void retryFailedSync() {
        int count = syncService.retryDue(50);
        if (count > 0) LOGGER.info("九州康健康数据同步自动重试成功数量={}", count);
    }
}
