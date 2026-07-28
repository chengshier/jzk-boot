package com.zbkj.service.service.impl.jiuzhoukang.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkOfflineSale;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkReportExportTask;
import com.zbkj.common.model.jiuzhoukang.JkStockCheck;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkReportExportTaskCreateRequest;
import com.zbkj.service.dao.jiuzhoukang.JkOfflineSaleDao;
import com.zbkj.service.dao.jiuzhoukang.JkOperationProfitRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkReportExportTaskDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockCheckDao;
import com.zbkj.service.service.impl.jiuzhoukang.storage.JkMinioObjectStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * V3.1 报表导出任务。文件只保存到 MinIO，数据库仅保存对象键和元数据。
 */
@Service
public class JkMinioReportExportService {
    @Autowired private JkReportExportTaskDao taskDao;
    @Autowired private JkPerformanceRecordDao performanceDao;
    @Autowired private JkOperationProfitRecordDao profitDao;
    @Autowired private JkOfflineSaleDao offlineSaleDao;
    @Autowired private JkStockCheckDao stockCheckDao;
    @Autowired private JkMinioObjectStorageService storageService;

    @Transactional(rollbackFor = Exception.class)
    public JkReportExportTask create(Long operatorId, JkReportExportTaskCreateRequest request) {
        requireSupported(request.getReportType());
        JkReportExportTask old = taskDao.selectOne(new LambdaQueryWrapper<JkReportExportTask>()
                .eq(JkReportExportTask::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) return enrich(old);
        Date now = new Date();
        Calendar expire = Calendar.getInstance(); expire.setTime(now); expire.add(Calendar.DAY_OF_MONTH, 7);
        JkReportExportTask task = new JkReportExportTask()
                .setTaskNo("EX" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setReportType(request.getReportType()).setRequestNo(request.getRequestNo())
                .setRequestJson(request.getRequestJson()).setStatus("PENDING").setProgress(0)
                .setStorageProvider("MINIO").setDownloadCount(0).setRequestUserId(operatorId).setCreatedBy(operatorId)
                .setExpireTime(expire.getTime()).setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        try { taskDao.insert(task); }
        catch (DuplicateKeyException ignored) {
            return enrich(taskDao.selectOne(new LambdaQueryWrapper<JkReportExportTask>()
                    .eq(JkReportExportTask::getRequestNo, request.getRequestNo()).last("limit 1")));
        }
        return task;
    }

    public PageInfo<JkReportExportTask> list(String reportType, String status, Long createdBy, PageParamRequest pageParam) {
        Page<JkReportExportTask> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkReportExportTask> query = new LambdaQueryWrapper<JkReportExportTask>()
                .eq(JkReportExportTask::getIsDeleted, false).orderByDesc(JkReportExportTask::getId);
        if (notBlank(reportType)) query.eq(JkReportExportTask::getReportType, reportType);
        if (notBlank(status)) query.eq(JkReportExportTask::getStatus, status);
        if (createdBy != null) query.and(q -> q.eq(JkReportExportTask::getCreatedBy, createdBy)
                .or().eq(JkReportExportTask::getRequestUserId, createdBy));
        List<JkReportExportTask> rows = taskDao.selectList(query);
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public int runPending(int limit) {
        if (!storageService.isEnabled()) throw new CrmebException("MinIO 尚未启用，不能执行报表导出");
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<JkReportExportTask> tasks = taskDao.selectList(new LambdaQueryWrapper<JkReportExportTask>()
                .eq(JkReportExportTask::getStatus, "PENDING").eq(JkReportExportTask::getIsDeleted, false)
                .orderByAsc(JkReportExportTask::getId).last("limit " + safeLimit));
        int completed = 0;
        for (JkReportExportTask task : tasks) {
            try { runOne(task.getId()); completed++; }
            catch (Exception ignored) { }
        }
        return completed;
    }

    @Transactional(rollbackFor = Exception.class)
    public JkReportExportTask runOne(Long id) {
        JkReportExportTask task = require(id);
        if ("COMPLETED".equals(task.getStatus())) return enrich(task);
        if (!("PENDING".equals(task.getStatus()) || "FAILED".equals(task.getStatus()))) {
            throw new CrmebException("当前导出任务不能执行");
        }
        Date now = new Date();
        task.setStatus("RUNNING").setProgress(10).setErrorMessage(null).setUpdateTime(now);
        taskDao.updateById(task);
        try {
            byte[] bytes = buildCsv(task.getReportType());
            String fileName = task.getReportType().toLowerCase() + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(now) + ".csv";
            String objectKey = "report-export/" + task.getReportType().toLowerCase() + "/" + task.getTaskNo() + ".csv";
            storageService.put(objectKey, bytes, "text/csv;charset=UTF-8");
            task.setStatus("COMPLETED").setProgress(100).setFileName(fileName).setObjectKey(objectKey)
                    .setFilePath(null).setFileSize((long) bytes.length).setContentType("text/csv;charset=UTF-8")
                    .setCompletedTime(new Date()).setUpdateTime(new Date());
            taskDao.updateById(task);
            return enrich(task);
        } catch (Exception e) {
            task.setStatus("FAILED").setProgress(0).setErrorMessage(safe(e.getMessage())).setUpdateTime(new Date());
            taskDao.updateById(task);
            if (e instanceof CrmebException) throw (CrmebException) e;
            throw new CrmebException("报表导出失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public JkReportExportTask download(Long id, Long viewerUserId, boolean admin) {
        JkReportExportTask task = require(id);
        if (!admin && !viewerUserId.equals(task.getCreatedBy()) && !viewerUserId.equals(task.getRequestUserId())) {
            throw new CrmebException("无权下载该报表");
        }
        if (!"COMPLETED".equals(task.getStatus()) || !notBlank(task.getObjectKey())) throw new CrmebException("报表尚未生成");
        if (task.getExpireTime() != null && !task.getExpireTime().after(new Date())) throw new CrmebException("报表已过期，请重新生成");
        task.setDownloadCount(nvl(task.getDownloadCount()) + 1).setUpdateTime(new Date());
        taskDao.updateById(task);
        return enrich(task);
    }

    private byte[] buildCsv(String reportType) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        if ("PERFORMANCE".equals(reportType)) {
            csv.append("业绩编号,来源类型,来源单号,归属用户,角色,业绩类型,数量,基础金额,有效业绩,已冲减,状态,发生时间\n");
            for (JkPerformanceRecord row : performanceDao.selectList(new LambdaQueryWrapper<JkPerformanceRecord>()
                    .eq(JkPerformanceRecord::getIsDeleted, false).orderByDesc(JkPerformanceRecord::getId).last("limit 100000"))) {
                line(csv, row.getPerformanceNo(), row.getSourceType(), row.getSourceNo(), row.getOwnerUserId(), row.getOwnerRoleCode(),
                        row.getPerformanceType(), row.getQuantity(), row.getBaseAmount(), row.getPerformanceAmount(), row.getReversedAmount(),
                        row.getStatus(), row.getOccurredAt());
            }
        } else if ("OPERATION_PROFIT".equals(reportType)) {
            csv.append("收益编号,来源类型,来源单号,归属用户,角色,收益性质,收入,成本,经营毛利,已冲减,状态,创建时间\n");
            for (JkOperationProfitRecord row : profitDao.selectList(new LambdaQueryWrapper<JkOperationProfitRecord>()
                    .eq(JkOperationProfitRecord::getIsDeleted, false).orderByDesc(JkOperationProfitRecord::getId).last("limit 100000"))) {
                line(csv, row.getProfitNo(), row.getSourceType(), row.getSourceNo(), row.getUserId(), row.getRoleCode(), row.getIncomeNature(),
                        row.getRevenueAmount(), row.getCostAmount(), row.getProfitAmount(), row.getReversedAmount(), row.getStatus(), row.getCreateTime());
            }
        } else if ("OFFLINE_SALE".equals(reportType)) {
            csv.append("销售单号,销售人,角色,区域,客户类型,销售时间,销售额,成本,经营毛利,审核状态,业务状态\n");
            for (JkOfflineSale row : offlineSaleDao.selectList(new LambdaQueryWrapper<JkOfflineSale>()
                    .eq(JkOfflineSale::getIsDeleted, false).orderByDesc(JkOfflineSale::getId).last("limit 100000"))) {
                line(csv, row.getSaleNo(), row.getSellerUserId(), row.getSellerRoleCode(), row.getRegionCode(), row.getCustomerType(),
                        row.getSaleTime(), row.getTotalAmount(), row.getTotalCostAmount(), row.getTotalProfitAmount(), row.getAuditStatus(), row.getStatus());
            }
        } else if ("STOCK_CHECK".equals(reportType)) {
            csv.append("盘点单号,库存账户,归属用户,账面数量,实盘数量,盘盈,盘亏,冻结状态,业务状态,完成时间\n");
            for (JkStockCheck row : stockCheckDao.selectList(new LambdaQueryWrapper<JkStockCheck>()
                    .eq(JkStockCheck::getIsDeleted, false).orderByDesc(JkStockCheck::getId).last("limit 100000"))) {
                line(csv, row.getCheckNo(), row.getStockAccountId(), row.getOwnerUserId(), row.getBookTotalQty(), row.getActualTotalQty(),
                        row.getProfitQty(), row.getLossQty(), row.getFreezeStatus(), row.getStatus(), row.getCompletedTime());
            }
        } else {
            throw new CrmebException("不支持的报表类型");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void line(StringBuilder csv, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) csv.append(',');
            String value = values[i] == null ? "" : String.valueOf(values[i]);
            csv.append('"').append(value.replace("\"", "\"\"").replace('\n', ' ').replace('\r', ' ')).append('"');
        }
        csv.append('\n');
    }

    private JkReportExportTask enrich(JkReportExportTask task) {
        if (task != null && "COMPLETED".equals(task.getStatus()) && notBlank(task.getObjectKey())
                && (task.getExpireTime() == null || task.getExpireTime().after(new Date())) && storageService.isEnabled()) {
            try { task.setDownloadUrl(storageService.presignedDownloadUrl(task.getObjectKey(), 10)); }
            catch (Exception ignored) { task.setDownloadUrl(null); }
        }
        return task;
    }
    private JkReportExportTask require(Long id) { JkReportExportTask task = taskDao.selectById(id); if (task == null || Boolean.TRUE.equals(task.getIsDeleted())) throw new CrmebException("导出任务不存在"); return task; }
    private void requireSupported(String type) { if (!Arrays.asList("PERFORMANCE", "OPERATION_PROFIT", "OFFLINE_SALE", "STOCK_CHECK").contains(type)) throw new CrmebException("不支持的报表类型"); }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
    private String safe(String value) { return value == null ? "未知错误" : value.replace('\n', ' ').replace('\r', ' ').substring(0, Math.min(value.length(), 900)); }
}
