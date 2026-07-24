package com.zbkj.service.service.jiuzhoukang.report;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkReportExportTask;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkReportExportCreateRequest;
import com.zbkj.common.response.jiuzhoukang.*;
import java.util.Date;
import java.util.List;

public interface JkAdvancedReportService {
    int aggregateDay(Date metricDate);
    List<JkReportMetricResponse> trend(String metricCode, Date startDate, Date endDate, String dimensionType, String dimensionCode);
    List<JkReportMetricResponse> regionPerformance(Date startDate, Date endDate);
    List<JkReportMetricResponse> teamPerformance(Date startDate, Date endDate, Long rootUserId);
    List<JkInventoryAgingResponse> inventoryAging(String regionCode, int warnDays, int seriousDays);
    JkFinanceReconcileResponse financeReconcile(Date startDate, Date endDate);
    List<JkStockBatchReconcileResponse> stockReconcile(String regionCode, boolean onlyMismatch);
    List<JkHealthAnonymousSummaryResponse> healthAnonymous(Date startDate, Date endDate, String regionCode, int minSampleSize);
    JkReportExportTask createExport(Long operatorId, JkReportExportCreateRequest request);
    PageInfo<JkReportExportTask> exportTasks(Long operatorId, String status, PageParamRequest page);
    int runPendingExports(int limit);
    JkReportExportTask getExport(Long id, Long operatorId, boolean platformAll);
}
