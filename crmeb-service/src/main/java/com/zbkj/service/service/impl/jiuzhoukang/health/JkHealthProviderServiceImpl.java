package com.zbkj.service.service.impl.jiuzhoukang.health;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import com.zbkj.common.model.jiuzhoukang.JkHealthProvider;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 健康厂商通用接入底座。
 *
 * <p>V3.1 当前阶段不接入任何真实厂商。通用 PULL/CALLBACK/HYBRID、REST、鉴权和解析能力保留，
 * 但只有显式设置 {@code jk.health.provider-enabled=true} 后才允许启用 Provider、拉取或接收回调。
 * 默认关闭时不扫描任务、不产生周期性失败日志，回调统一返回 provider disabled。</p>
 */
@Service
public class JkHealthProviderServiceImpl implements JkHealthProviderService {
    @Autowired private JkHealthProviderDao providerDao;
    @Autowired private JkHealthSensitiveCodec codec;
    @Autowired private HealthProviderAdapterRegistry registry;
    @Autowired private JkHealthSyncService syncService;

    @Value("${jk.health.provider-enabled:false}")
    private boolean providerEnabled;
    @Value("${jk.health.callback-enabled:false}")
    private boolean callbackEnabled;

    @Override
    public PageInfo<JkHealthProvider> list(String keyword, String syncMode, Boolean enabled, PageParamRequest pageParam) {
        Page<JkHealthProvider> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkHealthProvider> query = new LambdaQueryWrapper<JkHealthProvider>()
                .eq(JkHealthProvider::getIsDeleted, false).orderByDesc(JkHealthProvider::getId);
        if (StrUtil.isNotBlank(keyword)) {
            query.and(q -> q.like(JkHealthProvider::getProviderCode, keyword)
                    .or().like(JkHealthProvider::getProviderName, keyword));
        }
        if (StrUtil.isNotBlank(syncMode)) query.eq(JkHealthProvider::getSyncMode, syncMode);
        if (enabled != null) query.eq(JkHealthProvider::getEnabled, enabled);
        List<JkHealthProvider> rows = providerDao.selectList(query);
        rows.forEach(this::mask);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkHealthProvider save(Long operatorId, JkHealthProviderSaveRequest request) {
        String mode = request.getSyncMode().toUpperCase();
        if (!Arrays.asList("CALLBACK", "PULL", "HYBRID").contains(mode)) {
            throw new CrmebException("syncMode 只允许 CALLBACK/PULL/HYBRID");
        }
        boolean requestedEnabled = request.getEnabled() == null || request.getEnabled();
        if (requestedEnabled && !providerEnabled) throw providerDisabled();

        JkHealthProvider old = request.getId() == null ? null : providerDao.selectById(request.getId());
        JkHealthProvider duplicate = providerDao.selectOne(new LambdaQueryWrapper<JkHealthProvider>()
                .eq(JkHealthProvider::getProviderCode, request.getProviderCode())
                .eq(JkHealthProvider::getIsDeleted, false)
                .ne(request.getId() != null, JkHealthProvider::getId, request.getId()).last("limit 1"));
        if (duplicate != null) throw new CrmebException("厂商编码已存在");

        Date now = new Date();
        JkHealthProvider entity = old == null
                ? new JkHealthProvider().setCreateUserId(operatorId).setCreateTime(now)
                .setRetryCount(0).setIsDeleted(false).setVersion(0)
                : old;
        entity.setProviderCode(request.getProviderCode().trim())
                .setProviderName(request.getProviderName().trim())
                .setAdapterType(StrUtil.blankToDefault(request.getAdapterType(), "GENERIC_REST").toUpperCase())
                .setSyncMode(mode)
                .setAuthType(StrUtil.blankToDefault(request.getAuthType(), "NONE").toUpperCase())
                .setBaseUrl(request.getBaseUrl()).setCallbackPath(request.getCallbackPath())
                .setEnabled(requestedEnabled).setUpdateUserId(operatorId).setUpdateTime(now);
        if (StrUtil.isNotBlank(request.getCredentialJson())) {
            entity.setCredentialCipher(codec.encode(request.getCredentialJson()));
        }
        if (StrUtil.isNotBlank(request.getConfigJson())) {
            entity.setConfigCipher(codec.encode(request.getConfigJson()));
        }
        if (old == null) providerDao.insert(entity); else providerDao.updateById(entity);
        return mask(entity);
    }

    @Override
    public JkHealthProvider detail(Long id) {
        JkHealthProvider entity = providerDao.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) throw new CrmebException("厂商配置不存在");
        return mask(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pullOne(Long providerId, boolean resetCursor, int limit) {
        assertProviderEnabled();
        JkHealthProvider provider = providerDao.selectById(providerId);
        if (provider == null || Boolean.TRUE.equals(provider.getIsDeleted()) || !Boolean.TRUE.equals(provider.getEnabled())) {
            throw new CrmebException("厂商配置不存在或未启用");
        }
        if (!("PULL".equals(provider.getSyncMode()) || "HYBRID".equals(provider.getSyncMode()))) {
            throw new CrmebException("该厂商未启用主动拉取模式");
        }
        if (resetCursor) provider.setPullCursor(null);
        try {
            HealthProviderAdapter adapter = registry.require(provider.getAdapterType());
            List<JkHealthDeviceCallbackRequest> rows = adapter.pull(provider,
                    decode(provider.getCredentialCipher()), decode(provider.getConfigCipher()),
                    Math.max(1, Math.min(limit, 500)));
            int success = 0;
            for (JkHealthDeviceCallbackRequest row : rows) {
                syncService.receive(row);
                success++;
            }
            Date now = new Date();
            provider.setLastPullTime(now).setLastPullStatus("SUCCESS").setLastErrorMessage(null)
                    .setRetryCount(0).setNextPullTime(next(provider, decode(provider.getConfigCipher()), now))
                    .setUpdateTime(now);
            providerDao.updateById(provider);
            return success;
        } catch (Exception exception) {
            int retry = provider.getRetryCount() == null ? 1 : provider.getRetryCount() + 1;
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, Math.min(60, retry * 5));
            provider.setLastPullStatus("FAILED").setLastErrorMessage(limit(exception.getMessage(), 500))
                    .setRetryCount(retry).setNextPullTime(calendar.getTime()).setUpdateTime(new Date());
            providerDao.updateById(provider);
            if (exception instanceof CrmebException) throw (CrmebException) exception;
            throw new CrmebException("健康厂商主动拉取失败");
        }
    }

    @Override
    public int pullDue(int limit) {
        // 总开关关闭时不查询、不调度，也不制造无意义失败日志。
        if (!providerEnabled) return 0;
        List<JkHealthProvider> rows = providerDao.selectList(new LambdaQueryWrapper<JkHealthProvider>()
                .eq(JkHealthProvider::getEnabled, true)
                .eq(JkHealthProvider::getIsDeleted, false)
                .in(JkHealthProvider::getSyncMode, Arrays.asList("PULL", "HYBRID"))
                .and(q -> q.isNull(JkHealthProvider::getNextPullTime)
                        .or().le(JkHealthProvider::getNextPullTime, new Date()))
                .orderByAsc(JkHealthProvider::getId)
                .last("limit " + Math.max(1, Math.min(limit, 50))));
        int count = 0;
        for (JkHealthProvider row : rows) {
            try {
                pullOne(row.getId(), false, 200);
                count++;
            } catch (Exception ignored) {
                // 具体厂商启用后由 pullOne 记录单次失败；总开关关闭不会进入此处。
            }
        }
        return count;
    }

    @Override
    public List<JkHealthData> receiveCallback(String providerCode, String rawBody, Map<String, String> headers) {
        assertProviderEnabled();
        if (!callbackEnabled) throw providerDisabled();
        JkHealthProvider provider = findEnabled(providerCode);
        if (provider == null) throw providerDisabled();
        if (!("CALLBACK".equals(provider.getSyncMode()) || "HYBRID".equals(provider.getSyncMode()))) {
            throw new CrmebException("该厂商未启用回调模式");
        }
        HealthProviderAdapter adapter = registry.require(provider.getAdapterType());
        List<JkHealthDeviceCallbackRequest> rows = adapter.parseCallback(provider,
                decode(provider.getCredentialCipher()), decode(provider.getConfigCipher()), rawBody, headers);
        List<JkHealthData> result = new ArrayList<JkHealthData>();
        for (JkHealthDeviceCallbackRequest row : rows) result.add(syncService.receive(row));
        return result;
    }

    @Override
    public String callbackSecret(String providerCode) {
        assertProviderEnabled();
        if (!callbackEnabled) throw providerDisabled();
        JkHealthProvider provider = findEnabled(providerCode);
        if (provider == null) throw providerDisabled();
        if (!("CALLBACK".equals(provider.getSyncMode()) || "HYBRID".equals(provider.getSyncMode()))) {
            throw new CrmebException("该厂商未启用回调模式");
        }
        try {
            com.alibaba.fastjson.JSONObject credential = com.alibaba.fastjson.JSON.parseObject(
                    decode(provider.getCredentialCipher()));
            return credential.getString("callbackSecret");
        } catch (Exception exception) {
            throw new CrmebException("厂商回调密钥配置无效");
        }
    }

    @Override
    public JkHealthProvider findEnabled(String providerCode) {
        if (!providerEnabled) return null;
        return providerDao.selectOne(new LambdaQueryWrapper<JkHealthProvider>()
                .eq(JkHealthProvider::getProviderCode, providerCode)
                .eq(JkHealthProvider::getEnabled, true)
                .eq(JkHealthProvider::getIsDeleted, false).last("limit 1"));
    }

    private void assertProviderEnabled() {
        if (!providerEnabled) throw providerDisabled();
    }

    private CrmebException providerDisabled() {
        return new CrmebException("provider disabled");
    }

    private Date next(JkHealthProvider provider, String configJson, Date now) {
        int minutes = 5;
        try {
            com.alibaba.fastjson.JSONObject config = com.alibaba.fastjson.JSON.parseObject(configJson);
            if (config != null && config.getIntValue("pullIntervalMinutes") > 0) {
                minutes = Math.max(1, config.getIntValue("pullIntervalMinutes"));
            }
        } catch (Exception ignored) {
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    private String decode(String cipher) {
        return StrUtil.isBlank(cipher) ? "{}" : codec.decode(cipher);
    }

    private JkHealthProvider mask(JkHealthProvider entity) {
        entity.setCredentialConfigured(StrUtil.isNotBlank(entity.getCredentialCipher()))
                .setCallbackSupported(providerEnabled && ("CALLBACK".equals(entity.getSyncMode()) || "HYBRID".equals(entity.getSyncMode())))
                .setPullSupported(providerEnabled && ("PULL".equals(entity.getSyncMode()) || "HYBRID".equals(entity.getSyncMode())))
                .setStatusText(!providerEnabled ? "全局未开放" : (Boolean.TRUE.equals(entity.getEnabled()) ? "启用" : "停用"))
                .setCredentialCipher(null).setConfigCipher(null);
        return entity;
    }

    private String limit(String value, int max) {
        if (value == null) return "未知错误";
        value = value.replace('\n', ' ').replace('\r', ' ');
        return value.length() > max ? value.substring(0, max) : value;
    }
}
