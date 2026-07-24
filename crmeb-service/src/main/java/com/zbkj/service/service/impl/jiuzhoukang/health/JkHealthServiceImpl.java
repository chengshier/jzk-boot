package com.zbkj.service.service.impl.jiuzhoukang.health;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.*;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthService;
import com.zbkj.service.service.jiuzhoukang.risk.JkRiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 第五阶段健康数据核心服务。
 * <p>所有“查看他人健康数据”的入口必须经过本类的 authorizeAccess；Controller 和报表不得直接查询健康明细表。</p>
 * <p>本人访问也会记录访问日志，以满足敏感数据完整留痕要求。</p>
 */
@Service
public class JkHealthServiceImpl implements JkHealthService {
    private static final Set<String> LIFESTYLE_TYPES = new HashSet<>(Arrays.asList("DIET", "EXERCISE", "MEDICINE"));
    @Autowired private JkHealthProfileDao profileDao;
    @Autowired private JkHealthDeviceDao deviceDao;
    @Autowired private JkHealthDeviceBindDao bindDao;
    @Autowired private JkHealthDataDao dataDao;
    @Autowired private JkHealthAuthorizationDao authorizationDao;
    @Autowired private JkHealthAlertRuleDao alertRuleDao;
    @Autowired private JkHealthAlertRecordDao alertRecordDao;
    @Autowired private JkHealthAccessLogDao accessLogDao;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private UserService userService;
    @Autowired private JkHealthSensitiveCodec codec;
    @Autowired private JkRiskService riskService;

    @Override
    public JkHealthDashboardResponse dashboard(Long userId) {
        authorizeAccess(userId, userId, "DASHBOARD", "GLUCOSE");
        JkHealthDashboardResponse response = new JkHealthDashboardResponse();
        response.setProfile(profile(userId, userId));
        List<JkHealthData> recent = dataDao.selectList(new LambdaQueryWrapper<JkHealthData>()
                .eq(JkHealthData::getUserId, userId).eq(JkHealthData::getIsDeleted, false)
                .orderByDesc(JkHealthData::getMeasuredAt).last("limit 10"));
        recent.forEach(this::decodeData);
        response.setRecentRecords(recent);
        response.setLatestGlucose(recent.stream().filter(v -> "GLUCOSE".equals(v.getDataType())).findFirst().orElse(null));
        Date start = startOfDay(new Date());
        response.setTodayRecordCount(dataDao.selectCount(new LambdaQueryWrapper<JkHealthData>()
                .eq(JkHealthData::getUserId, userId).ge(JkHealthData::getMeasuredAt, start).eq(JkHealthData::getIsDeleted, false)));
        response.setActiveAlertCount(alertRecordDao.selectCount(new LambdaQueryWrapper<JkHealthAlertRecord>()
                .eq(JkHealthAlertRecord::getUserId, userId).in(JkHealthAlertRecord::getStatus, Arrays.asList("OPEN", "ACKNOWLEDGED"))
                .eq(JkHealthAlertRecord::getIsDeleted, false)));
        response.setBoundDeviceCount(bindDao.selectCount(new LambdaQueryWrapper<JkHealthDeviceBind>()
                .eq(JkHealthDeviceBind::getUserId, userId).eq(JkHealthDeviceBind::getStatus, "ACTIVE")
                .eq(JkHealthDeviceBind::getIsDeleted, false)));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthProfile saveProfile(Long userId, JkHealthProfileSaveRequest request) {
        if (request.getGlucoseTargetMin() != null && request.getGlucoseTargetMax() != null
                && request.getGlucoseTargetMin().compareTo(request.getGlucoseTargetMax()) >= 0) {
            throw new CrmebException("目标血糖下限必须小于上限");
        }
        JkHealthProfile entity = profileDao.selectOne(new LambdaQueryWrapper<JkHealthProfile>()
                .eq(JkHealthProfile::getUserId, userId).eq(JkHealthProfile::getIsDeleted, false).last("limit 1"));
        Date now = new Date();
        if (entity == null) {
            entity = new JkHealthProfile().setUserId(userId).setIsDeleted(false).setCreateUserId(userId)
                    .setCreateTime(now).setVersion(0);
        }
        entity.setHeightCm(request.getHeightCm()).setWeightKg(request.getWeightKg()).setDiabetesType(request.getDiabetesType())
                .setGlucoseTargetMin(request.getGlucoseTargetMin()).setGlucoseTargetMax(request.getGlucoseTargetMax())
                .setRemarkCipher(codec.encode(request.getRemark())).setUpdateUserId(userId).setUpdateTime(now);
        if (entity.getId() == null) profileDao.insert(entity); else profileDao.updateById(entity);
        entity.setRemark(request.getRemark());
        entity.setRemarkCipher(null); // 响应只返回解密后的展示字段，不暴露密文或开发模式明文载荷。
        logAccess(userId, userId, null, "UPDATE_PROFILE", "PROFILE", "ALLOWED", null, "APP");
        return entity;
    }

    @Override
    public JkHealthProfile profile(Long viewerUserId, Long ownerUserId) {
        authorizeAccess(viewerUserId, ownerUserId, "VIEW_PROFILE", "PROFILE");
        JkHealthProfile entity = profileDao.selectOne(new LambdaQueryWrapper<JkHealthProfile>()
                .eq(JkHealthProfile::getUserId, ownerUserId).eq(JkHealthProfile::getIsDeleted, false).last("limit 1"));
        if (entity != null) {
            entity.setRemark(codec.decode(entity.getRemarkCipher()));
            entity.setRemarkCipher(null);
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthData saveGlucose(Long userId, JkHealthGlucoseSaveRequest request) {
        String externalNo = StrUtil.isBlank(request.getRequestNo()) ? "MANUAL-GLUCOSE-" + IdWorker.getIdStr()
                : "MANUAL-GLUCOSE-" + userId + "-" + request.getRequestNo();
        JkHealthData old = byExternalNo(externalNo);
        if (old != null) return decodeData(old);
        JSONObject detail = new JSONObject();
        detail.put("remark", request.getRemark());
        JkHealthData entity = baseData(userId, "GLUCOSE", request.getMeasuredAt(), externalNo, "MANUAL")
                .setNumericValue(request.getValue()).setUnit("mmol/L").setPeriodCode(request.getPeriod())
                .setDetailCipher(codec.encode(detail.toJSONString()));
        dataDao.insert(entity);
        evaluateAlerts(entity);
        logAccess(userId, userId, null, "CREATE_DATA", "GLUCOSE", "ALLOWED", null, "APP");
        return decodeData(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthData saveLifestyle(Long userId, JkHealthLifestyleSaveRequest request) {
        String type = request.getDataType() == null ? "" : request.getDataType().toUpperCase(Locale.ROOT);
        if (!LIFESTYLE_TYPES.contains(type)) throw new CrmebException("不支持的健康记录类型");
        String externalNo = StrUtil.isBlank(request.getRequestNo()) ? "MANUAL-" + type + "-" + IdWorker.getIdStr()
                : "MANUAL-" + type + "-" + userId + "-" + request.getRequestNo();
        JkHealthData old = byExternalNo(externalNo);
        if (old != null) return decodeData(old);
        JSONObject detail = new JSONObject();
        detail.put("content", request.getContent()); detail.put("category", request.getCategory());
        detail.put("durationMinutes", request.getDurationMinutes()); detail.put("calories", request.getCalories());
        detail.put("remark", request.getRemark());
        JkHealthData entity = baseData(userId, type, request.getOccurredAt(), externalNo, "MANUAL")
                .setDetailCipher(codec.encode(detail.toJSONString()));
        dataDao.insert(entity);
        logAccess(userId, userId, null, "CREATE_DATA", type, "ALLOWED", null, "APP");
        return decodeData(entity);
    }

    @Override
    public PageInfo<JkHealthData> listData(Long viewerUserId, Long ownerUserId, String dataType, PageParamRequest pageParam) {
        AccessDecision decision = authorizeAccess(viewerUserId, ownerUserId, "LIST_DATA", dataType == null ? "ALL" : dataType);
        Page<JkHealthData> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkHealthData> q = new LambdaQueryWrapper<JkHealthData>()
                .eq(JkHealthData::getUserId, ownerUserId).eq(JkHealthData::getIsDeleted, false).orderByDesc(JkHealthData::getMeasuredAt);
        if (StrUtil.isNotBlank(dataType)) {
            q.eq(JkHealthData::getDataType, dataType.toUpperCase(Locale.ROOT));
        } else if (!viewerUserId.equals(ownerUserId)) {
            // 查看他人的“全部记录”并不等于授权全部类型，必须按授权 scopeCodes 收窄查询。
            List<String> authorizedTypes = parseScopeCodes(decision.scopeCodes);
            if (authorizedTypes.isEmpty()) {
                deny(viewerUserId, ownerUserId, decision.authorizationId, "LIST_DATA", "ALL", "授权范围不包含任何健康记录类型");
            }
            q.in(JkHealthData::getDataType, authorizedTypes);
        }
        List<JkHealthData> rows = dataDao.selectList(q);
        rows.forEach(this::decodeData);
        logAccess(viewerUserId, ownerUserId, decision.authorizationId, "LIST_DATA", dataType == null ? "ALL" : dataType,
                "ALLOWED", null, viewerUserId.equals(ownerUserId) ? "APP" : "AUTHORIZED");
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public JkHealthData dataDetail(Long viewerUserId, Long ownerUserId, Long id) {
        AccessDecision decision = authorizeAccess(viewerUserId, ownerUserId, "VIEW_DATA", "DETAIL");
        JkHealthData entity = dataDao.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted()) || !ownerUserId.equals(entity.getUserId())) throw new CrmebException("健康记录不存在");
        if (!viewerUserId.equals(ownerUserId) && !scopeContains(decision.scopeCodes, entity.getDataType())) {
            deny(viewerUserId, ownerUserId, decision.authorizationId, "VIEW_DATA", entity.getDataType(), "授权范围不包含该数据类型");
        }
        logAccess(viewerUserId, ownerUserId, decision.authorizationId, "VIEW_DATA", entity.getDataType(), "ALLOWED", null,
                viewerUserId.equals(ownerUserId) ? "APP" : "AUTHORIZED");
        return decodeData(entity);
    }

    @Override
    public List<JkHealthData> exportData(Long viewerUserId, Long ownerUserId, String dataType) {
        AccessDecision decision = authorizeAccess(viewerUserId, ownerUserId, "EXPORT_DATA", StrUtil.isBlank(dataType) ? "ALL" : dataType);
        if (!viewerUserId.equals(ownerUserId) && !decision.allowExport) {
            deny(viewerUserId, ownerUserId, decision.authorizationId, "EXPORT_DATA", dataType, "用户只授权查看，未授权导出");
        }
        LambdaQueryWrapper<JkHealthData> q = new LambdaQueryWrapper<JkHealthData>()
                .eq(JkHealthData::getUserId, ownerUserId).eq(JkHealthData::getIsDeleted, false)
                .orderByDesc(JkHealthData::getMeasuredAt).last("limit 5000");
        if (StrUtil.isNotBlank(dataType)) {
            q.eq(JkHealthData::getDataType, dataType.toUpperCase(Locale.ROOT));
        } else if (!viewerUserId.equals(ownerUserId)) {
            List<String> scopes = parseScopeCodes(decision.scopeCodes);
            if (scopes.isEmpty()) deny(viewerUserId, ownerUserId, decision.authorizationId, "EXPORT_DATA", "ALL", "授权范围不包含可导出的健康记录");
            q.in(JkHealthData::getDataType, scopes);
        }
        List<JkHealthData> rows = dataDao.selectList(q);
        rows.forEach(this::decodeData);
        logAccess(viewerUserId, ownerUserId, decision.authorizationId, "EXPORT_DATA", StrUtil.isBlank(dataType) ? "ALL" : dataType,
                "ALLOWED", null, viewerUserId.equals(ownerUserId) ? "APP" : "AUTHORIZED");
        return rows;
    }

    @Override
    public PageInfo<JkHealthData> emergencyListData(Integer adminId, Long ownerUserId, String dataType, String reason, PageParamRequest pageParam) {
        requireEmergencyReason(adminId, ownerUserId, reason);
        Page<JkHealthData> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkHealthData> q = new LambdaQueryWrapper<JkHealthData>()
                .eq(JkHealthData::getUserId, ownerUserId).eq(JkHealthData::getIsDeleted, false)
                .orderByDesc(JkHealthData::getMeasuredAt);
        if (StrUtil.isNotBlank(dataType)) q.eq(JkHealthData::getDataType, dataType.toUpperCase(Locale.ROOT));
        List<JkHealthData> rows = dataDao.selectList(q);
        rows.forEach(this::decodeData);
        logAccessExtended(-adminId.longValue(), ownerUserId, null, "EMERGENCY_VIEW", StrUtil.isBlank(dataType) ? "ALL" : dataType,
                "ALLOWED", null, "ADMIN", "EMERGENCY", reason, adminId);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public List<JkHealthData> emergencyExportData(Integer adminId, Long ownerUserId, String dataType, String reason) {
        requireEmergencyReason(adminId, ownerUserId, reason);
        LambdaQueryWrapper<JkHealthData> q = new LambdaQueryWrapper<JkHealthData>()
                .eq(JkHealthData::getUserId, ownerUserId).eq(JkHealthData::getIsDeleted, false)
                .orderByDesc(JkHealthData::getMeasuredAt).last("limit 5000");
        if (StrUtil.isNotBlank(dataType)) q.eq(JkHealthData::getDataType, dataType.toUpperCase(Locale.ROOT));
        List<JkHealthData> rows = dataDao.selectList(q);
        rows.forEach(this::decodeData);
        logAccessExtended(-adminId.longValue(), ownerUserId, null, "EMERGENCY_EXPORT", StrUtil.isBlank(dataType) ? "ALL" : dataType,
                "ALLOWED", null, "ADMIN", "EMERGENCY", reason, adminId);
        return rows;
    }

    private void requireEmergencyReason(Integer adminId, Long ownerUserId, String reason) {
        if (adminId == null || ownerUserId == null) throw new CrmebException("平台核查主体不能为空");
        if (StrUtil.isBlank(reason) || reason.trim().length() < 5) throw new CrmebException("平台协助核查必须填写不少于5个字的原因");
    }

    @Override
    public PageInfo<JkHealthAlertRecord> myAlerts(Long userId, String status, PageParamRequest pageParam) {
        Page<JkHealthAlertRecord> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkHealthAlertRecord> q = new LambdaQueryWrapper<JkHealthAlertRecord>()
                .eq(JkHealthAlertRecord::getUserId, userId).eq(JkHealthAlertRecord::getIsDeleted, false)
                .orderByDesc(JkHealthAlertRecord::getId);
        if (StrUtil.isNotBlank(status)) q.eq(JkHealthAlertRecord::getStatus, status);
        return CommonPage.copyPageInfo(page, alertRecordDao.selectList(q));
    }

    @Override
    public List<JkHealthDeviceBind> myDevices(Long userId) {
        List<JkHealthDeviceBind> rows = bindDao.selectList(new LambdaQueryWrapper<JkHealthDeviceBind>()
                .eq(JkHealthDeviceBind::getUserId, userId).eq(JkHealthDeviceBind::getIsDeleted, false).orderByDesc(JkHealthDeviceBind::getId));
        for (JkHealthDeviceBind row : rows) {
            JkHealthDevice d = deviceDao.selectById(row.getDeviceId());
            if (d != null) row.setDeviceSn(d.getDeviceSn()).setDeviceType(d.getDeviceType()).setDeviceModel(d.getDeviceModel());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthDeviceBind bindDevice(Long userId, JkHealthDeviceBindRequest request) {
        JkHealthDevice device = deviceDao.selectOne(new LambdaQueryWrapper<JkHealthDevice>()
                .eq(JkHealthDevice::getDeviceSn, request.getDeviceSn()).eq(JkHealthDevice::getIsDeleted, false).last("limit 1"));
        if (device == null || !"ENABLED".equals(device.getStatus())) throw new CrmebException("设备不存在或未启用");
        if (!hash(request.getBindCode()).equalsIgnoreCase(device.getBindCodeHash())) throw new CrmebException("设备绑定码错误");
        JkHealthDeviceBind active = bindDao.selectOne(new LambdaQueryWrapper<JkHealthDeviceBind>()
                .eq(JkHealthDeviceBind::getDeviceId, device.getId()).eq(JkHealthDeviceBind::getStatus, "ACTIVE")
                .eq(JkHealthDeviceBind::getIsDeleted, false).last("limit 1"));
        if (active != null) {
            if (userId.equals(active.getUserId())) return active;
            throw new CrmebException("设备已绑定其他用户");
        }
        Date now = new Date();
        JkHealthDeviceBind bind = new JkHealthDeviceBind().setBindNo("HDB" + IdWorker.getIdStr()).setDeviceId(device.getId())
                .setUserId(userId).setStatus("ACTIVE").setBindSource("USER_CODE").setBindTime(now).setIsDeleted(false)
                .setCreateUserId(userId).setUpdateUserId(userId).setCreateTime(now).setUpdateTime(now).setVersion(0);
        bindDao.insert(bind);
        return bind.setDeviceSn(device.getDeviceSn()).setDeviceType(device.getDeviceType()).setDeviceModel(device.getDeviceModel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unbindDevice(Long userId, Long bindId, String reason) {
        int rows = bindDao.update(null, new UpdateWrapper<JkHealthDeviceBind>().eq("id", bindId).eq("user_id", userId).eq("status", "ACTIVE")
                .set("status", "UNBOUND").set("unbind_time", new Date()).set("unbind_reason", reason)
                .set("update_user_id", userId).set("update_time", new Date()));
        if (rows != 1) throw new CrmebException("设备绑定记录不存在或已解绑");
        return true;
    }

    @Override
    public List<JkHealthAuthorization> myAuthorizations(Long ownerUserId) {
        List<JkHealthAuthorization> rows = authorizationDao.selectList(new LambdaQueryWrapper<JkHealthAuthorization>()
                .eq(JkHealthAuthorization::getOwnerUserId, ownerUserId).eq(JkHealthAuthorization::getIsDeleted, false)
                .orderByDesc(JkHealthAuthorization::getId));
        rows.forEach(this::enrichAuthorization);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthAuthorization authorize(Long ownerUserId, JkHealthAuthorizationSaveRequest request) {
        if (ownerUserId.equals(request.getGranteeUserId())) throw new CrmebException("无需授权给本人");
        requireHealthAdvisor(request.getGranteeUserId());
        Date now = new Date();
        Date effective = request.getEffectiveTime() == null ? now : request.getEffectiveTime();
        if (request.getExpireTime() != null && !request.getExpireTime().after(effective)) throw new CrmebException("授权失效时间必须晚于生效时间");
        authorizationDao.update(null, new UpdateWrapper<JkHealthAuthorization>()
                .eq("owner_user_id", ownerUserId).eq("grantee_user_id", request.getGranteeUserId()).eq("status", "ACTIVE")
                .set("status", "REVOKED").set("revoke_time", now).set("revoke_reason", "新授权替换旧授权").set("update_time", now));
        JkHealthAuthorization entity = new JkHealthAuthorization().setAuthorizationNo("HA" + IdWorker.getIdStr())
                .setOwnerUserId(ownerUserId).setGranteeUserId(request.getGranteeUserId())
                .setGranteeRoleCode("health_advisor").setScopeCodes(normalizeScopes(request.getScopeCodes()))
                .setAllowExport(Boolean.TRUE.equals(request.getAllowExport()))
                .setEffectiveTime(effective).setExpireTime(request.getExpireTime()).setStatus("ACTIVE")
                .setIsDeleted(false).setCreateUserId(ownerUserId).setUpdateUserId(ownerUserId)
                .setCreateTime(now).setUpdateTime(now).setVersion(0);
        authorizationDao.insert(entity);
        return enrichAuthorization(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean revokeAuthorization(Long ownerUserId, Long authorizationId, String reason) {
        int rows = authorizationDao.update(null, new UpdateWrapper<JkHealthAuthorization>()
                .eq("id", authorizationId).eq("owner_user_id", ownerUserId).eq("status", "ACTIVE")
                .set("status", "REVOKED").set("revoke_time", new Date()).set("revoke_reason", reason)
                .set("update_user_id", ownerUserId).set("update_time", new Date()));
        if (rows != 1) throw new CrmebException("授权不存在或已失效");
        return true;
    }

    @Override
    public List<JkHealthAuthorizedOwnerResponse> authorizedOwners(Long viewerUserId) {
        Date now = new Date();
        List<JkHealthAuthorization> rows = authorizationDao.selectList(new LambdaQueryWrapper<JkHealthAuthorization>()
                .eq(JkHealthAuthorization::getGranteeUserId, viewerUserId).eq(JkHealthAuthorization::getStatus, "ACTIVE")
                .le(JkHealthAuthorization::getEffectiveTime, now)
                .and(q -> q.isNull(JkHealthAuthorization::getExpireTime).or().gt(JkHealthAuthorization::getExpireTime, now))
                .eq(JkHealthAuthorization::getIsDeleted, false).orderByDesc(JkHealthAuthorization::getId));
        return rows.stream().map(v -> {
            User user = userService.getById(v.getOwnerUserId().intValue());
            JkHealthAuthorizedOwnerResponse r = new JkHealthAuthorizedOwnerResponse();
            r.setOwnerUserId(v.getOwnerUserId()); r.setAuthorizationId(v.getId()); r.setScopeCodes(v.getScopeCodes()); r.setAllowExport(Boolean.TRUE.equals(v.getAllowExport()));
            if (user != null) { r.setOwnerName(displayName(user)); r.setOwnerPhoneMasked(maskPhone(user.getPhone())); }
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public List<JkOptionResponse> advisorOptions(String keyword, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<JkUserBusinessRole> roles = userRoleDao.selectList(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getRoleCode, "health_advisor")
                .eq(JkUserBusinessRole::getAuditStatus, "EFFECTIVE")
                .eq(JkUserBusinessRole::getEffectiveStatus, "ENABLED")
                .eq(JkUserBusinessRole::getIsDeleted, false).orderByDesc(JkUserBusinessRole::getId).last("limit " + safeLimit * 3));
        List<JkOptionResponse> result = new ArrayList<>();
        String k = keyword == null ? "" : keyword.trim();
        for (JkUserBusinessRole role : roles) {
            User user = userService.getById(role.getUserId().intValue());
            if (user == null) continue;
            String label = displayName(user) + (StrUtil.isBlank(user.getPhone()) ? "" : "（" + maskPhone(user.getPhone()) + "）");
            if (StrUtil.isNotBlank(k) && !label.contains(k) && !String.valueOf(user.getUid()).contains(k)) continue;
            JkOptionResponse option = new JkOptionResponse();
            option.setValue(String.valueOf(user.getUid())); option.setLabel(label);
            result.add(option); if (result.size() >= safeLimit) break;
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthData ingestDeviceData(JkHealthDeviceCallbackRequest request) {
        JkHealthData old = byExternalNo(request.getProviderCode() + ":" + request.getExternalNo());
        if (old != null) return decodeData(old);
        JkHealthDevice device = deviceDao.selectOne(new LambdaQueryWrapper<JkHealthDevice>()
                .eq(JkHealthDevice::getDeviceSn, request.getDeviceSn()).eq(JkHealthDevice::getProviderCode, request.getProviderCode())
                .eq(JkHealthDevice::getStatus, "ENABLED").eq(JkHealthDevice::getIsDeleted, false).last("limit 1"));
        if (device == null) throw new CrmebException("回调设备未登记或未启用");
        JkHealthDeviceBind bind = bindDao.selectOne(new LambdaQueryWrapper<JkHealthDeviceBind>()
                .eq(JkHealthDeviceBind::getDeviceId, device.getId()).eq(JkHealthDeviceBind::getStatus, "ACTIVE")
                .eq(JkHealthDeviceBind::getIsDeleted, false).last("limit 1"));
        if (bind == null) throw new CrmebException("设备尚未绑定用户");
        JkHealthData entity = baseData(bind.getUserId(), "GLUCOSE", request.getMeasuredAt(), request.getProviderCode() + ":" + request.getExternalNo(), "DEVICE")
                .setDeviceId(device.getId()).setNumericValue(request.getValue()).setUnit(StrUtil.isBlank(request.getUnit()) ? "mmol/L" : request.getUnit())
                .setPeriodCode(request.getPeriod()).setDetailCipher(codec.encode("{}"));
        dataDao.insert(entity);
        evaluateAlerts(entity);
        device.setLastSyncTime(new Date()).setUpdateTime(new Date()); deviceDao.updateById(device);
        return decodeData(entity);
    }

    /**
     * 访问判定的核心边界：本人直接允许；他人必须存在当前有效授权，且授权范围包含目标数据类型。
     * 拒绝访问也会写访问日志并记录风险事件，但不会在风险明细中保存健康值。
     */
    private AccessDecision authorizeAccess(Long viewer, Long owner, String action, String scope) {
        if (viewer == null || owner == null) throw new CrmebException("健康数据访问用户不能为空");
        if (viewer.equals(owner)) return new AccessDecision(null, "ALL", true);
        Date now = new Date();
        List<JkHealthAuthorization> list = authorizationDao.selectList(new LambdaQueryWrapper<JkHealthAuthorization>()
                .eq(JkHealthAuthorization::getOwnerUserId, owner).eq(JkHealthAuthorization::getGranteeUserId, viewer)
                .eq(JkHealthAuthorization::getStatus, "ACTIVE").le(JkHealthAuthorization::getEffectiveTime, now)
                .and(q -> q.isNull(JkHealthAuthorization::getExpireTime).or().gt(JkHealthAuthorization::getExpireTime, now))
                .eq(JkHealthAuthorization::getIsDeleted, false).orderByDesc(JkHealthAuthorization::getId));
        for (JkHealthAuthorization auth : list) {
            if ("ALL".equals(scope) || "DETAIL".equals(scope) || scopeContains(auth.getScopeCodes(), scope)) {
                return new AccessDecision(auth.getId(), auth.getScopeCodes(), Boolean.TRUE.equals(auth.getAllowExport()));
            }
        }
        deny(viewer, owner, null, action, scope, "未获得用户授权或授权已失效");
        return null;
    }


    private List<String> parseScopeCodes(String scopeCodes) {
        if (StrUtil.isBlank(scopeCodes)) return Collections.emptyList();
        return Arrays.stream(scopeCodes.split(","))
                .map(v -> v == null ? "" : v.trim().toUpperCase(Locale.ROOT))
                .filter(StrUtil::isNotBlank)
                .filter(v -> !"PROFILE".equals(v))
                .distinct()
                .collect(Collectors.toList());
    }

    private void deny(Long viewer, Long owner, Long authId, String action, String scope, String reason) {
        logAccess(viewer, owner, authId, action, scope, "DENIED", reason, "AUTHORIZED");
        riskService.record("HEALTH_ACCESS_DENIED", "MEDIUM", "HEALTH_ACCESS", null, null, viewer,
                "未授权健康数据访问被拦截", "{\"ownerUserId\":" + owner + ",\"scope\":\"" + safe(scope) + "\"}");
        throw new CrmebException(reason);
    }

    private void evaluateAlerts(JkHealthData data) {
        if (!"GLUCOSE".equals(data.getDataType()) || data.getNumericValue() == null) return;
        List<JkHealthAlertRule> rules = alertRuleDao.selectList(new LambdaQueryWrapper<JkHealthAlertRule>()
                .eq(JkHealthAlertRule::getDataType, "GLUCOSE").eq(JkHealthAlertRule::getEnabled, true)
                .eq(JkHealthAlertRule::getIsDeleted, false)
                .and(q -> q.isNull(JkHealthAlertRule::getOwnerUserId).or().eq(JkHealthAlertRule::getOwnerUserId, data.getUserId()))
                .orderByDesc(JkHealthAlertRule::getOwnerUserId).orderByDesc(JkHealthAlertRule::getId));
        for (JkHealthAlertRule rule : rules) {
            if (StrUtil.isNotBlank(rule.getPeriodCode()) && !rule.getPeriodCode().equals(data.getPeriodCode())) continue;
            boolean low = rule.getMinValue() != null && data.getNumericValue().compareTo(rule.getMinValue()) < 0;
            boolean high = rule.getMaxValue() != null && data.getNumericValue().compareTo(rule.getMaxValue()) > 0;
            if (!low && !high) continue;
            JkHealthAlertRecord old = alertRecordDao.selectOne(new LambdaQueryWrapper<JkHealthAlertRecord>()
                    .eq(JkHealthAlertRecord::getHealthDataId, data.getId()).eq(JkHealthAlertRecord::getRuleId, rule.getId()).last("limit 1"));
            if (old == null) {
                Date now = new Date();
                alertRecordDao.insert(new JkHealthAlertRecord().setHealthDataId(data.getId()).setRuleId(rule.getId())
                        .setUserId(data.getUserId()).setDataType(data.getDataType()).setMeasuredValue(data.getNumericValue())
                        .setAlertLevel(rule.getAlertLevel()).setStatus("OPEN").setIsDeleted(false)
                        .setCreateTime(now).setUpdateTime(now).setVersion(0));
                data.setRiskLevel(rule.getAlertLevel()); dataDao.updateById(data);
            }
            break;
        }
    }

    private JkHealthData baseData(Long userId, String type, Date measuredAt, String externalNo, String source) {
        Date now = new Date();
        return new JkHealthData().setExternalNo(externalNo).setUserId(userId).setDataType(type)
                .setMeasuredAt(measuredAt == null ? now : measuredAt).setSourceType(source).setRiskLevel("NORMAL")
                .setStatus("VALID").setIsDeleted(false).setCreateUserId(userId).setUpdateUserId(userId)
                .setCreateTime(now).setUpdateTime(now).setVersion(0);
    }

    private JkHealthData byExternalNo(String externalNo) {
        return dataDao.selectOne(new LambdaQueryWrapper<JkHealthData>().eq(JkHealthData::getExternalNo, externalNo).last("limit 1"));
    }

    private JkHealthData decodeData(JkHealthData entity) {
        if (entity == null) return null;
        entity.setDetail(codec.decode(entity.getDetailCipher()));
        entity.setDetailCipher(null); // API 不返回数据库密文；开发明文模式下尤其必须清空该字段。
        entity.setDataTypeText(dataTypeText(entity.getDataType()));
        entity.setRiskLevelText(riskText(entity.getRiskLevel()));
        return entity;
    }

    private JkHealthAuthorization enrichAuthorization(JkHealthAuthorization auth) {
        User owner = userService.getById(auth.getOwnerUserId().intValue());
        User grantee = userService.getById(auth.getGranteeUserId().intValue());
        auth.setOwnerName(displayName(owner)).setGranteeName(displayName(grantee));
        auth.setStatusText("ACTIVE".equals(auth.getStatus()) ? "有效" : "REVOKED".equals(auth.getStatus()) ? "已撤销" : "已失效");
        return auth;
    }

    private void requireHealthAdvisor(Long userId) {
        Integer count = userRoleDao.selectCount(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, userId).eq(JkUserBusinessRole::getRoleCode, "health_advisor")
                .eq(JkUserBusinessRole::getAuditStatus, "EFFECTIVE").eq(JkUserBusinessRole::getEffectiveStatus, "ENABLED")
                .eq(JkUserBusinessRole::getIsDeleted, false));
        if (count == null || count <= 0) throw new CrmebException("只能授权给已生效的健康顾问");
    }

    private void logAccess(Long viewer, Long owner, Long authId, String action, String scope, String result, String reason, String source) {
        String accessType = viewer != null && viewer.equals(owner) ? "SELF" : "AUTHORIZED";
        logAccessExtended(viewer, owner, authId, action, scope, result, reason, source, accessType, null, null);
    }

    private void logAccessExtended(Long viewer, Long owner, Long authId, String action, String scope, String result,
                                   String denyReason, String source, String accessType, String accessReason, Integer adminId) {
        Date now = new Date();
        accessLogDao.insert(new JkHealthAccessLog().setRequestNo("HAL" + IdWorker.getIdStr()).setViewerUserId(viewer)
                .setOwnerUserId(owner).setAuthorizationId(authId).setActionType(action).setScopeCode(scope)
                .setAccessResult(result).setDenyReason(denyReason).setOperateSource(source).setAccessType(accessType)
                .setAccessReason(accessReason).setAdminId(adminId).setAccessTime(now)
                .setIsDeleted(false).setCreateTime(now));
    }

    private String normalizeScopes(List<String> scopes) {
        Set<String> allowed = new LinkedHashSet<>();
        for (String v : scopes) {
            if (v == null) continue;
            String code = v.trim().toUpperCase(Locale.ROOT);
            if (Arrays.asList("PROFILE", "GLUCOSE", "DIET", "EXERCISE", "MEDICINE").contains(code)) allowed.add(code);
        }
        if (allowed.isEmpty()) throw new CrmebException("至少选择一个有效授权范围");
        return String.join(",", allowed);
    }

    private boolean scopeContains(String scopeCodes, String scope) {
        if ("ALL".equals(scope) || "DETAIL".equals(scope)) return true;
        if (scopeCodes == null) return false;
        for (String v : scopeCodes.split(",")) if (v.trim().equalsIgnoreCase(scope)) return true;
        return false;
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(); for (byte b : bytes) sb.append(String.format("%02x", b & 0xff)); return sb.toString();
        } catch (Exception e) { throw new CrmebException("绑定码校验失败"); }
    }

    private Date startOfDay(Date date) { Calendar c = Calendar.getInstance(); c.setTime(date); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTime(); }
    private String displayName(User u) { if (u == null) return null; return StrUtil.isNotBlank(u.getRealName()) ? u.getRealName() : u.getNickname(); }
    private String maskPhone(String p) { return p == null || p.length() < 7 ? p : p.substring(0,3) + "****" + p.substring(p.length()-4); }
    private String safe(String s) { return s == null ? "" : s.replace("\\", "").replace("\"", ""); }
    private String dataTypeText(String v) { if ("GLUCOSE".equals(v)) return "血糖"; if ("DIET".equals(v)) return "饮食"; if ("EXERCISE".equals(v)) return "运动"; if ("MEDICINE".equals(v)) return "用药"; return v; }
    private String riskText(String v) { if ("HIGH".equals(v)) return "高风险"; if ("MEDIUM".equals(v)) return "中风险"; if ("LOW".equals(v)) return "低风险"; return "正常"; }

    private static class AccessDecision { final Long authorizationId; final String scopeCodes; final boolean allowExport; AccessDecision(Long id,String scopes,boolean export){this.authorizationId=id;this.scopeCodes=scopes;this.allowExport=export;} }
}
