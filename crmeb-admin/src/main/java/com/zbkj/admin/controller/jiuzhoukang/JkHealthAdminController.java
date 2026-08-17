package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.*;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthAdminService;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthService;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthSyncService;
import com.zbkj.service.service.jiuzhoukang.support.JkHealthCsvExportSupport;
import com.zbkj.common.response.jiuzhoukang.JkHealthIntegrationStatusResponse;
import com.zbkj.common.response.jiuzhoukang.JkSinocareCallbackLogResponse;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 第五阶段健康管理后台。健康明细访问不在此 Controller 直接开放，防止管理员绕过用户授权。 */
@RestController
@RequestMapping("api/admin/jk/health")
public class JkHealthAdminController {
    @Autowired private JkHealthAdminService service;
    @Autowired private JkHealthService healthService;
    @Autowired private JkHealthSyncService syncService;
    @Autowired private com.zbkj.service.service.jiuzhoukang.health.JkHealthProviderService providerService;
    @Autowired private com.zbkj.service.service.jiuzhoukang.health.SinocareCallbackService sinocareCallbackService;
    @Autowired private JkHealthCsvExportSupport csvExport;
    @Autowired private JkAdminActorService actor;

    @GetMapping("/device/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_DEVICE_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<CommonPage<JkHealthDevice>> devices(@RequestParam(required=false)String keyword,@RequestParam(required=false)String status,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.listDevices(keyword,status,page)));}
    @PostMapping("/device/save") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_DEVICE_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<JkHealthDevice> saveDevice(@RequestBody @Validated JkHealthDeviceSaveRequest r){return CommonResult.success(service.saveDevice(operator(),r));}
    /** 查看设备绑定历史，不提供直接修改历史的接口。 */
    @GetMapping("/bind/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_BIND_LIST+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<CommonPage<JkHealthDeviceBind>> binds(@RequestParam(required=false)Long userId,@RequestParam(required=false)String deviceSn,@RequestParam(required=false)String status,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.listDeviceBinds(userId,deviceSn,status,page)));}

    /**
     * 后台查看健康明细也不能使用超级管理员绕过授权，必须先把后台账号映射到已获用户授权的前台健康顾问。
     */
    @GetMapping("/data/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_DATA_LIST+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_DATA_VIEW_AUTHORIZED)
    public CommonResult<CommonPage<JkHealthData>> data(@RequestParam Long ownerUserId,@RequestParam(required=false)String dataType,PageParamRequest page){return CommonResult.success(CommonPage.restPage(healthService.listData(healthViewer(),ownerUserId,dataType,page)));}

    @GetMapping("/data/export") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_DATA_EXPORT+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_DATA_EXPORT_AUTHORIZED)
    public void export(@RequestParam Long ownerUserId,@RequestParam(required=false)String dataType,HttpServletResponse response){csvExport.write(response,healthService.exportData(healthViewer(),ownerUserId,dataType),"授权健康数据.csv");}

    @GetMapping("/authorization/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_AUTH_LIST+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<CommonPage<JkHealthAuthorization>> authorizations(@RequestParam(required=false)Long ownerUserId,@RequestParam(required=false)Long granteeUserId,@RequestParam(required=false)String status,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.listAuthorizations(ownerUserId,granteeUserId,status,page)));}
    @GetMapping("/alert-rule/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_ALERT_RULE_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<CommonPage<JkHealthAlertRule>> rules(@RequestParam(required=false)String dataType,@RequestParam(required=false)Boolean enabled,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.listRules(dataType,enabled,page)));}
    @PostMapping("/alert-rule/save") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_ALERT_RULE_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<JkHealthAlertRule> saveRule(@RequestBody @Validated JkHealthAlertRuleSaveRequest r){return CommonResult.success(service.saveRule(operator(),r));}
    @GetMapping("/alert-record/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_ALERT_RECORD_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ALERT_VIEW)
    public CommonResult<CommonPage<JkHealthAlertRecord>> alerts(@RequestParam(required=false)Long userId,@RequestParam(required=false)String status,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.listAlerts(userId,status,page)));}
    @PostMapping("/alert-record/process") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_ALERT_RECORD_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<JkHealthAlertRecord> process(@RequestBody @Validated JkHealthAlertProcessRequest r){return CommonResult.success(service.processAlert(operator(),r));}
    @GetMapping("/access-log/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_ACCESS_LOG_LIST+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<CommonPage<JkHealthAccessLog>> logs(@RequestParam(required=false)Long ownerUserId,@RequestParam(required=false)Long viewerUserId,@RequestParam(required=false)String accessResult,PageParamRequest page){return CommonResult.success(CommonPage.restPage(service.listAccessLogs(ownerUserId,viewerUserId,accessResult,page)));}
    /** 查看第三方同步结果，Service 会清空加密回调载荷。 */
    @GetMapping("/sync/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_SYNC_LIST+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<CommonPage<JkHealthSyncLog>> syncLogs(@RequestParam(required=false)String providerCode,@RequestParam(required=false)String syncStatus,PageParamRequest page){return CommonResult.success(CommonPage.restPage(syncService.list(providerCode,syncStatus,page)));}

    /** 人工补偿失败同步；重复成功数据由 externalNo 幂等保护。 */
    @PostMapping("/sync/{id}/retry") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_SYNC_RETRY+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<JkHealthData> retrySync(@PathVariable Long id){return CommonResult.success(syncService.retry(id,operator()));}

    /** 三诺回调日志只返回已脱敏的字段，密文与签名不可查看。 */
    @GetMapping("/sinocare/callback/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_SYNC_LIST+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<CommonPage<JkSinocareCallbackLogResponse>> sinocareCallbacks(@RequestParam(required=false) String eventType,@RequestParam(required=false) String processStatus,@RequestParam(required=false) String uniqueId,PageParamRequest page){return CommonResult.success(CommonPage.restPage(sinocareCallbackService.list(eventType,processStatus,uniqueId,page)));}

    /** 只允许人工重试失败的三诺回调。 */
    @PostMapping("/sinocare/callback/{id}/retry") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_SYNC_RETRY+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<JkSinocareCallbackLogResponse> retrySinocareCallback(@PathVariable Long id){return CommonResult.success(sinocareCallbackService.retry(id));}

    /** 厂商接入配置。标准 REST/JSON 厂商可以通过配置同时支持回调和主动拉取。 */
    @GetMapping("/provider/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_PROVIDER_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_PROVIDER_MANAGE)
    public CommonResult<CommonPage<JkHealthProvider>> providers(@RequestParam(required=false)String keyword,@RequestParam(required=false)String syncMode,@RequestParam(required=false)Boolean enabled,PageParamRequest page){return CommonResult.success(CommonPage.restPage(providerService.list(keyword,syncMode,enabled,page)));}

    @PostMapping("/provider/save") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_PROVIDER_MANAGE+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_PROVIDER_MANAGE)
    public CommonResult<JkHealthProvider> saveProvider(@RequestBody @Validated JkHealthProviderSaveRequest r){return CommonResult.success(providerService.save(operator(),r));}

    @PostMapping("/provider/pull") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_PROVIDER_PULL+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_PROVIDER_MANAGE)
    public CommonResult<Integer> pullProvider(@RequestBody @Validated JkHealthProviderPullRequest r){return CommonResult.success(providerService.pullOne(r.getProviderId(),Boolean.TRUE.equals(r.getResetCursor()),r.getLimit()==null?200:r.getLimit()));}

    /**
     * 平台超管协助用户核查。该接口与普通授权查询分开，必须具有专属权限、二次确认和原因。
     */
    @PostMapping("/emergency/data/list") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_EMERGENCY_VIEW+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_DATA_EMERGENCY_VIEW)
    public CommonResult<CommonPage<JkHealthData>> emergencyData(@RequestBody @Validated JkHealthEmergencyAccessRequest r,PageParamRequest page){requirePlatformSuperAdmin();return CommonResult.success(CommonPage.restPage(healthService.emergencyListData(actor.getCurrentAdmin().getId(),r.getOwnerUserId(),r.getDataType(),r.getReason(),page)));}

    @PostMapping("/emergency/data/export") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_EMERGENCY_EXPORT+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_DATA_EMERGENCY_VIEW)
    public void emergencyExport(@RequestBody @Validated JkHealthEmergencyAccessRequest r,HttpServletResponse response){requirePlatformSuperAdmin();csvExport.write(response,healthService.emergencyExportData(actor.getCurrentAdmin().getId(),r.getOwnerUserId(),r.getDataType(),r.getReason()),"平台协助核查健康数据.csv");}

    /** 只返回配置是否就绪，绝不回显回调密钥和加密密钥。 */
    @GetMapping("/integration/status") @PreAuthorize("hasAuthority('"+JkPermissionCodes.ADMIN_HEALTH_INTEGRATION_STATUS+"')") @JkBizPermission(value=JkBizPermissionCodes.HEALTH_ADMIN_MANAGE)
    public CommonResult<JkHealthIntegrationStatusResponse> integrationStatus(){return CommonResult.success(service.integrationStatus());}

    private void requirePlatformSuperAdmin(){if(!actor.isPlatformSuperAdmin(actor.getCurrentAdmin()))throw new IllegalStateException("仅平台超级管理员可使用健康协助核查能力");}
    private Long healthViewer(){Long linked=actor.getLinkedFrontUserId(actor.getCurrentAdmin());if(linked==null)throw new IllegalStateException("查看或导出用户健康明细前，必须先将后台账号映射到已获用户授权的健康顾问账号");return linked;}
    private Long operator(){Long linked=actor.getLinkedFrontUserId(actor.getCurrentAdmin());if(linked!=null)return linked;if(actor.isPlatformSuperAdmin(actor.getCurrentAdmin()))return -Long.valueOf(actor.getCurrentAdmin().getId());throw new IllegalStateException("后台管理员未绑定业务用户");}
}
