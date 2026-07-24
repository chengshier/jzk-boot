package com.zbkj.admin.task.jiuzhoukang;

import com.zbkj.service.service.jiuzhoukang.risk.JkRiskRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第六阶段自动风险扫描。
 * <p>默认关闭。真实阈值和角色权限完成验收后再开启；扫描只告警，不自动改账。</p>
 */
@Component
@ConditionalOnProperty(prefix="jk.risk",name="auto-scan-enabled",havingValue="true")
public class JkRiskScanScheduledTask {
    @Autowired private JkRiskRuleService service;
    @Scheduled(cron="${jk.risk.auto-scan-cron:0 30 2 * * ?}")
    public void scan(){service.runEnabled(-1L,100);}
}
