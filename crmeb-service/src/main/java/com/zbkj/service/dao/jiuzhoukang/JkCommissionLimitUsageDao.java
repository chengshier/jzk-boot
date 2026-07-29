package com.zbkj.service.dao.jiuzhoukang;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionLimitUsage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface JkCommissionLimitUsageDao extends BaseMapper<JkCommissionLimitUsage> {
    @Select("SELECT * FROM jk_commission_limit_usage WHERE usage_type=#{usageType} AND rule_id=#{ruleId} " +
            "AND user_id=#{userId} AND period_key=#{periodKey} LIMIT 1 FOR UPDATE")
    JkCommissionLimitUsage selectForUpdate(@Param("usageType") String usageType,
                                           @Param("ruleId") Long ruleId,
                                           @Param("userId") Long userId,
                                           @Param("periodKey") String periodKey);
}
