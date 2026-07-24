package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 健康数据 CSV 导出输出器。
 * <p>本类只负责格式化，调用前必须由 JkHealthService 完成本人/授权范围/allowExport 校验和导出日志。</p>
 */
@Component
public class JkHealthCsvExportSupport {
    public void write(HttpServletResponse response, List<JkHealthData> rows, String filename) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + URLEncoder.encode(filename, "UTF-8"));
            OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
            writer.write('\ufeff');
            writer.write("记录ID,数据类型,测量时间,数值,单位,时段,记录详情,来源,风险等级\r\n");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (JkHealthData row : rows) {
                writer.write(csv(row.getId()) + ',' + csv(row.getDataTypeText()) + ','
                        + csv(row.getMeasuredAt() == null ? "" : format.format(row.getMeasuredAt())) + ','
                        + csv(row.getNumericValue()) + ',' + csv(row.getUnit()) + ',' + csv(row.getPeriodCode()) + ','
                        + csv(row.getDetail()) + ',' + csv(row.getSourceType()) + ',' + csv(row.getRiskLevelText()) + "\r\n");
            }
            writer.flush();
        } catch (Exception e) {
            throw new IllegalStateException("健康数据导出失败", e);
        }
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return '"' + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + '"';
    }
}
