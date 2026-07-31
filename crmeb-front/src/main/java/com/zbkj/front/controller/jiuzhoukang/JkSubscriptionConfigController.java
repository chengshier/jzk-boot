package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.wechat.JkWechatAccessTokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录用户查询当前场景可主动订阅的微信模板。
 *
 * <p>模板 ID 并非密钥，但仅在微信能力、appid/secret 和订阅发送均就绪时返回。每个场景最多返回
 * 三个模板，满足小程序 requestSubscribeMessage 单次调用限制。</p>
 */
@RestController
@RequestMapping("api/front/jk/subscription")
@Api(tags = "九州康小程序订阅消息配置")
public class JkSubscriptionConfigController {

    @Autowired private JkWechatAccessTokenService tokenService;

    @Value("${jk.wechat.subscribe-enabled:false}") private boolean subscribeEnabled;
    @Value("${jk.wechat.subscribe.audit-template-id:}") private String auditTemplateId;
    @Value("${jk.wechat.subscribe.transfer-template-id:}") private String transferTemplateId;
    @Value("${jk.wechat.subscribe.receive-template-id:}") private String receiveTemplateId;
    @Value("${jk.wechat.subscribe.withdraw-template-id:}") private String withdrawTemplateId;

    @GetMapping("/config")
    @ApiOperation("查询业务或提现场景可订阅模板")
    public CommonResult<Map<String, Object>> config(@RequestParam(defaultValue = "BUSINESS") String scene) {
        String normalized = normalizeScene(scene);
        Map<String, Object> wechatStatus = tokenService.status();
        boolean wechatEnabled = Boolean.TRUE.equals(wechatStatus.get("enabled"));
        boolean wechatReady = Boolean.TRUE.equals(wechatStatus.get("ready"));
        List<String> templateIds = new ArrayList<String>();
        if (wechatReady && subscribeEnabled) {
            if ("WITHDRAW".equals(normalized)) {
                add(templateIds, withdrawTemplateId);
            } else {
                add(templateIds, auditTemplateId);
                add(templateIds, transferTemplateId);
                add(templateIds, receiveTemplateId);
            }
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scene", normalized);
        result.put("wechatEnabled", wechatEnabled);
        result.put("wechatReady", wechatReady);
        result.put("appidConfigured", wechatStatus.get("appidConfigured"));
        result.put("secretConfigured", wechatStatus.get("secretConfigured"));
        result.put("subscribeEnabled", subscribeEnabled);
        result.put("ready", wechatReady && subscribeEnabled && !templateIds.isEmpty());
        result.put("templateIds", templateIds);
        result.put("message", message(wechatEnabled, wechatReady, templateIds));
        return CommonResult.success(result);
    }

    private String normalizeScene(String scene) {
        String value = scene == null ? "BUSINESS" : scene.trim().toUpperCase();
        if (!"BUSINESS".equals(value) && !"WITHDRAW".equals(value)) return "BUSINESS";
        return value;
    }

    private void add(List<String> ids, String value) {
        if (value == null || value.trim().isEmpty() || ids.size() >= 3) return;
        String id = value.trim();
        if (!ids.contains(id)) ids.add(id);
    }

    private String message(boolean wechatEnabled, boolean wechatReady, List<String> ids) {
        if (!wechatEnabled) return "微信能力尚未启用";
        if (!wechatReady) return "微信 appid 或 secret 尚未配置完整";
        if (!subscribeEnabled) return "订阅消息发送尚未启用";
        if (ids.isEmpty()) return "当前场景模板尚未配置";
        return "可申请订阅";
    }
}
