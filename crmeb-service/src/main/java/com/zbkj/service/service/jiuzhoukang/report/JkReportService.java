package com.zbkj.service.service.jiuzhoukang.report;

import com.zbkj.common.response.jiuzhoukang.JkPhaseSixOverviewResponse;

/**
 * 第六阶段统一报表入口。
 * <p>所有指标必须来源于真实业务表和账本，不能使用前端缓存拼接结果。
 * 当前 V1 为直接聚合，数据量增大后应切换日快照/汇总表。</p>
 */
public interface JkReportService {
    /** 返回跨身份、库存、订货、调拨、佣金、提现、健康和风险的总览指标。 */
    JkPhaseSixOverviewResponse overview();
}
