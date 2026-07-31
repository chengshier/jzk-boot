package com.zbkj.service.service.impl.jiuzhoukang.promotion;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkPromotionCodeCache;
import com.zbkj.common.model.jiuzhoukang.JkPromotionScene;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkPromotionSceneSaveRequest;
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
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 真实微信小程序码生成；配置不完整时明确失败，绝不返回占位二维码。 */
@Service
public class JkPromotionCodeServiceImpl implements JkPromotionCodeService {
    @Autowired private JkPromotionSceneDao sceneDao;
    @Autowired private JkPromotionCodeCacheDao cacheDao;
    @Autowired private JkWechatAccessTokenService tokenService;
    @Autowired private JkFileStorageService storageService;

    @Override
    public PageInfo<JkPromotionScene> listScenes(String keyword, Boolean status, PageParamRequest pageParam) {
        Page<JkPromotionScene> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkPromotionScene> query = new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getIsDeleted, false).orderByAsc(JkPromotionScene::getSceneCode)
                .orderByDesc(JkPromotionScene::getVersionNo);
        if (StrUtil.isNotBlank(keyword)) {
            query.and(q -> q.like(JkPromotionScene::getSceneCode, keyword)
                    .or().like(JkPromotionScene::getSceneName, keyword));
        }
        if (status != null) query.eq(JkPromotionScene::getStatus, status);
        return CommonPage.copyPageInfo(page, sceneDao.selectList(query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionScene saveScene(JkPromotionSceneSaveRequest request, Long operatorId) {
        if (request.getVersionNo() == null || request.getVersionNo() < 1) throw new CrmebException("版本号必须大于0");
        if (request.getSceneTemplate().getBytes(StandardCharsets.UTF_8).length > 32) {
            throw new CrmebException("scene 模板最长32字节");
        }
        JkPromotionScene duplicate = sceneDao.selectOne(new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getSceneCode, request.getSceneCode())
                .eq(JkPromotionScene::getVersionNo, request.getVersionNo())
                .eq(JkPromotionScene::getIsDeleted, false)
                .ne(request.getId() != null, JkPromotionScene::getId, request.getId()).last("limit 1"));
        if (duplicate != null) throw new CrmebException("相同场景编码和版本已存在");
        Date now = new Date();
        JkPromotionScene value = request.getId() == null
                ? new JkPromotionScene().setCreateTime(now).setIsDeleted(false)
                : sceneDao.selectById(request.getId());
        if (value == null || Boolean.TRUE.equals(value.getIsDeleted())) throw new CrmebException("推广场景不存在");
        value.setSceneCode(request.getSceneCode().trim()).setSceneName(request.getSceneName().trim())
                .setPagePath(request.getPagePath().trim()).setRoleCodes(request.getRoleCodes())
                .setSceneTemplate(request.getSceneTemplate().trim()).setVersionNo(request.getVersionNo())
                .setStatus(Boolean.TRUE.equals(request.getStatus())).setRemark(request.getRemark()).setUpdateTime(now);
        if (value.getId() == null) sceneDao.insert(value); else sceneDao.updateById(value);
        return value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionCodeResponse generate(Long ownerUserId, String ownerRoleCode, String sceneCode, String requestNo) {
        if (ownerUserId == null || ownerUserId <= 0) throw new CrmebException("推广用户不能为空");
        if (StrUtil.isBlank(sceneCode)) throw new CrmebException("推广场景不能为空");
        if (StrUtil.isBlank(requestNo)) throw new CrmebException("requestNo不能为空");
        JkPromotionScene scene = sceneDao.selectOne(new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getSceneCode, sceneCode).eq(JkPromotionScene::getStatus, true)
                .eq(JkPromotionScene::getIsDeleted, false).orderByDesc(JkPromotionScene::getVersionNo).last("limit 1"));
        if (scene == null) throw new CrmebException("推广场景未启用");
        assertRole(scene.getRoleCodes(), ownerRoleCode);
        if (!Boolean.TRUE.equals(storageService.status().get("ready"))) throw new CrmebException("统一文件存储尚未就绪");
        String sceneValue = scene.getSceneTemplate().replace("{userId}", String.valueOf(ownerUserId));
        if (sceneValue.getBytes(StandardCharsets.UTF_8).length > 32) throw new CrmebException("生成后的 scene 超过微信32字节限制");
        JkPromotionCodeCache existing = cacheDao.selectOne(new LambdaQueryWrapper<JkPromotionCodeCache>()
                .eq(JkPromotionCodeCache::getSceneId, scene.getId()).eq(JkPromotionCodeCache::getOwnerUserId, ownerUserId)
                .eq(JkPromotionCodeCache::getSceneValue, sceneValue).eq(JkPromotionCodeCache::getIsDeleted, false)
                .last("limit 1"));
        if (existing != null && "SUCCESS".equals(existing.getStatus()) && existing.getFileObjectId() != null) {
            return response(scene, existing);
        }
        JkPromotionCodeCache requestCache = cacheDao.selectOne(new LambdaQueryWrapper<JkPromotionCodeCache>()
                .eq(JkPromotionCodeCache::getRequestNo, requestNo).last("limit 1"));
        if (requestCache != null) return response(scene, requestCache);
        Date now = new Date();
        JkPromotionCodeCache cache = existing == null
                ? new JkPromotionCodeCache().setSceneId(scene.getId()).setOwnerUserId(ownerUserId)
                    .setSceneValue(sceneValue).setRequestNo(requestNo).setStatus("GENERATING")
                    .setIsDeleted(false).setCreateTime(now)
                : existing;
        cache.setStatus("GENERATING").setErrorMessage(null).setUpdateTime(now);
        try {
            if (cache.getId() == null) cacheDao.insert(cache); else cacheDao.updateById(cache);
        } catch (DuplicateKeyException duplicate) {
            JkPromotionCodeCache duplicateValue = cacheDao.selectOne(new LambdaQueryWrapper<JkPromotionCodeCache>()
                    .eq(JkPromotionCodeCache::getRequestNo, requestNo).last("limit 1"));
            if (duplicateValue != null) return response(scene, duplicateValue);
            throw new CrmebException("推广码正在生成，请勿重复提交");
        }
        try {
            byte[] image = requestWechatCode(sceneValue, scene.getPagePath());
            JkFileObjectResponse file = storageService.storeBytes(image,
                    "wx-code-" + scene.getSceneCode() + "-" + ownerUserId + ".png", "image/png",
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
                && Boolean.TRUE.equals(storageService.status().get("ready"))
                && enabledSceneCount != null && enabledSceneCount > 0);
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
            throw new CrmebException("微信小程序码接口失败：" + safe(error.getStr("errmsg"))
                    + "（" + error.getInt("errcode", -1) + "）");
        }
        if (bytes.length < 8 || bytes[0] != (byte) 0x89 || bytes[1] != 0x50) {
            throw new CrmebException("微信未返回有效 PNG 小程序码");
        }
        return bytes;
    }

    private JkPromotionCodeResponse response(JkPromotionScene scene, JkPromotionCodeCache cache) {
        return new JkPromotionCodeResponse().setCacheId(cache.getId()).setSceneCode(scene.getSceneCode())
                .setSceneName(scene.getSceneName()).setSceneValue(cache.getSceneValue()).setPagePath(scene.getPagePath())
                .setFileObjectId(cache.getFileObjectId())
                .setDownloadPath(cache.getFileObjectId() == null ? null : "/api/front/jk/file/" + cache.getFileObjectId() + "/download")
                .setStatus(cache.getStatus()).setGeneratedAt(cache.getGeneratedAt()).setErrorMessage(cache.getErrorMessage());
    }

    private void assertRole(String roles, String role) {
        if (StrUtil.isBlank(roles)) return;
        List<String> allowed = Arrays.asList(roles.split(","));
        if (StrUtil.isBlank(role) || !allowed.contains(role)) throw new CrmebException("当前身份不能使用该推广场景");
    }

    private String safe(String value) {
        return value == null ? "未知错误" : value.replace('\r', ' ').replace('\n', ' ');
    }
}
