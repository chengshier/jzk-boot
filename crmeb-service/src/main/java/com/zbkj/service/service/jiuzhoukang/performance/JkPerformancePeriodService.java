package com.zbkj.service.service.jiuzhoukang.performance;

import com.zbkj.common.model.jiuzhoukang.JkPerformancePeriod;
import com.zbkj.common.request.jiuzhoukang.JkPerformancePeriodBuildRequest;
import com.zbkj.common.request.jiuzhoukang.JkPerformancePeriodCloseRequest;

import java.util.List;
import java.util.Map;

public interface JkPerformancePeriodService {
    List<JkPerformancePeriod> list(String status, String periodType);
    Map<String, Object> detail(Long id);
    JkPerformancePeriod build(JkPerformancePeriodBuildRequest request, Long operatorId);
    Map<String, Object> trial(Long id);
    JkPerformancePeriod close(Long id, JkPerformancePeriodCloseRequest request, Long operatorId);
}
