package com.zbkj.service.service.jiuzhoukang.health.provider;

import com.zbkj.common.model.jiuzhoukang.JkHealthProvider;
import com.zbkj.common.request.jiuzhoukang.JkHealthDeviceCallbackRequest;
import java.util.List;
import java.util.Map;

/**
 * 第三方健康设备适配器。
 * <p>标准 REST/JSON 厂商可以通过后台配置同时完成主动拉取和回调字段转换；
 * 只有 SDK、MQTT、私有二进制协议或厂商专用加密算法才需要新增适配器实现。</p>
 */
public interface HealthProviderAdapter {
    String adapterType();
    /** 主动调用厂商 API 拉取增量数据。 */
    List<JkHealthDeviceCallbackRequest> pull(JkHealthProvider provider, String credentialJson, String configJson, int limit);
    /**
     * 把厂商原始回调转换为九州康统一数据。实现必须先完成厂商级签名和时间戳校验，
     * 返回的数据随后统一进入 JkHealthSyncService 的幂等、加密、预警和重试链路。
     */
    List<JkHealthDeviceCallbackRequest> parseCallback(JkHealthProvider provider, String credentialJson, String configJson,
                                                       String rawBody, Map<String,String> headers);
}
