package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

@Data
public class JkPromotionQrcodeResponse {
    private Long userId;
    private String sharePath;
    private String shareUrl;
    private String qrCodeBase64;
    private String description;
}
