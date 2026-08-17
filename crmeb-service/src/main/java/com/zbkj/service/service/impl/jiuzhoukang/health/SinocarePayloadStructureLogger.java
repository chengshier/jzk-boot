package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 测试联调时记录三诺解密载荷的字段结构，不记录任何字段值。
 * 生产环境默认关闭；开启后仍只会输出字段名、类型和数组长度。
 */
@Component
public class SinocarePayloadStructureLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(SinocarePayloadStructureLogger.class);

    @Value("${jk.health.sinocare.debug-structure-enabled:false}")
    private boolean enabled;

    public void log(String eventType, JSONObject payload) {
        if (enabled) {
            LOGGER.info("Sinocare callback structure: eventType={}, structure={}", eventType, describe(payload));
        }
    }

    static String describe(JSONObject payload) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (String key : payload.keySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append(key).append(':').append(typeOf(payload.get(key)));
        }
        return builder.append('}').toString();
    }

    private static String typeOf(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof JSONObject) {
            return "object";
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            return "array(size=" + array.size() + (array.isEmpty() ? ")" : ",item=" + typeOf(array.get(0)) + ")");
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        return "string";
    }
}
