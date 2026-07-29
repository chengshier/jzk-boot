package com.zbkj.service.service.impl.jiuzhoukang.wechat;

import com.zbkj.service.service.jiuzhoukang.wechat.JkSubscriptionTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 微信订阅消息任务自动处理器。
 *
 * <p>只有显式配置 {@code jk.wechat.subscribe-auto-process-enabled=true} 时才创建调度器并启用
 * Spring Scheduling，避免默认状态误启动其他周期任务。多实例同时运行时，由任务服务状态 CAS 抢占
 * 保证同一任务只由一个实例发送；异常退出后，超过十分钟的 PROCESSING 任务可被下一轮重新抢占。</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "jk.wechat.subscribe-auto-process-enabled", havingValue = "true")
public class JkSubscriptionTaskScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JkSubscriptionTaskScheduler.class);

    @Autowired private JkSubscriptionTaskService taskService;

    @Value("${jk.wechat.subscribe-process-batch-size:20}")
    private int batchSize;

    @Scheduled(cron = "${jk.wechat.subscribe-process-cron:0 */1 * * * ?}")
    public void process() {
        try {
            int sent = taskService.processDue(Math.max(1, Math.min(100, batchSize)));
            if (sent > 0) LOGGER.info("九州康订阅消息任务处理完成，发送成功数量={}", sent);
        } catch (Exception error) {
            LOGGER.error("九州康订阅消息任务自动处理失败", error);
        }
    }
}
