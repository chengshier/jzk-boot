package com.zbkj.service.service.impl.jiuzhoukang.promotion;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkPromotionCodeCache;
import com.zbkj.common.model.jiuzhoukang.JkPromotionScene;
import com.zbkj.common.response.jiuzhoukang.JkFileObjectResponse;
import com.zbkj.common.response.jiuzhoukang.JkPromotionCodeResponse;
import com.zbkj.service.dao.jiuzhoukang.JkPromotionCodeCacheDao;
import com.zbkj.service.dao.jiuzhoukang.JkPromotionSceneDao;
import com.zbkj.service.service.jiuzhoukang.promotion.JkPromotionCodeService;
import com.zbkj.service.service.jiuzhoukang.storage.JkFileStorageService;
import com.zbkj.service.service.jiuzhoukang.wechat.JkWechatAccessTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 真实微信小程序码生成服务；配置缺失时明确不可用，不生成占位图。 */
@Service
public class JkPromotionCodeServiceImpl implements JkPromotionCodeService {
    @Autowired private JkPromotionSceneDao sceneDao;
    @Autowired private JkPromotionCodeCacheDao cacheDao;
    @Autowired private JkWechatAccessTokenService tokenService;
    @Autowired private JkFileStorageService storageService;

    @Override
    public List<JkPromotionScene> scenes() {
        return sceneDao.selectList(new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getIsDeleted, false).orderByDesc(JkPromotionScene::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionScene saveScene(JkPromotionScene request) {
        if (request == null || StrUtil.isBlank(request.getSceneCode()) || StrUtil.isBlank(request.getSceneName())
                || StrUtil.isBlank(request.getPagePath()) || StrUtil.isBlank(request.getSceneTemplate())) {
            throw new CrmebException("推广场景编码、名称、页面和scene模板不能为空");
        }
        Date now = new Date();
        if (request.getId() == null) {
            request.setStatus(Boolean.TRUE.equals(request.getStatus())).setIsDeleted(false)
                    .setCreateTime(now).setUpdateTime(now);
            sceneDao.insert(request);
        } else {
            JkPromotionScene old = sceneDao.selectById(request.getId());
            if (old == null || Boolean.TRUE.equals(old.getIsDeleted())) throw new CrmebException("推广场景不存在");
            old.setSceneName(request.getSceneName()).setPagePath(request.getPagePath())
                    .setSceneTemplate(request.getSceneTemplate()).setExpireDays(request.getExpireDays())
                    .setStatus(Boolean.TRUE.equals(request.getStatus())).setUpdateTime(now);
            sceneDao.updateById(old);
            request = old;
        }
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionCodeResponse generate(Long ownerUserId, String sceneCode, String requestNo) {
        if (ownerUserId == null || ownerUserId <= 0) throw new CrmebException("推广用户不能为空");
        if (StrUtil.isBlank(sceneCode)) throw new CrmebException("推广场景不能为空");
        if (StrUtil.isBlank(requestNo)) throw new CrmebException("requestNo不能为空");
        JkPromotionScene scene = sceneDao.selectOne(new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getSceneCode, sceneCode).eq(JkPromotionScene::getStatus, true)
                .eq(JkPromotionScene::getIsDeleted, false).last("limit 1"));
        if (scene == null) throw new CrmebException("推广场景未启用");
        String sceneValue = buildSceneValue(scene, ownerUserId);
        JkPromotionCodeCache old = cacheDao.selectOne(new LambdaQueryWrapper<JkPromotionCodeCache>()
                .eq(JkPromotionCodeCache::getSceneId, scene.getId()).eq(JkPromotionCodeCache::getOwnerUserId, ownerUserId)
                .eq(JkPromotionCodeCache::getSceneValue, sceneValue).eq(JkPromotionCodeCache::getStatus, "SUCCESS")
                .eq(JkPromotionCodeCache::getIsDeleted, false).orderByDesc(JkPromotionCodeCache::getId).last("limit 1"));
        if (old != null && old.getFileObjectId() != null) return response(scene, old);
        JkPromotionCodeCache requestCache = cacheDao.selectOne(new LambdaQueryWrapper<JkPromotionCodeCache>()
                .eq(JkPromotionCodeCache::getRequestNo, requestNo).last("limit 1"));
        if (requestCache != null) return response(scene, requestCache);
        Date now = new Date();
        JkPromotionCodeCache cache = new JkPromotionCodeCache().setSceneId(scene.getId()).setOwnerUserId(ownerUserId)
                .setSceneValue(sceneValue).setStatus("GENERATING").setRequestNo(requestNo)
                .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        try {
            cacheDao.insert(cache);
        } catch (DuplicateKeyException duplicate) {
            JkPromotionCodeCache duplicateValue = cacheDao.selectOne(new LambdaQueryWrapper<JkPromotionCodeCache>()
                    .eq(JkPromotionCodeCache::getRequestNo, requestNo).last("limit 1"));
            if (duplicateValue != null) return response(scene, duplicateValue);
            throw new CrmebException("推广码正在生成，请勿重复提交");
        }
        try {
            byte[] png = requestWechatCode(sceneValue, scene.getPagePath());
            JkFileObjectResponse file = storageService.storeBytes(png, sceneCode + "-" + ownerUserId + ".png", "image/png",
                    "PROMOTION_CODE", cache.getId(), ownerUserId, "PRIVATE");
            cache.setFileObjectId(file.getId()).setStatus("SUCCESS").setGeneratedAt(new Date()).setUpdateTime(new Date());
            cacheDao.updateById(cache);
            return response(scene, cache);
        } catch (Exception error) {
            cache.setStatus("FAILED").setErrorMessage(safe(error.getMessage())).setUpdateTime(new Date());
            cacheDao.updateById(cache);
            if (error instanceof CrmebException) throw (CrmebException) error;
            throw new CrmebException("生成真实微信小程序码失败：" + safe(error.getMessage()));
        }
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("wechat", tokenService.status());
        result.put("storage", storageService.status());
        Integer enabledSceneCount = sceneDao.selectCount(new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getStatus, true).eq(JkPromotionScene::getIsDeleted, false));
        result.put("enabledSceneCount", enabledSceneCount == null ? 0 : enabledSceneCount);
        result.put("ready", Boolean.TRUE.equals(tokenService.status().get("ready"))
                && Boolean.TRUE.equals(storageService.status().get("ready")) && enabledSceneCount != null && enabledSceneCount > 0);
        return result;
    }

    private byte[] requestWechatCode(String scene, String page) {
        String token = tokenService.token();
        JSONObject body = new JSONObject();
        body.put("scene", scene);
        body.put("page", page);
        body.put("check_path", false);
        body.put("env_version", "release");
        HttpResponse response = HttpRequest.post("https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + token)
                .header("Content-Type", "application/json").body(body.toString()).timeout(20000).execute();
        byte[] bytes = response.bodyBytes();
        if (bytes == null || bytes.length == 0) throw new CrmebException("微信返回空的小程序码");
        String contentType = response.header("Content-Type");
        if ((contentType != null && contentType.contains("json")) || bytes[0] == '{') {
            JSONObject error = JSONUtil.parseObj(new String(bytes, StandardCharsets.UTF_8));
            throw new CrmebException("微信小程序码接口失败：" + safe(error.getStr("errmsg")) + "（" + error.getInt("errcode", -1) + "）");
        }
        if (bytes.length < 8 || bytes[0] != (byte) 0x89 || bytes[1] != 0x50) throw new CrmebException("微信未返回有效 PNG 小程序码");
        return bytes;
    }

    private JkPromotionCodeResponse response(JkPromotionScene scene, JkPromotionCodeCache cache) {
        return new JkPromotionCodeResponse().setCacheId(cache.getId()).setSceneCode(scene.getSceneCode())
                .setSceneName(scene.getSceneName()).setSceneValue(cache.getSceneValue()).setPagePath(scene.getPagePath())
                .setFileObjectId(cache.getFileObjectId()).setDownloadPath(cache.getFileObjectId() == null ? null : "/api/front/jk/file/" + cache.getFileObjectId() + "/download")
                .setStatus(cache.getStatus()).setErrorMessage(cache.getErrorMessage());
    }

    private String buildSceneValue(JkPromotionScene scene, Long ownerUserId) {
        String value = scene.getSceneTemplate().replace("{userId}", String.valueOf(ownerUserId));
        if (value.length() > 32) throw new CrmebException("微信小程序码scene长度不能超过32字符");
        return value;
    }

    private String safe(String value) { return value == null ? "" : value; }
}
