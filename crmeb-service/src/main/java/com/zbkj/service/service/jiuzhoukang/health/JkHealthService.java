package com.zbkj.service.service.jiuzhoukang.health;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.*;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

/**
 * 第五阶段健康数据业务总入口。
 *
 * <p>健康数据属于敏感数据，Controller 禁止直接调用 DAO。所有查看行为必须经过本服务，
 * 这样才能统一完成“本人/授权范围判断、敏感字段解密、访问日志和越权风险记录”。</p>
 */
public interface JkHealthService {
    /** 汇总本人最近血糖、今日记录、预警和设备数量。 */
    JkHealthDashboardResponse dashboard(Long userId);

    /** 三诺授权、设备监测和首条血糖同步状态。 */
    JkSinocareDeviceStatusResponse deviceStatus(Long userId);

    /** 指定时间范围内的血糖曲线及汇总值。 */
    JkGlucoseTrendResponse glucoseTrend(Long userId, java.util.Date startAt, java.util.Date endAt);

    List<JkSinocareReportResponse> sinocareReports(Long userId, String reportType);

    JkSinocareReportResponse sinocareReport(Long userId, Long reportId);

    /** 校验授权归属后将第三方 PDF 报告以文件流透传给前端（不暴露第三方地址）。 */
    void downloadSinocareReportFile(Long userId, Long reportId, HttpServletResponse response);

    /** 保存本人健康档案；敏感备注由实现层加密后落库。 */
    JkHealthProfile saveProfile(Long userId, JkHealthProfileSaveRequest request);

    /** 查看健康档案。viewer 与 owner 不同时必须具有 PROFILE 授权。 */
    JkHealthProfile profile(Long viewerUserId, Long ownerUserId);

    /** 人工记录血糖，requestNo 用于防止重复提交。 */
    JkHealthData saveGlucose(Long userId, JkHealthGlucoseSaveRequest request);

    /** 保存饮食、运动或用药记录，数据类型由对应 Controller 固定，不信任前端传值。 */
    JkHealthData saveLifestyle(Long userId, JkHealthLifestyleSaveRequest request);

    /**
     * 按授权范围查询健康数据。
     * <p>dataType 为空时不能理解成“授权全部”，实现层会按 scopeCodes 过滤返回类型。</p>
     */
    PageInfo<JkHealthData> listData(Long viewerUserId, Long ownerUserId, String dataType, PageParamRequest page);

    /** 查看单条健康记录，并再次校验该记录类型是否在授权范围内。 */
    JkHealthData dataDetail(Long viewerUserId, Long ownerUserId, Long id);

    /** 最多导出 5000 条；他人数据还必须 allowExport=true，并记录 EXPORT_DATA 访问日志。 */
    List<JkHealthData> exportData(Long viewerUserId, Long ownerUserId, String dataType);

    /** 平台超管协助用户核查健康明细；只能由专属后台接口调用，必须填写原因并强审计。 */
    PageInfo<JkHealthData> emergencyListData(Integer adminId, Long ownerUserId, String dataType, String reason, PageParamRequest page);

    /** 平台超管紧急导出；仍受 5000 条上限并记录 EMERGENCY_EXPORT。 */
    List<JkHealthData> emergencyExportData(Integer adminId, Long ownerUserId, String dataType, String reason);

    /** 查询本人异常提醒，健康顾问不能借此接口读取他人的预警。 */
    PageInfo<JkHealthAlertRecord> myAlerts(Long userId, String status, PageParamRequest page);

    /** 查询本人设备绑定历史。 */
    List<JkHealthDeviceBind> myDevices(Long userId);

    /** 使用设备编号和一次性绑定码绑定设备，同一设备只能有一个有效绑定。 */
    JkHealthDeviceBind bindDevice(Long userId, JkHealthDeviceBindRequest request);

    /** 解绑时保留历史记录，不物理删除原绑定。 */
    Boolean unbindDevice(Long userId, Long bindId, String reason);

    /** 查询本人发出的授权。 */
    List<JkHealthAuthorization> myAuthorizations(Long ownerUserId);

    /** 授权给有效健康顾问；范围和是否允许导出均由数据所有人明确选择。 */
    JkHealthAuthorization authorize(Long ownerUserId, JkHealthAuthorizationSaveRequest request);

    /** 撤销后立即失效，后续查询和导出必须重新授权。 */
    Boolean revokeAuthorization(Long ownerUserId, Long authorizationId, String reason);

    /** 健康顾问查询当前仍有效的授权用户。 */
    List<JkHealthAuthorizedOwnerResponse> authorizedOwners(Long viewerUserId);

    /** 授权页面使用的健康顾问选择器，只返回身份有效且未冻结的用户。 */
    List<JkOptionResponse> advisorOptions(String keyword, int limit);

    /**
     * 第三方设备数据入库。
     * <p>调用前必须在回调 Controller 完成签名和时间戳校验；externalNo 负责业务幂等。</p>
     */
    JkHealthData ingestDeviceData(JkHealthDeviceCallbackRequest request);
}
