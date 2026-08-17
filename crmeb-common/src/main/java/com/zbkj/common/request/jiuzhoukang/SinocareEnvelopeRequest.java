package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/** 三诺爱看回调统一密文信封；明文仅能在验签和解密成功后处理。 */
@Data
@Accessors(chain = true)
public class SinocareEnvelopeRequest {
    @NotBlank
    private String ciphertext;
    @NotBlank
    private String signature;
}
