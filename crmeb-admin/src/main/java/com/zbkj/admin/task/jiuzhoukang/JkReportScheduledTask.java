package com.zbkj.admin.task.jiuzhoukang;

import com.zbkj.service.service.jiuzhoukang.report.JkAdvancedReportService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.*;

/** 日汇总和异步导出任务。默认关闭，口径和历史数据验收后再开启。 */
@Component
@ConditionalOnProperty(prefix="jk.report",name="scheduled-enabled",havingValue="true")
public class JkReportScheduledTask {
    private static final Logger LOGGER=LoggerFactory.getLogger(JkReportScheduledTask.class);
    @Autowired private JkAdvancedReportService service;
    @Scheduled(cron="${jk.report.daily-cron:0 0 2 * * ?}") public void daily(){Calendar c=Calendar.getInstance();c.add(Calendar.DAY_OF_MONTH,-1);int n=service.aggregateDay(c.getTime());LOGGER.info("九州康经营日报汇总完成，指标数={}",n);}
    @Scheduled(cron="${jk.report.export-cron:0 */2 * * * ?}") public void exports(){int n=service.runPendingExports(10);if(n>0)LOGGER.info("九州康报表导出完成数量={}",n);}
}
