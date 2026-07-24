package com.zbkj.admin.task.jiuzhoukang;

import com.zbkj.service.service.jiuzhoukang.commission.*;
import com.zbkj.service.service.jiuzhoukang.event.JkBusinessEventService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class JkClosureScheduledTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(JkClosureScheduledTask.class);
    @Autowired private CommissionAutoSettleService autoSettleService;
    @Autowired private AccountReconcileService reconcileService;
    @Autowired private JkBusinessEventService eventService;
    @Value("${jk.commission.auto-settle-enabled:false}") private boolean autoSettleEnabled;
    @Value("${jk.account.auto-reconcile-enabled:false}") private boolean autoReconcileEnabled;
    @Value("${jk.business-event.auto-retry-enabled:false}") private boolean autoRetryEnabled;

    @Scheduled(cron = "${jk.business-event.retry-cron:0 */10 * * * ?}")
    public void retryEvents() {
        if (!autoRetryEnabled) return;
        int count = eventService.retryDue(100);
        if (count > 0) LOGGER.info("九州康业务事件自动补偿完成，处理数量={}", count);
    }

    @Scheduled(cron = "${jk.commission.auto-settle-cron:0 15 2 * * ?}")
    public void autoSettle() {
        if (!autoSettleEnabled) return;
        String day = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int count = autoSettleService.settleDue(500, null, day);
        LOGGER.info("九州康到期佣金自动结算完成，记录数量={}", count);
    }

    @Scheduled(cron = "${jk.account.auto-reconcile-cron:0 30 3 * * ?}")
    public void autoReconcile() {
        if (!autoReconcileEnabled) return;
        String batch = "AUTO-" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        int count = reconcileService.reconcile(null, null, null, batch).size();
        LOGGER.info("九州康账户自动对账完成，账户数量={}", count);
    }
}
