package com.zbkj.admin.task.jiuzhoukang;

import com.zbkj.service.service.jiuzhoukang.health.JkHealthProviderService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 主动拉取模式定时任务。默认关闭，真实厂商沙箱联调通过后再启用。 */
@Component
@ConditionalOnProperty(prefix="jk.health",name="provider-pull-enabled",havingValue="true")
public class JkHealthProviderPullScheduledTask {
    private static final Logger LOGGER=LoggerFactory.getLogger(JkHealthProviderPullScheduledTask.class);
    @Autowired private JkHealthProviderService service;
    @Scheduled(cron="${jk.health.provider-pull-cron:0 */5 * * * ?}")
    public void pull(){int count=service.pullDue(20);if(count>0)LOGGER.info("九州康健康厂商主动拉取完成，厂商数量={}",count);}
}
