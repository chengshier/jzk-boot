package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import com.zbkj.service.dao.jiuzhoukang.JkHealthDataDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** 将三诺一批 CGM 数据和其预警判定作为同一事务提交。 */
@Service
public class SinocareCgmIngestionService {
    @Autowired private JkHealthDataDao healthData;
    @Autowired private JkHealthServiceImpl healthService;

    @Transactional(rollbackFor = Exception.class)
    public void ingest(Long userId, JSONObject body) {
        JSONArray points = body.getJSONArray("data");
        if (points == null || points.isEmpty()) return;
        for (Object raw : points) {
            JSONObject point = (JSONObject) raw;
            String key = "SINOCARE:" + body.getString("deviceSn") + ":" + body.getInteger("detectionDate") + ":" + point.getInteger("sn");
            if (healthData.selectCount(new LambdaQueryWrapper<JkHealthData>().eq(JkHealthData::getExternalNo, key)) > 0) continue;
            Date now = new Date();
            Long time = point.getLong("time");
            Date measuredAt = time == null || time <= 0 ? now : new Date(time);
            JkHealthData data = new JkHealthData().setExternalNo(key).setUserId(userId).setDataType("GLUCOSE")
                    .setNumericValue(point.getBigDecimal("value")).setUnit("mmol/L").setMeasuredAt(measuredAt)
                    .setSourceType("SINOCARE").setRiskLevel("NORMAL").setStatus("VALID").setIsDeleted(false)
                    .setCreateUserId(userId).setUpdateUserId(userId).setCreateTime(now).setUpdateTime(now).setVersion(0);
            healthData.insert(data);
            healthService.evaluateAlerts(data);
        }
    }
}
