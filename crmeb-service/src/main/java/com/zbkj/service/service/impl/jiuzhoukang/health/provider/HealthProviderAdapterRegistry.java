package com.zbkj.service.service.impl.jiuzhoukang.health.provider;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.service.service.jiuzhoukang.health.provider.HealthProviderAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

/** 适配器注册中心。后台配置 adapterType 后由这里选择实现。 */
@Component
public class HealthProviderAdapterRegistry {
    private final Map<String, HealthProviderAdapter> adapters = new HashMap<String, HealthProviderAdapter>();
    @Autowired
    public HealthProviderAdapterRegistry(List<HealthProviderAdapter> rows) {
        if (rows != null) for (HealthProviderAdapter row : rows) adapters.put(row.adapterType().toUpperCase(), row);
    }
    public HealthProviderAdapter require(String type) {
        String key = type == null || type.trim().isEmpty() ? "GENERIC_REST" : type.trim().toUpperCase();
        HealthProviderAdapter adapter = adapters.get(key);
        if (adapter == null) throw new CrmebException("未找到健康厂商适配器: " + key);
        return adapter;
    }
}
