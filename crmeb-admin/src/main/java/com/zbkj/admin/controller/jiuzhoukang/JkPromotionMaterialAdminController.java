package com.zbkj.admin.controller.jiuzhoukang;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.SystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/promotion/material")
@Api(tags = "九州康推广素材管理")
public class JkPromotionMaterialAdminController {

    @Autowired private SystemConfigService systemConfigService;

    @GetMapping("/list")
    @ApiOperation("推广素材配置列表")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_DICT_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE, checkDataScope = false)
    public CommonResult<List<Map<String, Object>>> list() {
        String raw = systemConfigService.getValueByKey(JkBizConstants.CONFIG_KEY_PROMOTION_MATERIALS);
        if (StrUtil.isBlank(raw) || !JSONUtil.isTypeJSONArray(raw)) {
            return CommonResult.success(new ArrayList<Map<String, Object>>());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        JSONArray array = JSONUtil.parseArray(raw);
        for (Object value : array) result.add(new LinkedHashMap<>(JSONUtil.parseObj(value)));
        return CommonResult.success(result);
    }

    @PostMapping("/save")
    @ApiOperation("保存推广素材配置")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_DICT_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE, checkDataScope = false)
    public CommonResult<Boolean> save(@RequestBody List<Map<String, Object>> rows) {
        if (rows == null) rows = new ArrayList<>();
        JSONArray result = new JSONArray();
        int index = 0;
        for (Map<String, Object> source : rows) {
            JSONObject item = new JSONObject(source);
            String type = StrUtil.blankToDefault(item.getStr("type"), "poster");
            if (!("copy".equals(type) || "product".equals(type) || "health".equals(type) || "poster".equals(type))) {
                throw new IllegalArgumentException("不支持的素材类型：" + type);
            }
            String title = item.getStr("title");
            if (StrUtil.isBlank(title)) throw new IllegalArgumentException("第" + (index + 1) + "条素材标题不能为空");
            String imageUrl = item.getStr("imageUrl");
            if (!"copy".equals(type) && StrUtil.isBlank(imageUrl)) {
                throw new IllegalArgumentException("第" + (index + 1) + "条图片素材地址不能为空");
            }
            if (StrUtil.isBlank(item.getStr("id"))) item.set("id", "PM" + System.currentTimeMillis() + index);
            item.set("type", type);
            item.set("title", title.trim());
            item.set("description", StrUtil.blankToDefault(item.getStr("description"), ""));
            item.set("copyText", StrUtil.blankToDefault(item.getStr("copyText"), ""));
            item.set("imageUrl", StrUtil.blankToDefault(imageUrl, ""));
            item.set("sort", item.getInt("sort", 0));
            item.set("status", item.getBool("status", true));
            if (item.get("roleCodes") == null) item.set("roleCodes", new JSONArray());
            result.add(item);
            index++;
        }
        return CommonResult.success(systemConfigService.updateOrSaveValueByName(
                JkBizConstants.CONFIG_KEY_PROMOTION_MATERIALS, JSONUtil.toJsonStr(result)));
    }
}
