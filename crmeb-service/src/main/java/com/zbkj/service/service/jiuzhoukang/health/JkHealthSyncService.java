package com.zbkj.service.service.jiuzhoukang.health;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import com.zbkj.common.model.jiuzhoukang.JkHealthSyncLog;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkHealthDeviceCallbackRequest;

/**
 * 已通过验签的第三方健康数据同步、失败留痕和重试入口。
 * <p>原始回调只以密文保存；后台查询必须清空 payloadCipher，避免管理员看到健康敏感载荷。</p>
 */
public interface JkHealthSyncService {
    /** 首次接收回调；providerCode + externalNo 用于第三方数据幂等。 */
    JkHealthData receive(JkHealthDeviceCallbackRequest request);
    /** 人工重试指定失败记录；已成功记录只返回幂等结果，不重复入库。 */
    JkHealthData retry(Long syncLogId, Long operatorId);
    /** 定时任务批量处理到期 FAILED 记录，每次限制数量以保护数据库。 */
    int retryDue(int limit);
    /** 后台同步日志；返回结果不包含加密载荷。 */
    PageInfo<JkHealthSyncLog> list(String providerCode, String syncStatus, PageParamRequest page);
}
