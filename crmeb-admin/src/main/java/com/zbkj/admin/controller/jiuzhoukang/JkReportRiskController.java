package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.*;
import com.zbkj.common.model.jiuzhoukang.JkRiskEvent;
import com.zbkj.common.model.jiuzhoukang.JkRiskRule;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRiskHandleRequest;
import com.zbkj.common.request.jiuzhoukang.JkRiskRuleSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkReportExportCreateRequest;
import com.zbkj.common.model.jiuzhoukang.JkReportExportTask;
import com.zbkj.common.response.jiuzhoukang.*;
import org.springframework.format.annotation.DateTimeFormat;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.report.JkReportService;
import com.zbkj.service.service.jiuzhoukang.report.JkAdvancedReportService;
import com.zbkj.service.service.jiuzhoukang.risk.JkRiskService;
import com.zbkj.service.service.jiuzhoukang.risk.JkRiskRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 第六阶段报表与风险入口。当前先提供可追溯概览和人工处置，不自动改业务账。 */
@RestController
@RequestMapping("api/admin/jk/phase-six")
public class JkReportRiskController {
    @Autowired private JkReportService reportService; @Autowired private JkAdvancedReportService advancedReport; @Autowired private JkRiskService riskService; @Autowired private JkRiskRuleService riskRuleService; @Autowired private JkAdminActorService actor;
    @GetMapping("/overview") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_OVERVIEW+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<JkPhaseSixOverviewResponse> overview(){return CommonResult.success(reportService.overview());}
    @PostMapping("/report/daily/run") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_DAILY_RUN+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<Integer> runDaily(@RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date metricDate){requirePlatform();return CommonResult.success(advancedReport.aggregateDay(metricDate));}

    @GetMapping("/report/trend") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_TREND+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<List<JkReportMetricResponse>> trend(@RequestParam String metricCode,@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date startDate,@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date endDate,@RequestParam(required=false)String dimensionType,@RequestParam(required=false)String dimensionCode){requirePlatform();return CommonResult.success(advancedReport.trend(metricCode,startDate,endDate,dimensionType,dimensionCode));}

    @GetMapping("/report/region") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_REGION+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<List<JkReportMetricResponse>> region(@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date startDate,@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date endDate){requirePlatform();return CommonResult.success(advancedReport.regionPerformance(startDate,endDate));}

    @GetMapping("/report/team") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_TEAM+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<List<JkReportMetricResponse>> team(@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date startDate,@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date endDate,@RequestParam(required=false)Long rootUserId){requirePlatform();return CommonResult.success(advancedReport.teamPerformance(startDate,endDate,rootUserId));}

    @GetMapping("/report/inventory-aging") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_INVENTORY+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<List<JkInventoryAgingResponse>> inventory(@RequestParam(required=false)String regionCode,@RequestParam(defaultValue="60")int warnDays,@RequestParam(defaultValue="90")int seriousDays){requirePlatform();return CommonResult.success(advancedReport.inventoryAging(regionCode,warnDays,seriousDays));}

    @GetMapping("/report/inventory-reconcile") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_INVENTORY_RECONCILE+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<List<JkStockBatchReconcileResponse>> inventoryReconcile(@RequestParam(required=false)String regionCode,@RequestParam(defaultValue="true")boolean onlyMismatch){requirePlatform();return CommonResult.success(advancedReport.stockReconcile(regionCode,onlyMismatch));}

    @GetMapping("/report/finance") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_FINANCE+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<JkFinanceReconcileResponse> finance(@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date startDate,@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date endDate){requirePlatform();return CommonResult.success(advancedReport.financeReconcile(startDate,endDate));}

    @GetMapping("/report/health-anonymous") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_HEALTH+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<List<JkHealthAnonymousSummaryResponse>> health(@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date startDate,@RequestParam(required=false)@DateTimeFormat(pattern="yyyy-MM-dd")Date endDate,@RequestParam(required=false)String regionCode,@RequestParam(defaultValue="10")int minSampleSize){requirePlatform();return CommonResult.success(advancedReport.healthAnonymous(startDate,endDate,regionCode,minSampleSize));}

    @PostMapping("/report/export/create") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_EXPORT+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<JkReportExportTask> createExport(@RequestBody @Validated JkReportExportCreateRequest r){requirePlatform();return CommonResult.success(advancedReport.createExport(operator(),r));}
    @GetMapping("/report/export/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_EXPORT+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<CommonPage<JkReportExportTask>> exports(@RequestParam(required=false)String status,PageParamRequest page){requirePlatform();return CommonResult.success(CommonPage.restPage(advancedReport.exportTasks(operator(),status,page)));}
    @PostMapping("/report/export/run") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_EXPORT+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public CommonResult<Integer> runExports(){requirePlatform();return CommonResult.success(advancedReport.runPendingExports(10));}
    @GetMapping("/report/export/{id}/download") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_REPORT_EXPORT+"')") @JkBizPermission(value=JkBizPermissionCodes.REPORT_VIEW)
    public void download(@PathVariable Long id,HttpServletResponse response)throws IOException{requirePlatform();JkReportExportTask t=advancedReport.getExport(id,operator(),true);if(!"SUCCESS".equals(t.getStatus())||t.getFilePath()==null)throw new IllegalStateException("导出文件尚未生成");File f=new File(t.getFilePath());if(!f.isFile())throw new IllegalStateException("导出文件不存在或已过期");response.setContentType("text/csv;charset=UTF-8");response.setHeader("Content-Disposition","attachment; filename=\""+t.getFileName()+"\"");Files.copy(f.toPath(),response.getOutputStream());}

    @GetMapping("/risk/rule/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_RISK_RULE_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.RISK_MANAGE)
    public CommonResult<CommonPage<JkRiskRule>> riskRules(@RequestParam(required=false)String keyword,@RequestParam(required=false)String scannerType,@RequestParam(required=false)Boolean enabled,PageParamRequest page){requirePlatform();return CommonResult.success(CommonPage.restPage(riskRuleService.list(keyword,scannerType,enabled,page)));}
    @PostMapping("/risk/rule/save") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_RISK_RULE_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.RISK_MANAGE)
    public CommonResult<JkRiskRule> saveRiskRule(@RequestBody @Validated JkRiskRuleSaveRequest r){requirePlatform();return CommonResult.success(riskRuleService.save(operator(),r));}
    @PostMapping("/risk/rule/{id}/enabled") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_RISK_RULE_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.RISK_MANAGE)
    public CommonResult<JkRiskRule> enableRiskRule(@PathVariable Long id,@RequestParam boolean enabled){requirePlatform();return CommonResult.success(riskRuleService.setEnabled(operator(),id,enabled));}
    @PostMapping("/risk/rule/{id}/run") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_RISK_RULE_RUN+"')") @JkBizPermission(value=JkBizPermissionCodes.RISK_MANAGE)
    public CommonResult<Integer> runRiskRule(@PathVariable Long id){requirePlatform();return CommonResult.success(riskRuleService.runOne(operator(),id));}
    @PostMapping("/risk/rule/run-all") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_RISK_RULE_RUN+"')") @JkBizPermission(value=JkBizPermissionCodes.RISK_MANAGE)
    public CommonResult<Integer> runRiskRules(){requirePlatform();return CommonResult.success(riskRuleService.runEnabled(operator(),100));}

    @GetMapping("/risk/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_RISK_EVENT_LIST+"')") @JkBizPermission(value=JkBizPermissionCodes.RISK_MANAGE)
    public CommonResult<CommonPage<JkRiskEvent>> riskList(@RequestParam(required=false)String riskType,@RequestParam(required=false)String riskLevel,@RequestParam(required=false)String status,PageParamRequest page){return CommonResult.success(CommonPage.restPage(riskService.list(riskType,riskLevel,status,page)));}
    @PostMapping("/risk/handle") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_RISK_EVENT_HANDLE+"')") @JkBizPermission(value=JkBizPermissionCodes.RISK_MANAGE)
    public CommonResult<JkRiskEvent> handle(@RequestBody @Validated JkRiskHandleRequest r){return CommonResult.success(riskService.handle(operator(),r));}
    private void requirePlatform(){if(!actor.isPlatformSuperAdmin(actor.getCurrentAdmin()))throw new IllegalStateException("完整经营报表仅平台管理员可访问；区县代和代理使用个人经营中心");}
    private Long operator(){Long linked=actor.getLinkedFrontUserId(actor.getCurrentAdmin());if(linked!=null)return linked;if(actor.isPlatformSuperAdmin(actor.getCurrentAdmin()))return -Long.valueOf(actor.getCurrentAdmin().getId());throw new IllegalStateException("后台管理员未绑定业务用户");}
}
