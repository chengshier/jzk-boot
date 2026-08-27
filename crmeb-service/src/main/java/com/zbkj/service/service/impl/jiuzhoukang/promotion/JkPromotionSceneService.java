package com.zbkj.service.service.impl.jiuzhoukang.promotion;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkPromotionScene;
import com.zbkj.common.model.jiuzhoukang.JkPromotionStat;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.JkPromotionSceneDao;
import com.zbkj.service.dao.jiuzhoukang.JkPromotionStatDao;
import com.zbkj.service.service.impl.jiuzhoukang.storage.JkMinioObjectStorageService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 微信小程序码场景服务。外部开关默认关闭，不返回伪造二维码。 */
@Service
public class JkPromotionSceneService {
    @Autowired private JkPromotionSceneDao sceneDao;
    @Autowired private JkPromotionStatDao statDao;
    @Autowired private JkUserContextService contextService;
    @Autowired private JkMinioObjectStorageService storageService;

    @Value("${jk.wechat.miniprogram-code.enabled:false}") private boolean enabled;
    @Value("${jk.wechat.miniprogram-code.app-id:}") private String appId;
    @Value("${jk.wechat.miniprogram-code.secret:}") private String secret;
    @Value("${jk.wechat.miniprogram-code.page:pages/index/index}") private String defaultPage;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> qrcode(Long promoterUserId, boolean forceRefresh) {
        requireEnabled();
        JkUserContext context = contextService.getFrontContext(promoterUserId);
        if (context == null || context.getPrimaryRoleCode() == null || Boolean.TRUE.equals(context.getFreezeStatus())) {
            throw new CrmebException("当前身份不能生成推广码");
        }
        JkPromotionScene scene = sceneDao.selectOne(new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getPromoterUserId, promoterUserId).eq(JkPromotionScene::getStatus, "ACTIVE")
                .eq(JkPromotionScene::getIsDeleted, false).orderByDesc(JkPromotionScene::getId).last("limit 1"));
        if (scene == null) {
            Date now = new Date();
            String opaque = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            scene = new JkPromotionScene().setSceneCode("PS" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                    .setPromoterUserId(promoterUserId).setPromoterRoleCode(context.getPrimaryRoleCode())
                    .setRegionCode(context.getRegionCode()).setPagePath(defaultPage).setSceneValue(opaque)
                    .setStatus("ACTIVE").setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
            sceneDao.insert(scene);
        }
        if (forceRefresh || blank(scene.getObjectKey())) {
            byte[] image = requestWechatCode(scene.getSceneValue(), scene.getPagePath());
            String key = "promotion/qrcode/" + promoterUserId + "/" + scene.getSceneCode() + ".png";
            scene.setObjectKey(storageService.put(key, image, "image/png")).setUpdateTime(new Date());
            sceneDao.updateById(scene);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sceneCode", scene.getSceneCode());
        result.put("sceneValue", scene.getSceneValue());
        result.put("pagePath", scene.getPagePath());
        result.put("imageUrl", storageService.presignedDownloadUrl(scene.getObjectKey(), 60));
        result.put("expiresInMinutes", 60);
        return result;
    }

    public Map<String, Object> resolve(String sceneValue) {
        JkPromotionScene scene = sceneDao.selectOne(new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getSceneValue, sceneValue).eq(JkPromotionScene::getStatus, "ACTIVE")
                .eq(JkPromotionScene::getIsDeleted, false).last("limit 1"));
        if (scene == null || (scene.getExpireTime() != null && !scene.getExpireTime().after(new Date()))) {
            throw new CrmebException("推广场景不存在或已失效");
        }
        record(scene.getId(), 1, 0, 0, 0, 0, BigDecimal.ZERO);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("promoterUserId", scene.getPromoterUserId());
        result.put("promoterRoleCode", scene.getPromoterRoleCode());
        result.put("regionCode", scene.getRegionCode());
        result.put("sceneCode", scene.getSceneCode());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordBusiness(String sceneValue, boolean newUser, boolean initialBind, boolean effectiveBind,
                               boolean buyer, BigDecimal saleAmount) {
        JkPromotionScene scene = sceneDao.selectOne(new LambdaQueryWrapper<JkPromotionScene>()
                .eq(JkPromotionScene::getSceneValue, sceneValue).eq(JkPromotionScene::getStatus, "ACTIVE")
                .eq(JkPromotionScene::getIsDeleted, false).last("limit 1"));
        if (scene == null) return;
        record(scene.getId(), 0, newUser ? 1 : 0, initialBind ? 1 : 0, effectiveBind ? 1 : 0,
                buyer ? 1 : 0, saleAmount == null ? BigDecimal.ZERO : saleAmount);
    }

    public PageInfo<JkPromotionStat> listStats(Long sceneId, Date startDate, Date endDate, PageParamRequest pageParam) {
        Page<JkPromotionStat> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkPromotionStat> query = new LambdaQueryWrapper<JkPromotionStat>().orderByDesc(JkPromotionStat::getStatDate);
        if (sceneId != null) query.eq(JkPromotionStat::getSceneId, sceneId);
        if (startDate != null) query.ge(JkPromotionStat::getStatDate, startOfDay(startDate));
        if (endDate != null) query.le(JkPromotionStat::getStatDate, startOfDay(endDate));
        List<JkPromotionStat> rows = statDao.selectList(query);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Transactional(rollbackFor = Exception.class)
    protected void record(Long sceneId, int scan, int newUser, int initialBind, int effectiveBind, int buyer, BigDecimal amount) {
        Date day = startOfDay(new Date());
        JkPromotionStat stat = statDao.selectOne(new LambdaQueryWrapper<JkPromotionStat>()
                .eq(JkPromotionStat::getSceneId, sceneId).eq(JkPromotionStat::getStatDate, day).last("limit 1 for update"));
        Date now = new Date();
        if (stat == null) {
            stat = new JkPromotionStat().setSceneId(sceneId).setStatDate(day).setScanCount(scan).setNewUserCount(newUser)
                    .setInitialBindCount(initialBind).setEffectiveBindCount(effectiveBind).setBuyerCount(buyer)
                    .setSaleAmount(amount).setCreateTime(now).setUpdateTime(now);
            statDao.insert(stat);
        } else {
            stat.setScanCount(nvl(stat.getScanCount()) + scan).setNewUserCount(nvl(stat.getNewUserCount()) + newUser)
                    .setInitialBindCount(nvl(stat.getInitialBindCount()) + initialBind)
                    .setEffectiveBindCount(nvl(stat.getEffectiveBindCount()) + effectiveBind)
                    .setBuyerCount(nvl(stat.getBuyerCount()) + buyer)
                    .setSaleAmount(money(stat.getSaleAmount()).add(amount)).setUpdateTime(now);
            statDao.updateById(stat);
        }
    }

    private byte[] requestWechatCode(String scene, String page) {
        if (blank(appId) || blank(secret)) throw new CrmebException("微信小程序 AppId 或 Secret 未配置");
        RestTemplate rest = new RestTemplate();
        String tokenRaw = rest.getForObject("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appId}&secret={secret}", String.class, appId, secret);
        JSONObject tokenJson = JSON.parseObject(tokenRaw);
        String token = tokenJson.getString("access_token");
        if (blank(token)) throw new CrmebException("获取微信 access_token 失败：" + tokenJson.getString("errmsg"));
        JSONObject body = new JSONObject(); body.put("scene", scene); body.put("page", page); body.put("check_path", true); body.put("env_version", "release"); body.put("width", 430);
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<byte[]> response = rest.postForEntity("https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + token,
                new HttpEntity<String>(body.toJSONString(), headers), byte[].class);
        byte[] bytes = response.getBody();
        if (bytes == null || bytes.length < 100) throw new CrmebException("微信小程序码响应为空");
        String head = new String(bytes, 0, Math.min(bytes.length, 80));
        if (head.trim().startsWith("{")) {
            JSONObject error = JSON.parseObject(new String(bytes));
            throw new CrmebException("生成微信小程序码失败：" + error.getString("errmsg"));
        }
        return bytes;
    }

    private void requireEnabled() { if (!enabled) throw new CrmebException("微信小程序码能力尚未启用"); if (!storageService.isEnabled()) throw new CrmebException("MinIO 尚未启用，不能生成可持久化推广码"); }
    private Date startOfDay(Date date) { Calendar c = Calendar.getInstance(); c.setTime(date); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTime(); }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
