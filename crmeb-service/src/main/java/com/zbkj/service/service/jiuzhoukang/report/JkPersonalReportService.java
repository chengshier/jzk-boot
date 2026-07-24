package com.zbkj.service.service.jiuzhoukang.report;

import com.zbkj.common.response.jiuzhoukang.JkPersonalReportResponse;
import java.util.Date;

public interface JkPersonalReportService {
    JkPersonalReportResponse summary(Long userId, Date startDate, Date endDate);
}
