package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkDictItem;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.dict.JkDictService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/front/jk/dict")
@Api(tags = "九州康前台字典")
public class JkDictFrontController {
    @Autowired private JkDictService dictService;

    @GetMapping("/{dictType}")
    public CommonResult<List<JkDictItem>> list(@PathVariable String dictType) {
        return CommonResult.success(dictService.listItems(dictType, true));
    }
}
