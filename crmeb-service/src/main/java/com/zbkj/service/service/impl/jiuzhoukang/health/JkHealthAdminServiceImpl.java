package com.zbkj.service.service.impl.jiuzhoukang.health;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkHealthIntegrationStatusResponse;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** 第五阶段后台管理服务。设备绑定码只在保存时接收，后续不回显明文。 */
@Service
public class JkHealthAdminServiceImpl implements JkHealthAdminService {
    @Autowired private JkHealthDeviceDao deviceDao;
    @Autowired private JkHealthDeviceBindDao bindDao;
    @Autowired private JkHealthAuthorizationDao authorizationDao;
    @Autowired private JkHealthAlertRuleDao ruleDao;
    @Autowired private JkHealthAlertRecordDao alertDao;
    @Autowired private JkHealthAccessLogDao accessLogDao;
    @Autowired private UserService userService;
    @Value("${jk.health.callback-enabled:false}") private boolean callbackEnabled;
    @Value("${jk.health.callback-secret:}") private String callbackSecret;
    @Value("${jk.health.data-encryption-key:}") private String encryptionKey;
    @Value("${jk.health.allow-plaintext:false}") private boolean allowPlaintext;
    @Value("${jk.health.sync-auto-retry-enabled:false}") private boolean syncAutoRetryEnabled;
    @Value("${jk.health.retention-days:0}") private int retentionDays;
    @Value("${jk.health.archive-enabled:false}") private boolean archiveEnabled;
    @Value("${jk.health.sinocare.app-id:}") private String sinocareAppId;
    @Value("${jk.health.sinocare.authorization-h5-url:}") private String sinocareAuthorizationH5Url;
    @Value("${jk.health.sinocare.public-key:}") private String sinocarePublicKey;


    @Override
    public PageInfo<JkHealthDevice> listDevices(String keyword, String status, PageParamRequest p) {
        Page<JkHealthDevice> page = PageHelper.startPage(p.getPage(), p.getLimit());
        LambdaQueryWrapper<JkHealthDevice> q = new LambdaQueryWrapper<JkHealthDevice>().eq(JkHealthDevice::getIsDeleted, false).orderByDesc(JkHealthDevice::getId);
        if (StrUtil.isNotBlank(keyword)) q.and(w -> w.like(JkHealthDevice::getDeviceSn, keyword).or().like(JkHealthDevice::getDeviceModel, keyword));
        if (StrUtil.isNotBlank(status)) q.eq(JkHealthDevice::getStatus, status);
        List<JkHealthDevice> rows = deviceDao.selectList(q);
        for (JkHealthDevice d : rows) {
            JkHealthDeviceBind bind = bindDao.selectOne(new LambdaQueryWrapper<JkHealthDeviceBind>()
                    .eq(JkHealthDeviceBind::getDeviceId, d.getId()).eq(JkHealthDeviceBind::getStatus, "ACTIVE")
                    .eq(JkHealthDeviceBind::getIsDeleted, false).last("limit 1"));
            if (bind != null) { d.setBoundUserId(bind.getUserId()); User u=userService.getById(bind.getUserId().intValue()); d.setBoundUserName(name(u)); }
        }
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthDevice saveDevice(Long adminUserId, JkHealthDeviceSaveRequest request) {
        JkHealthDevice duplicate = deviceDao.selectOne(new LambdaQueryWrapper<JkHealthDevice>()
                .eq(JkHealthDevice::getDeviceSn, request.getDeviceSn()).eq(JkHealthDevice::getIsDeleted, false)
                .ne(request.getId()!=null, JkHealthDevice::getId, request.getId()).last("limit 1"));
        if (duplicate != null) throw new CrmebException("设备编号已存在");
        Date now = new Date(); JkHealthDevice entity = request.getId()==null ? null : deviceDao.selectById(request.getId());
        if (entity == null) entity = new JkHealthDevice().setIsDeleted(false).setCreateUserId(adminUserId).setCreateTime(now).setVersion(0);
        entity.setDeviceSn(request.getDeviceSn()).setProviderCode(request.getProviderCode()).setExternalDeviceId(request.getExternalDeviceId()).setDeviceType(request.getDeviceType())
                .setDeviceModel(request.getDeviceModel()).setStatus(StrUtil.isBlank(request.getStatus())?"ENABLED":request.getStatus())
                .setUpdateUserId(adminUserId).setUpdateTime(now);
        if (StrUtil.isNotBlank(request.getBindCode())) entity.setBindCodeHash(hash(request.getBindCode()));
        if (entity.getId()==null && StrUtil.isBlank(entity.getBindCodeHash())) throw new CrmebException("新增设备必须设置绑定码");
        if (entity.getId()==null) deviceDao.insert(entity); else deviceDao.updateById(entity);
        entity.setBindCodeHash(null);
        return entity;
    }

    @Override
    public PageInfo<JkHealthDeviceBind> listDeviceBinds(Long userId, String deviceSn, String status, PageParamRequest p) {
        Page<JkHealthDeviceBind> page = PageHelper.startPage(p.getPage(), p.getLimit());
        LambdaQueryWrapper<JkHealthDeviceBind> q = new LambdaQueryWrapper<JkHealthDeviceBind>()
                .eq(JkHealthDeviceBind::getIsDeleted, false).orderByDesc(JkHealthDeviceBind::getId);
        if (userId != null) q.eq(JkHealthDeviceBind::getUserId, userId);
        if (StrUtil.isNotBlank(status)) q.eq(JkHealthDeviceBind::getStatus, status);
        if (StrUtil.isNotBlank(deviceSn)) {
            List<JkHealthDevice> devices = deviceDao.selectList(new LambdaQueryWrapper<JkHealthDevice>()
                    .like(JkHealthDevice::getDeviceSn, deviceSn).eq(JkHealthDevice::getIsDeleted, false));
            List<Long> ids = new ArrayList<>(); for (JkHealthDevice d : devices) ids.add(d.getId());
            if (ids.isEmpty()) return CommonPage.copyPageInfo(page, Collections.emptyList());
            q.in(JkHealthDeviceBind::getDeviceId, ids);
        }
        List<JkHealthDeviceBind> rows = bindDao.selectList(q);
        for (JkHealthDeviceBind b : rows) {
            JkHealthDevice d = deviceDao.selectById(b.getDeviceId());
            User u = userService.getById(b.getUserId().intValue());
            if (d != null) b.setDeviceSn(d.getDeviceSn()).setDeviceType(d.getDeviceType()).setDeviceModel(d.getDeviceModel());
            b.setUserName(name(u));
        }
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public PageInfo<JkHealthAuthorization> listAuthorizations(Long ownerId, Long granteeId, String status, PageParamRequest p) {
        Page<JkHealthAuthorization> page=PageHelper.startPage(p.getPage(),p.getLimit());
        LambdaQueryWrapper<JkHealthAuthorization> q=new LambdaQueryWrapper<JkHealthAuthorization>().eq(JkHealthAuthorization::getIsDeleted,false).orderByDesc(JkHealthAuthorization::getId);
        if(ownerId!=null)q.eq(JkHealthAuthorization::getOwnerUserId,ownerId); if(granteeId!=null)q.eq(JkHealthAuthorization::getGranteeUserId,granteeId); if(StrUtil.isNotBlank(status))q.eq(JkHealthAuthorization::getStatus,status);
        List<JkHealthAuthorization> rows=authorizationDao.selectList(q); for(JkHealthAuthorization a:rows){a.setOwnerName(name(userService.getById(a.getOwnerUserId().intValue())));a.setGranteeName(name(userService.getById(a.getGranteeUserId().intValue())));}
        return CommonPage.copyPageInfo(page,rows);
    }

    @Override
    public PageInfo<JkHealthAlertRule> listRules(String dataType, Boolean enabled, PageParamRequest p) {
        Page<JkHealthAlertRule> page=PageHelper.startPage(p.getPage(),p.getLimit()); LambdaQueryWrapper<JkHealthAlertRule> q=new LambdaQueryWrapper<JkHealthAlertRule>().eq(JkHealthAlertRule::getIsDeleted,false).orderByDesc(JkHealthAlertRule::getId);
        if(StrUtil.isNotBlank(dataType))q.eq(JkHealthAlertRule::getDataType,dataType); if(enabled!=null)q.eq(JkHealthAlertRule::getEnabled,enabled);
        return CommonPage.copyPageInfo(page,ruleDao.selectList(q));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthAlertRule saveRule(Long adminUserId, JkHealthAlertRuleSaveRequest request) {
        if(request.getMinValue()!=null&&request.getMaxValue()!=null&&request.getMinValue().compareTo(request.getMaxValue())>=0)throw new CrmebException("预警下限必须小于上限");
        if(request.getMinValue()==null&&request.getMaxValue()==null)throw new CrmebException("至少配置一个预警边界");
        Date now=new Date(); JkHealthAlertRule entity=request.getId()==null?null:ruleDao.selectById(request.getId()); if(entity==null)entity=new JkHealthAlertRule().setIsDeleted(false).setCreateUserId(adminUserId).setCreateTime(now).setVersion(0);
        entity.setRuleName(request.getRuleName()).setOwnerUserId(request.getOwnerUserId()).setDataType(request.getDataType().toUpperCase(Locale.ROOT)).setPeriodCode(request.getPeriodCode())
                .setMinValue(request.getMinValue()).setMaxValue(request.getMaxValue()).setAlertLevel(request.getAlertLevel()).setEnabled(request.getEnabled()==null?true:request.getEnabled()).setUpdateUserId(adminUserId).setUpdateTime(now);
        if(entity.getId()==null)ruleDao.insert(entity);else ruleDao.updateById(entity);return entity;
    }

    @Override
    public PageInfo<JkHealthAlertRecord> listAlerts(Long userId,String status,PageParamRequest p){Page<JkHealthAlertRecord> page=PageHelper.startPage(p.getPage(),p.getLimit());LambdaQueryWrapper<JkHealthAlertRecord> q=new LambdaQueryWrapper<JkHealthAlertRecord>().eq(JkHealthAlertRecord::getIsDeleted,false).orderByDesc(JkHealthAlertRecord::getId);if(userId!=null)q.eq(JkHealthAlertRecord::getUserId,userId);if(StrUtil.isNotBlank(status))q.eq(JkHealthAlertRecord::getStatus,status);List<JkHealthAlertRecord> rows=alertDao.selectList(q);for(JkHealthAlertRecord a:rows)a.setUserName(name(userService.getById(a.getUserId().intValue())));return CommonPage.copyPageInfo(page,rows);}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthAlertRecord processAlert(Long adminUserId,JkHealthAlertProcessRequest request){String target="CLOSE".equalsIgnoreCase(request.getAction())?"CLOSED":"ACKNOWLEDGED";int n=alertDao.update(null,new UpdateWrapper<JkHealthAlertRecord>().eq("id",request.getAlertId()).in("status",Arrays.asList("OPEN","ACKNOWLEDGED")).set("status",target).set("process_user_id",adminUserId).set("process_time",new Date()).set("process_remark",request.getRemark()).set("update_time",new Date()));if(n!=1)throw new CrmebException("预警不存在或已关闭");return alertDao.selectById(request.getAlertId());}

    @Override
    public PageInfo<JkHealthAccessLog> listAccessLogs(Long ownerId,Long viewerId,String result,PageParamRequest p){Page<JkHealthAccessLog> page=PageHelper.startPage(p.getPage(),p.getLimit());LambdaQueryWrapper<JkHealthAccessLog> q=new LambdaQueryWrapper<JkHealthAccessLog>().eq(JkHealthAccessLog::getIsDeleted,false).orderByDesc(JkHealthAccessLog::getId);if(ownerId!=null)q.eq(JkHealthAccessLog::getOwnerUserId,ownerId);if(viewerId!=null)q.eq(JkHealthAccessLog::getViewerUserId,viewerId);if(StrUtil.isNotBlank(result))q.eq(JkHealthAccessLog::getAccessResult,result);return CommonPage.copyPageInfo(page,accessLogDao.selectList(q));}

    @Override
    public JkHealthIntegrationStatusResponse integrationStatus() {
        JkHealthIntegrationStatusResponse r = new JkHealthIntegrationStatusResponse();
        r.setCallbackEnabled(callbackEnabled);
        r.setCallbackSecretConfigured(StrUtil.isNotBlank(callbackSecret));
        r.setEncryptionKeyConfigured(StrUtil.isNotBlank(encryptionKey));
        r.setPlaintextAllowed(allowPlaintext);
        r.setSyncAutoRetryEnabled(syncAutoRetryEnabled);
        r.setRetentionDays(retentionDays);
        r.setArchiveEnabled(archiveEnabled);
        r.setSinocareAppIdConfigured(StrUtil.isNotBlank(sinocareAppId));
        r.setSinocareAuthorizationH5UrlConfigured(StrUtil.isNotBlank(sinocareAuthorizationH5Url));
        r.setSinocarePublicKeyConfigured(StrUtil.isNotBlank(sinocarePublicKey));
        r.setCallbackPath("/api/front/jk/health/device/callback");
        r.setSecurityTip("生产环境必须配置回调密钥和健康数据加密密钥；本页永不回显密钥明文。");
        return r;
    }

    private String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();for(byte b:bytes)sb.append(String.format("%02x",b&0xff));return sb.toString();}catch(Exception e){throw new CrmebException("绑定码处理失败");}}
    private String name(User u){if(u==null)return null;return StrUtil.isNotBlank(u.getRealName())?u.getRealName():u.getNickname();}
}
