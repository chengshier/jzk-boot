package com.zbkj.service.service.impl.jiuzhoukang.health;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.*;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkHealthDeviceCallbackRequest;
import com.zbkj.common.request.jiuzhoukang.JkHealthProviderSaveRequest;
import com.zbkj.service.dao.jiuzhoukang.JkHealthProviderDao;
import com.zbkj.service.service.impl.jiuzhoukang.health.provider.HealthProviderAdapterRegistry;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthProviderService;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthSyncService;
import com.zbkj.service.service.jiuzhoukang.health.provider.HealthProviderAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 健康厂商配置和主动拉取调度。密钥仅在服务内部解密，接口永不回显。 */
@Service
public class JkHealthProviderServiceImpl implements JkHealthProviderService {
    @Autowired private JkHealthProviderDao providerDao;
    @Autowired private JkHealthSensitiveCodec codec;
    @Autowired private HealthProviderAdapterRegistry registry;
    @Autowired private JkHealthSyncService syncService;
    @Value("${jk.health.callback-enabled:false}") private boolean callbackEnabled;

    @Override public PageInfo<JkHealthProvider> list(String keyword, String syncMode, Boolean enabled, PageParamRequest pageParam) {
        Page<JkHealthProvider> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkHealthProvider> q = new LambdaQueryWrapper<JkHealthProvider>().eq(JkHealthProvider::getIsDeleted,false).orderByDesc(JkHealthProvider::getId);
        if (StrUtil.isNotBlank(keyword)) q.and(w -> w.like(JkHealthProvider::getProviderCode,keyword).or().like(JkHealthProvider::getProviderName,keyword));
        if (StrUtil.isNotBlank(syncMode)) q.eq(JkHealthProvider::getSyncMode,syncMode);
        if (enabled != null) q.eq(JkHealthProvider::getEnabled,enabled);
        List<JkHealthProvider> rows=providerDao.selectList(q); rows.forEach(this::mask);
        return CommonPage.copyPageInfo(page,rows);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public JkHealthProvider save(Long operatorId, JkHealthProviderSaveRequest r) {
        String mode=r.getSyncMode().toUpperCase();
        if (!Arrays.asList("CALLBACK","PULL","HYBRID").contains(mode)) throw new CrmebException("syncMode 只允许 CALLBACK/PULL/HYBRID");
        JkHealthProvider old=r.getId()==null?null:providerDao.selectById(r.getId());
        JkHealthProvider duplicate=providerDao.selectOne(new LambdaQueryWrapper<JkHealthProvider>().eq(JkHealthProvider::getProviderCode,r.getProviderCode()).eq(JkHealthProvider::getIsDeleted,false).ne(r.getId()!=null,JkHealthProvider::getId,r.getId()).last("limit 1"));
        if(duplicate!=null)throw new CrmebException("厂商编码已存在");
        Date now=new Date(); JkHealthProvider e=old==null?new JkHealthProvider().setCreateUserId(operatorId).setCreateTime(now).setRetryCount(0).setIsDeleted(false).setVersion(0):old;
        e.setProviderCode(r.getProviderCode().trim()).setProviderName(r.getProviderName().trim()).setAdapterType(StrUtil.blankToDefault(r.getAdapterType(),"GENERIC_REST").toUpperCase())
                .setSyncMode(mode).setAuthType(StrUtil.blankToDefault(r.getAuthType(),"NONE").toUpperCase()).setBaseUrl(r.getBaseUrl()).setCallbackPath(r.getCallbackPath())
                .setEnabled(r.getEnabled()==null||r.getEnabled()).setUpdateUserId(operatorId).setUpdateTime(now);
        if(StrUtil.isNotBlank(r.getCredentialJson())) e.setCredentialCipher(codec.encode(r.getCredentialJson()));
        if(StrUtil.isNotBlank(r.getConfigJson())) e.setConfigCipher(codec.encode(r.getConfigJson()));
        if(old==null)providerDao.insert(e);else providerDao.updateById(e);return mask(e);
    }

    @Override public JkHealthProvider detail(Long id){JkHealthProvider e=providerDao.selectById(id);if(e==null||Boolean.TRUE.equals(e.getIsDeleted()))throw new CrmebException("厂商配置不存在");return mask(e);}

    @Override @Transactional(rollbackFor=Exception.class)
    public int pullOne(Long providerId, boolean resetCursor, int limit) {
        JkHealthProvider provider=providerDao.selectById(providerId);
        if(provider==null||Boolean.TRUE.equals(provider.getIsDeleted())||!Boolean.TRUE.equals(provider.getEnabled()))throw new CrmebException("厂商配置不存在或未启用");
        if(!("PULL".equals(provider.getSyncMode())||"HYBRID".equals(provider.getSyncMode())))throw new CrmebException("该厂商未启用主动拉取模式");
        if(resetCursor)provider.setPullCursor(null);
        try{
            HealthProviderAdapter adapter=registry.require(provider.getAdapterType());
            List<JkHealthDeviceCallbackRequest> rows=adapter.pull(provider,decode(provider.getCredentialCipher()),decode(provider.getConfigCipher()),Math.max(1,Math.min(limit,500)));
            int success=0;for(JkHealthDeviceCallbackRequest row:rows){syncService.receive(row);success++;}
            Date now=new Date();provider.setLastPullTime(now).setLastPullStatus("SUCCESS").setLastErrorMessage(null).setRetryCount(0).setNextPullTime(next(provider,decode(provider.getConfigCipher()),now)).setUpdateTime(now);providerDao.updateById(provider);return success;
        }catch(Exception ex){int retry=provider.getRetryCount()==null?1:provider.getRetryCount()+1;Calendar c=Calendar.getInstance();c.add(Calendar.MINUTE,Math.min(60,retry*5));provider.setLastPullStatus("FAILED").setLastErrorMessage(limit(ex.getMessage(),500)).setRetryCount(retry).setNextPullTime(c.getTime()).setUpdateTime(new Date());providerDao.updateById(provider);if(ex instanceof CrmebException)throw (CrmebException)ex;throw new CrmebException("健康厂商主动拉取失败");}
    }

    @Override public int pullDue(int limit){List<JkHealthProvider> rows=providerDao.selectList(new LambdaQueryWrapper<JkHealthProvider>().eq(JkHealthProvider::getEnabled,true).eq(JkHealthProvider::getIsDeleted,false).in(JkHealthProvider::getSyncMode,Arrays.asList("PULL","HYBRID")).and(w->w.isNull(JkHealthProvider::getNextPullTime).or().le(JkHealthProvider::getNextPullTime,new Date())).orderByAsc(JkHealthProvider::getId).last("limit "+Math.max(1,Math.min(limit,50))));int count=0;for(JkHealthProvider row:rows){try{pullOne(row.getId(),false,200);count++;}catch(Exception ignored){}}return count;}

    @Override
    public List<JkHealthData> receiveCallback(String providerCode, String rawBody, Map<String,String> headers) {
        if (!callbackEnabled) throw new CrmebException("健康设备厂商回调尚未启用");
        JkHealthProvider provider = findEnabled(providerCode);
        if (provider == null) throw new CrmebException("健康厂商不存在或未启用");
        if (!("CALLBACK".equals(provider.getSyncMode()) || "HYBRID".equals(provider.getSyncMode())))
            throw new CrmebException("该厂商未启用回调模式");
        HealthProviderAdapter adapter = registry.require(provider.getAdapterType());
        List<JkHealthDeviceCallbackRequest> rows = adapter.parseCallback(provider, decode(provider.getCredentialCipher()),
                decode(provider.getConfigCipher()), rawBody, headers);
        List<JkHealthData> result = new ArrayList<JkHealthData>();
        for (JkHealthDeviceCallbackRequest row : rows) result.add(syncService.receive(row));
        return result;
    }

    @Override public String callbackSecret(String providerCode){JkHealthProvider p=findEnabled(providerCode);if(p==null)return null;if(!("CALLBACK".equals(p.getSyncMode())||"HYBRID".equals(p.getSyncMode())))throw new CrmebException("该厂商未启用回调模式");try{com.alibaba.fastjson.JSONObject c=com.alibaba.fastjson.JSON.parseObject(decode(p.getCredentialCipher()));return c.getString("callbackSecret");}catch(Exception e){throw new CrmebException("厂商回调密钥配置无效");}}
    @Override public JkHealthProvider findEnabled(String providerCode){return providerDao.selectOne(new LambdaQueryWrapper<JkHealthProvider>().eq(JkHealthProvider::getProviderCode,providerCode).eq(JkHealthProvider::getEnabled,true).eq(JkHealthProvider::getIsDeleted,false).last("limit 1"));}

    private Date next(JkHealthProvider p,String configJson,Date now){int minutes=5;try{com.alibaba.fastjson.JSONObject c=com.alibaba.fastjson.JSON.parseObject(configJson);if(c!=null&&c.getIntValue("pullIntervalMinutes")>0)minutes=Math.max(1,c.getIntValue("pullIntervalMinutes"));}catch(Exception ignored){}Calendar cal=Calendar.getInstance();cal.setTime(now);cal.add(Calendar.MINUTE,minutes);return cal.getTime();}
    private String decode(String cipher){return StrUtil.isBlank(cipher)?"{}":codec.decode(cipher);}
    private JkHealthProvider mask(JkHealthProvider e){e.setCredentialConfigured(StrUtil.isNotBlank(e.getCredentialCipher())).setCallbackSupported("CALLBACK".equals(e.getSyncMode())||"HYBRID".equals(e.getSyncMode())).setPullSupported("PULL".equals(e.getSyncMode())||"HYBRID".equals(e.getSyncMode())).setStatusText(Boolean.TRUE.equals(e.getEnabled())?"启用":"停用").setCredentialCipher(null).setConfigCipher(null);return e;}
    private String limit(String s,int max){if(s==null)return "未知错误";s=s.replace('\n',' ').replace('\r',' ');return s.length()>max?s.substring(0,max):s;}
}
