package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkHealthData;
import com.zbkj.common.request.jiuzhoukang.JkHealthDeviceCallbackRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.impl.jiuzhoukang.health.JkHealthSignatureVerifier;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthSyncService;
import com.zbkj.service.service.jiuzhoukang.health.JkHealthProviderService;
import java.util.List;
import java.util.Map;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 健康设备第三方回调。
 * <p>该接口不使用用户 token，但必须通过 HMAC 验签和时间戳校验；默认配置为关闭。</p>
 */
@RestController
@RequestMapping("api/front/jk/health/device")
@Api(tags = "九州康健康设备回调")
public class JkHealthDeviceCallbackController {
    @Autowired private JkHealthSignatureVerifier signatureVerifier;
    @Autowired private JkHealthSyncService syncService;
    @Autowired private JkHealthProviderService providerService;

    @PostMapping("/callback")
    public CommonResult<JkHealthData> callback(@RequestBody @Validated JkHealthDeviceCallbackRequest request){
        signatureVerifier.verify(request);
        return CommonResult.success(syncService.receive(request));
    }
    /**
     * 厂商级通用回调。路径中的 providerCode 决定字段映射和验签配置，
     * 因此常规 REST/JSON 厂商可以只改后台配置而无需新增 Controller 或 Service。
     */
    @PostMapping("/provider/{providerCode}/callback")
    public CommonResult<List<JkHealthData>> providerCallback(@PathVariable String providerCode,
                                                              @RequestBody String rawBody,
                                                              @RequestHeader Map<String,String> headers){
        return CommonResult.success(providerService.receiveCallback(providerCode, rawBody, headers));
    }

}
