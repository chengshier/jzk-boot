package com.zbkj.front.controller.jiuzhoukang;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/front/jk/promotion/material")
@Api(tags = "九州康推广素材")
public class JkPromotionMaterialController {
    public static final String CONFIG_KEY = "jk_promotion_materials_json";

    @Autowired private SystemConfigService systemConfigService;
    @Autowired private FrontTokenComponent token;
    @Autowired private JkUserContextService contextService;

    @GetMapping("/list")
    @ApiOperation("当前身份可用推广素材")
    @JkBizPermission(value = JkBizPermissionCodes.TEAM_VIEW, checkDataScope = false)
    public CommonResult<List<Map<String, Object>>> list(@RequestParam(required = false) String type) {
        Integer frontUserId = token.getUserId();
        if (frontUserId == null || frontUserId <= 0) throw new IllegalArgumentException("请先登录");
        JkUserContext context = contextService.getFrontContext(Long.valueOf(frontUserId));
        String roleCode = context == null ? null : context.getPrimaryRoleCode();
        String raw = systemConfigService.getValueByKey(CONFIG_KEY);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (StrUtil.isBlank(raw) || !JSONUtil.isTypeJSONArray(raw)) return CommonResult.success(rows);

        JSONArray array = JSONUtil.parseArray(raw);
        for (Object value : array) {
            JSONObject item = JSONUtil.parseObj(value);
            if (!item.getBool("status", true)) continue;
            String materialType = item.getStr("type", "poster");
            if (StrUtil.isNotBlank(type) && !type.equals(materialType)) continue;
            if (!roleVisible(item.get("roleCodes"), roleCode)) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getStr("id", String.valueOf(rows.size() + 1)));
            row.put("type", materialType);
            row.put("title", item.getStr("title", "推广素材"));
            row.put("description", item.getStr("description", ""));
            row.put("copyText", item.getStr("copyText", ""));
            row.put("imageUrl", item.getStr("imageUrl", ""));
            row.put("sort", item.getInt("sort", 0));
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt(item -> -Integer.parseInt(String.valueOf(item.get("sort")))));
        return CommonResult.success(rows);
    }

    private boolean roleVisible(Object configuredRoles, String roleCode) {
        if (configuredRoles == null) return true;
        JSONArray roles = JSONUtil.parseArray(configuredRoles);
        if (roles.isEmpty()) return true;
        for (Object role : roles) {
            if (String.valueOf(role).equals(roleCode)) return true;
        }
        return false;
    }
}
