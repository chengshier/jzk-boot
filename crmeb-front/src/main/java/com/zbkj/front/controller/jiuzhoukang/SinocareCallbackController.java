package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.request.jiuzhoukang.SinocareEnvelopeRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.jiuzhoukang.health.SinocareCallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 三诺爱看 1001-1005 加密回调；任何业务处理均在安全落库后异步执行。 */
@RestController
@RequestMapping("api/front/jk/health/sinocare")
public class SinocareCallbackController {
    @Autowired private SinocareCallbackService callbackService;

    @PostMapping("/authorization") public CommonResult<Object> authorization(@RequestBody @Validated SinocareEnvelopeRequest v){ return receive("1001", v); }
    @PostMapping("/device") public CommonResult<Object> device(@RequestBody @Validated SinocareEnvelopeRequest v){ return receive("1002", v); }
    @PostMapping("/cgm") public CommonResult<Object> cgm(@RequestBody @Validated SinocareEnvelopeRequest v){ return receive("1003", v); }
    @PostMapping("/report") public CommonResult<Object> report(@RequestBody @Validated SinocareEnvelopeRequest v){ return receive("1004", v); }
    @PostMapping("/report-file") public CommonResult<Object> reportFile(@RequestBody @Validated SinocareEnvelopeRequest v){ return receive("1005", v); }
    private CommonResult<Object> receive(String type, SinocareEnvelopeRequest v){ callbackService.receive(type, v); return CommonResult.success(); }
}
