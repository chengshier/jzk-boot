package com.zbkj.service.service.impl.jiuzhoukang.health;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import com.zbkj.common.model.jiuzhoukang.JkHealthReport;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkHealthReportGenerateRequest;
import com.zbkj.service.dao.jiuzhoukang.JkHealthDataDao;
import com.zbkj.service.dao.jiuzhoukang.JkHealthReportDao;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 健康周报/月报只聚合真实健康记录，不生成诊断、处方或虚构设备数据。 */
@Service
public class JkHealthReportServiceImpl implements JkHealthReportService {
    @Autowired private JkHealthDataDao dataDao;
    @Autowired private JkHealthReportDao reportDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthReport generate(Long userId, JkHealthReportGenerateRequest request) {
        if (!"WEEKLY".equals(request.getReportType()) && !"MONTHLY".equals(request.getReportType())) {
            throw new CrmebException("报告类型只允许 WEEKLY 或 MONTHLY");
        }
        Date start = dayStart(request.getPeriodStart());
        Date endExclusive = nextDay(request.getPeriodEnd());
        if (!endExclusive.after(start)) throw new CrmebException("报告结束日期必须不早于开始日期");
        long days = (endExclusive.getTime() - start.getTime()) / 86400000L;
        if ("WEEKLY".equals(request.getReportType()) && days > 7) throw new CrmebException("周报周期不能超过7天");
        if ("MONTHLY".equals(request.getReportType()) && days > 31) throw new CrmebException("月报周期不能超过31天");
        JkHealthReport oldByRequest = reportDao.selectOne(new LambdaQueryWrapper<JkHealthReport>()
                .eq(JkHealthReport::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (oldByRequest != null) {
            if (!userId.equals(oldByRequest.getUserId())) throw new CrmebException("requestNo 已被其他用户使用");
            return oldByRequest;
        }
        JkHealthReport existing = reportDao.selectOne(new LambdaQueryWrapper<JkHealthReport>()
                .eq(JkHealthReport::getUserId, userId).eq(JkHealthReport::getReportType, request.getReportType())
                .eq(JkHealthReport::getPeriodStart, start).eq(JkHealthReport::getPeriodEnd, request.getPeriodEnd())
                .eq(JkHealthReport::getIsDeleted, false).last("limit 1"));
        if (existing != null) return existing;

        List<JkHealthData> rows = dataDao.selectList(new LambdaQueryWrapper<JkHealthData>()
                .eq(JkHealthData::getUserId, userId).eq(JkHealthData::getIsDeleted, false)
                .ge(JkHealthData::getMeasuredAt, start).lt(JkHealthData::getMeasuredAt, endExclusive)
                .orderByAsc(JkHealthData::getMeasuredAt));
        int glucose = 0, high = 0, low = 0, normal = 0, diet = 0, exercise = 0, medicine = 0;
        BigDecimal sum = BigDecimal.ZERO, min = null, max = null;
        Map<String, Integer> sources = new LinkedHashMap<String, Integer>();
        for (JkHealthData row : rows) {
            String source = row.getSourceType() == null ? "UNKNOWN" : row.getSourceType();
            sources.put(source, sources.containsKey(source) ? sources.get(source) + 1 : 1);
            if ("GLUCOSE".equals(row.getDataType())) {
                glucose++;
                if (row.getNumericValue() != null) {
                    sum = sum.add(row.getNumericValue());
                    min = min == null || row.getNumericValue().compareTo(min) < 0 ? row.getNumericValue() : min;
                    max = max == null || row.getNumericValue().compareTo(max) > 0 ? row.getNumericValue() : max;
                }
                if ("HIGH".equals(row.getRiskLevel())) high++;
                else if ("LOW".equals(row.getRiskLevel())) low++;
                else if ("NORMAL".equals(row.getRiskLevel())) normal++;
            } else if ("DIET".equals(row.getDataType())) diet++;
            else if ("EXERCISE".equals(row.getDataType())) exercise++;
            else if ("MEDICINE".equals(row.getDataType())) medicine++;
        }
        BigDecimal average = glucose > 0 ? sum.divide(new BigDecimal(glucose), 2, RoundingMode.HALF_UP) : null;
        Date now = new Date();
        JkHealthReport report = new JkHealthReport().setReportNo("HR" + IdWorker.getIdStr()).setUserId(userId)
                .setReportType(request.getReportType()).setPeriodStart(start).setPeriodEnd(dayStart(request.getPeriodEnd()))
                .setRecordCount(rows.size()).setGlucoseCount(glucose).setAverageGlucose(average)
                .setMinimumGlucose(min).setMaximumGlucose(max).setHighCount(high).setLowCount(low).setNormalCount(normal)
                .setDietCount(diet).setExerciseCount(exercise).setMedicineCount(medicine)
                .setSourceSummaryJson(JSONUtil.toJsonStr(sources)).setSummaryText(summary(rows.size(), glucose, high, low, diet, exercise, medicine))
                .setStatus("GENERATED").setGeneratedAt(now).setRequestNo(request.getRequestNo()).setIsDeleted(false)
                .setCreateTime(now).setUpdateTime(now);
        reportDao.insert(report);
        return report;
    }

    @Override
    public PageInfo<JkHealthReport> list(Long userId, String reportType, PageParamRequest pageParam) {
        Page<JkHealthReport> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkHealthReport> query = new LambdaQueryWrapper<JkHealthReport>()
                .eq(JkHealthReport::getIsDeleted, false).orderByDesc(JkHealthReport::getPeriodEnd).orderByDesc(JkHealthReport::getId);
        if (userId != null) query.eq(JkHealthReport::getUserId, userId);
        if (reportType != null && !reportType.trim().isEmpty()) query.eq(JkHealthReport::getReportType, reportType);
        return CommonPage.copyPageInfo(page, reportDao.selectList(query));
    }

    @Override
    public JkHealthReport detail(Long viewerUserId, Long id, boolean admin) {
        JkHealthReport report = reportDao.selectById(id);
        if (report == null || Boolean.TRUE.equals(report.getIsDeleted())) throw new CrmebException("健康报告不存在");
        if (!admin && !viewerUserId.equals(report.getUserId())) throw new CrmebException("无权查看该健康报告");
        return report;
    }

    private String summary(int total,int glucose,int high,int low,int diet,int exercise,int medicine){
        return "本周期共记录"+total+"条健康数据，其中血糖"+glucose+"条（高风险标记"+high+"条、低风险标记"+low+"条），饮食"+diet+"条、运动"+exercise+"条、用药"+medicine+"条。报告仅用于记录汇总，不构成医疗诊断。";
    }
    private Date dayStart(Date value){Calendar c=Calendar.getInstance();c.setTime(value);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}
    private Date nextDay(Date value){Calendar c=Calendar.getInstance();c.setTime(dayStart(value));c.add(Calendar.DAY_OF_MONTH,1);return c.getTime();}
}
