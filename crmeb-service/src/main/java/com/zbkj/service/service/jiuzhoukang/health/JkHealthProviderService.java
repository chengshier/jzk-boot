package com.zbkj.service.service.jiuzhoukang.health;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkHealthProvider;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkHealthProviderSaveRequest;
import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import java.util.List;
import java.util.Map;

public interface JkHealthProviderService {
    PageInfo<JkHealthProvider> list(String keyword, String syncMode, Boolean enabled, PageParamRequest page);
    JkHealthProvider save(Long operatorId, JkHealthProviderSaveRequest request);
    JkHealthProvider detail(Long id);
    int pullOne(Long providerId, boolean resetCursor, int limit);
    int pullDue(int limit);
    String callbackSecret(String providerCode);
    JkHealthProvider findEnabled(String providerCode);
    /** 处理厂商原始 JSON 回调，字段映射和验签由所选适配器完成。 */
    List<JkHealthData> receiveCallback(String providerCode, String rawBody, Map<String,String> headers);
}
