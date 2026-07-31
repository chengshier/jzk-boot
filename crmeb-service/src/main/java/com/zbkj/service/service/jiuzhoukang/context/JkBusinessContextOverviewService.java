package com.zbkj.service.service.jiuzhoukang.context;

import java.util.Map;

public interface JkBusinessContextOverviewService {
    Map<String, Object> overview(String businessType, Long businessId);
}
