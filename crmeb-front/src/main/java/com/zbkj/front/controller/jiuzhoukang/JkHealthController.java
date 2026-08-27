package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.*;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthService;
import com.zbkj.service.service.jiuzhoukang.health.SinocareAuthorizationService;
import com.zbkj.service.service.jiuzhoukang.support.JkHealthCsvExportSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

/**
 * 第五阶段健康中心前台接口。
 * <p>接口层只负责取得当前登录用户和基础参数，健康授权、敏感字段解密与访问留痕全部由 Service 统一处理。</p>
 */
@RestController
@RequestMapping("api/front/jk/health")
@Api(tags = "九州康健康数据")
public class JkHealthController {
    @Autowired
    private FrontTokenComponent token;
    @Autowired
    private JkHealthService healthService;
    @Autowired
    private SinocareAuthorizationService sinocareAuthorizationService;
    @Autowired
    private JkHealthCsvExportSupport csvExport;

    private Long userId() {
        return Long.valueOf(token.getUserId());
    }

    @GetMapping("/dashboard")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    @ApiOperation("本人健康首页")
    public CommonResult<JkHealthDashboardResponse> dashboard() {
        return CommonResult.success(healthService.dashboard(userId()));
    }

    @GetMapping("/sinocare/device/status")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    @ApiOperation("三诺设备授权与同步状态")
    public CommonResult<JkSinocareDeviceStatusResponse> deviceStatus() {
        return CommonResult.success(healthService.deviceStatus(userId()));
    }

    @GetMapping("/glucose/trend")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    @ApiOperation("血糖趋势")
    public CommonResult<JkGlucoseTrendResponse> glucoseTrend(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.util.Date startAt,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.util.Date endAt) {
        return CommonResult.success(healthService.glucoseTrend(userId(), startAt, endAt));
    }

    @GetMapping("/sinocare/report/list")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<List<JkSinocareReportResponse>> sinocareReports(@RequestParam(required = false) String reportType) {
        return CommonResult.success(healthService.sinocareReports(userId(), reportType));
    }

    @GetMapping("/sinocare/report/{id}/file")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public void sinocareReportFile(@PathVariable Long id, HttpServletResponse response) {
        healthService.downloadSinocareReportFile(userId(), id, response);
    }

    @GetMapping("/sinocare/report/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<JkSinocareReportResponse> sinocareReport(@PathVariable Long id) {
        return CommonResult.success(healthService.sinocareReport(userId(), id));
    }

    /**
     * 供小程序获取并携带至三诺 H5 的稳定服务商用户标识。
     */
    @PostMapping("/sinocare/authorization/prepare")
    public CommonResult<JkSinocareAuthorizationPrepareResponse> prepareSinocareAuthorization() {
        return CommonResult.success(sinocareAuthorizationService.buildAuthorizationUrl(userId()));
    }

    @GetMapping("/profile")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<JkHealthProfile> profile() {
        return CommonResult.success(healthService.profile(userId(), userId()));
    }

    @PostMapping("/profile")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<JkHealthProfile> saveProfile(@RequestBody @Validated JkHealthProfileSaveRequest request) {
        return CommonResult.success(healthService.saveProfile(userId(), request));
    }

    @PostMapping("/glucose")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<JkHealthData> glucose(@RequestBody @Validated JkHealthGlucoseSaveRequest request) {
        return CommonResult.success(healthService.saveGlucose(userId(), request));
    }

    @PostMapping("/diet")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<JkHealthData> diet(@RequestBody @Validated JkHealthLifestyleSaveRequest request) {
        request.setDataType("DIET");
        return CommonResult.success(healthService.saveLifestyle(userId(), request));
    }

    @PostMapping("/exercise")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<JkHealthData> exercise(@RequestBody @Validated JkHealthLifestyleSaveRequest request) {
        request.setDataType("EXERCISE");
        return CommonResult.success(healthService.saveLifestyle(userId(), request));
    }

    @PostMapping("/medicine")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<JkHealthData> medicine(@RequestBody @Validated JkHealthLifestyleSaveRequest request) {
        request.setDataType("MEDICINE");
        return CommonResult.success(healthService.saveLifestyle(userId(), request));
    }

    @GetMapping("/data/list")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<CommonPage<JkHealthData>> list(@RequestParam(required = false) String dataType, PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(healthService.listData(userId(), userId(), dataType, page)));
    }

    @GetMapping("/data/export")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_EXPORT_SELF)
    public void exportSelf(@RequestParam(required = false) String dataType, HttpServletResponse response) {
        csvExport.write(response, healthService.exportData(userId(), userId(), dataType), "我的健康数据.csv");
    }

    @GetMapping("/alert/list")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<CommonPage<JkHealthAlertRecord>> myAlerts(@RequestParam(required = false) String status, PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(healthService.myAlerts(userId(), status, page)));
    }

    @GetMapping("/data/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_SELF)
    public CommonResult<JkHealthData> detail(@PathVariable Long id) {
        return CommonResult.success(healthService.dataDetail(userId(), userId(), id));
    }

    @GetMapping("/device/list")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DEVICE_BIND)
    public CommonResult<List<JkHealthDeviceBind>> devices() {
        return CommonResult.success(healthService.myDevices(userId()));
    }

    @PostMapping("/device/bind")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DEVICE_BIND)
    public CommonResult<JkHealthDeviceBind> bind(@RequestBody @Validated JkHealthDeviceBindRequest request) {
        return CommonResult.success(healthService.bindDevice(userId(), request));
    }

    @PostMapping("/device/{id}/unbind")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DEVICE_BIND)
    public CommonResult<Boolean> unbind(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return CommonResult.success(healthService.unbindDevice(userId(), id, reason));
    }

    @GetMapping("/authorization/list")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_AUTH_MANAGE)
    public CommonResult<List<JkHealthAuthorization>> authorizations() {
        return CommonResult.success(healthService.myAuthorizations(userId()));
    }

    @GetMapping("/authorization/advisor-options")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_AUTH_MANAGE)
    public CommonResult<List<JkOptionResponse>> advisors(@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "20") int limit) {
        return CommonResult.success(healthService.advisorOptions(keyword, limit));
    }

    @PostMapping("/authorization")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_AUTH_MANAGE)
    public CommonResult<JkHealthAuthorization> authorize(@RequestBody @Validated JkHealthAuthorizationSaveRequest request) {
        return CommonResult.success(healthService.authorize(userId(), request));
    }

    @PostMapping("/authorization/{id}/revoke")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_AUTH_MANAGE)
    public CommonResult<Boolean> revoke(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return CommonResult.success(healthService.revokeAuthorization(userId(), id, reason));
    }

    @GetMapping("/authorized/owners")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_AUTHORIZED)
    public CommonResult<List<JkHealthAuthorizedOwnerResponse>> authorizedOwners() {
        return CommonResult.success(healthService.authorizedOwners(userId()));
    }

    @GetMapping("/authorized/{ownerUserId}/data")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_AUTHORIZED)
    public CommonResult<CommonPage<JkHealthData>> authorizedData(@PathVariable Long ownerUserId, @RequestParam(required = false) String dataType, PageParamRequest page) {
        return CommonResult.success(CommonPage.restPage(healthService.listData(userId(), ownerUserId, dataType, page)));
    }

    @GetMapping("/authorized/{ownerUserId}/export")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_EXPORT_AUTHORIZED)
    public void exportAuthorized(@PathVariable Long ownerUserId, @RequestParam(required = false) String dataType, HttpServletResponse response) {
        csvExport.write(response, healthService.exportData(userId(), ownerUserId, dataType), "授权健康数据.csv");
    }

    @GetMapping("/authorized/{ownerUserId}/profile")
    @JkBizPermission(value = JkBizPermissionCodes.HEALTH_DATA_VIEW_AUTHORIZED)
    public CommonResult<JkHealthProfile> authorizedProfile(@PathVariable Long ownerUserId) {
        return CommonResult.success(healthService.profile(userId(), ownerUserId));
    }
}
