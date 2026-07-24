package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkDictItem;
import com.zbkj.common.model.jiuzhoukang.JkDictType;
import com.zbkj.common.request.jiuzhoukang.JkDictItemSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkDictTypeSaveRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.dict.JkDictService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/admin/jk/dict")
@Api(tags = "九州康动态字典管理")
public class JkDictController {
    @Autowired private JkDictService dictService;

    @GetMapping("/type/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_DICT_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE)
    public CommonResult<List<JkDictType>> typeList(@RequestParam(required = false) String keywords) {
        return CommonResult.success(dictService.listTypes(keywords));
    }

    @PostMapping("/type/save")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_DICT_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE)
    public CommonResult<JkDictType> saveType(@RequestBody @Validated JkDictTypeSaveRequest request) {
        return CommonResult.success(dictService.saveType(request));
    }

    @PostMapping("/type/status")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_DICT_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE)
    public CommonResult<Boolean> typeStatus(@RequestParam Long id, @RequestParam boolean status) {
        return CommonResult.success(dictService.updateTypeStatus(id, status));
    }

    @GetMapping("/item/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_DICT_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE)
    public CommonResult<List<JkDictItem>> itemList(@RequestParam String dictType) {
        return CommonResult.success(dictService.listItems(dictType, false));
    }

    @PostMapping("/item/save")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_DICT_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE)
    public CommonResult<JkDictItem> saveItem(@RequestBody @Validated JkDictItemSaveRequest request) {
        return CommonResult.success(dictService.saveItem(request));
    }

    @PostMapping("/item/status")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_DICT_MANAGE + "')")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE)
    public CommonResult<Boolean> itemStatus(@RequestParam Long id, @RequestParam boolean status) {
        return CommonResult.success(dictService.updateItemStatus(id, status));
    }
}
