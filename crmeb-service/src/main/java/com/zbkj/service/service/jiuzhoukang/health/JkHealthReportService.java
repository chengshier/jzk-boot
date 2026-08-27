package com.zbkj.service.service.jiuzhoukang.health;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkHealthReport;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkHealthReportGenerateRequest;

public interface JkHealthReportService {
    JkHealthReport generate(Long userId, JkHealthReportGenerateRequest request);
    PageInfo<JkHealthReport> list(Long userId, String reportType, PageParamRequest page);
    JkHealthReport detail(Long viewerUserId, Long id, boolean admin);
}
