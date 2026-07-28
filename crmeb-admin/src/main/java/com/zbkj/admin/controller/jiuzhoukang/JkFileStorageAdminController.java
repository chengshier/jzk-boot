package com.zbkj.admin.controller.jiuzhoukang;

import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.storage.JkFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/admin/jk/file-storage")
public class JkFileStorageAdminController {
    @Autowired private JkFileStorageService storageService;

    @GetMapping("/status")
    @JkBizPermission(value = JkBizPermissionCodes.DICT_MANAGE, checkDataScope = false)
    public CommonResult<Map<String, Object>> status() {
        return CommonResult.success(storageService.status());
    }
}
