package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkHealthAlertRecord;
import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import com.zbkj.service.dao.jiuzhoukang.JkHealthAlertRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkHealthDataDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于已存在健康数据生成统计报告，不依赖设备厂商，不作医疗诊断。
 */
@Service
public class JkHealthReportService {
    @Autowired private JkHealthDataDao dataDao;
    @Autowired private JkHealthAlertRecordDao alertDao;

    public Map<String, Object> report(Long userId, String period) {
        int days = "MONTH".equalsIgnoreCase(period) ? 30 : 7;
        Calendar calendar = Calendar.getInstance();
        Date end = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -(days - 1));
        Date start = startOfDay(calendar.getTime());
        List<JkHealthData> rows = dataDao.selectList(new LambdaQueryWrapper<JkHealthData>()
                .eq(JkHealthData::getUserId, userId).eq(JkHealthData::getDataType, "GLUCOSE")
                .eq(JkHealthData::getStatus, "VALID").eq(JkHealthData::getIsDeleted, false)
                .between(JkHealthData::getMeasuredAt, start, end).orderByAsc(JkHealthData::getMeasuredAt));
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        result.put("period", days == 30 ? "MONTH" : "WEEK");
        result.put("startDate", dateFormat.format(start));
        result.put("endDate", dateFormat.format(end));
        result.put("recordCount", rows.size());
        if (rows.isEmpty()) {
            result.put("riskLevel", "DATA_INSUFFICIENT");
            result.put("riskLevelText", "数据不足");
            result.put("periodStats", new ArrayList<Object>());
            result.put("sourceStats", new ArrayList<Object>());
            result.put("alerts", new ArrayList<Object>());
            result.put("summaryText", "当前周期没有有效血糖记录，无法生成趋势结论。报告不构成医疗诊断。");
            return result;
        }
        BigDecimal total = BigDecimal.ZERO, max = null, min = null;
        int validNumeric = 0, high = 0, medium = 0;
        Map<String, Stat> periodStats = new LinkedHashMap<String, Stat>();
        Map<String, Integer> sourceStats = new LinkedHashMap<String, Integer>();
        for (JkHealthData row : rows) {
            if (row.getNumericValue() != null) {
                total = total.add(row.getNumericValue()); validNumeric++;
                max = max == null ? row.getNumericValue() : max.max(row.getNumericValue());
                min = min == null ? row.getNumericValue() : min.min(row.getNumericValue());
                String code = row.getPeriodCode() == null ? "UNMARKED" : row.getPeriodCode();
                periodStats.computeIfAbsent(code, key -> new Stat()).add(row.getNumericValue());
            }
            if ("HIGH".equals(row.getRiskLevel())) high++;
            else if ("MEDIUM".equals(row.getRiskLevel())) medium++;
            String source = row.getSourceType() == null ? "UNKNOWN" : row.getSourceType();
            sourceStats.put(source, sourceStats.getOrDefault(source, 0) + 1);
        }
        result.put("averageValue", validNumeric == 0 ? null : total.divide(BigDecimal.valueOf(validNumeric), 1, RoundingMode.HALF_UP));
        result.put("maxValue", max);
        result.put("minValue", min);
        String risk = high > 0 ? "HIGH" : medium > 0 ? "MEDIUM" : "NORMAL";
        result.put("riskLevel", risk);
        result.put("riskLevelText", "HIGH".equals(risk) ? "存在高风险记录" : "MEDIUM".equals(risk) ? "存在需关注记录" : "本周期无系统风险标记");
        List<Map<String, Object>> periodRows = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Stat> entry : periodStats.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("periodCode", entry.getKey()); item.put("periodText", periodLabel(entry.getKey()));
            item.put("recordCount", entry.getValue().count);
            item.put("averageValue", entry.getValue().average());
            periodRows.add(item);
        }
        result.put("periodStats", periodRows);
        List<Map<String, Object>> sourceRows = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : sourceStats.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("sourceType", entry.getKey()); item.put("sourceText", sourceLabel(entry.getKey())); item.put("count", entry.getValue());
            sourceRows.add(item);
        }
        result.put("sourceStats", sourceRows);
        List<Map<String, Object>> alerts = new ArrayList<Map<String, Object>>();
        for (JkHealthAlertRecord alert : alertDao.selectList(new LambdaQueryWrapper<JkHealthAlertRecord>()
                .eq(JkHealthAlertRecord::getUserId, userId).eq(JkHealthAlertRecord::getIsDeleted, false)
                .between(JkHealthAlertRecord::getCreateTime, start, end).orderByDesc(JkHealthAlertRecord::getId).last("limit 20"))) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", alert.getId()); item.put("riskLevel", alert.getAlertLevel());
            item.put("riskLevelText", "HIGH".equals(alert.getAlertLevel()) ? "高风险提醒" : "需关注提醒");
            item.put("message", "记录值 " + alert.getMeasuredValue() + "，状态 " + alert.getStatus());
            item.put("createTime", alert.getCreateTime());
            alerts.add(item);
        }
        result.put("alerts", alerts);
        result.put("summaryText", "本周期共记录 " + rows.size() + " 次血糖数据，其中系统标记高风险 " + high
                + " 次、需关注 " + medium + " 次。数据来源和时段统计见下方，本报告不构成医疗诊断。");
        return result;
    }

    private Date startOfDay(Date value) { Calendar c = Calendar.getInstance(); c.setTime(value); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0); return c.getTime(); }
    private String periodLabel(String code) { if ("FASTING".equals(code)) return "空腹"; if ("BEFORE_MEAL".equals(code)) return "餐前"; if ("AFTER_MEAL".equals(code)) return "餐后"; if ("BEDTIME".equals(code)) return "睡前"; return "未标记时段"; }
    private String sourceLabel(String source) { if ("MANUAL".equals(source)) return "本人手工录入"; if ("ADMIN".equals(source)) return "管理员录入"; if ("DEVICE".equals(source) || "PROVIDER".equals(source)) return "设备同步"; return source; }

    private static final class Stat {
        private int count; private BigDecimal total = BigDecimal.ZERO;
        private void add(BigDecimal value) { count++; total = total.add(value); }
        private BigDecimal average() { return count == 0 ? null : total.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP); }
    }
}
