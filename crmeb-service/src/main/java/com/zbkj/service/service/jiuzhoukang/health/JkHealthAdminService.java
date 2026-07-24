package com.zbkj.service.service.jiuzhoukang.health;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkHealthIntegrationStatusResponse;

/** 健康模块后台管理接口，不承担他人明细访问授权，明细访问仍统一走 JkHealthService。 */
public interface JkHealthAdminService {
    PageInfo<JkHealthDevice> listDevices(String keyword, String status, PageParamRequest page);
    JkHealthDevice saveDevice(Long adminUserId, JkHealthDeviceSaveRequest request);
    PageInfo<JkHealthDeviceBind> listDeviceBinds(Long userId, String deviceSn, String status, PageParamRequest page);
    PageInfo<JkHealthAuthorization> listAuthorizations(Long ownerUserId, Long granteeUserId, String status, PageParamRequest page);
    PageInfo<JkHealthAlertRule> listRules(String dataType, Boolean enabled, PageParamRequest page);
    JkHealthAlertRule saveRule(Long adminUserId, JkHealthAlertRuleSaveRequest request);
    PageInfo<JkHealthAlertRecord> listAlerts(Long userId, String status, PageParamRequest page);
    JkHealthAlertRecord processAlert(Long adminUserId, JkHealthAlertProcessRequest request);
    PageInfo<JkHealthAccessLog> listAccessLogs(Long ownerUserId, Long viewerUserId, String accessResult, PageParamRequest page);
    JkHealthIntegrationStatusResponse integrationStatus();
}
