package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;

/** 平台超管协助核查健康明细请求，必须填写原因并二次确认。 */
@Data
public class JkHealthEmergencyAccessRequest {
    @NotNull private Long ownerUserId;
    private String dataType;
    @NotBlank @Size(max=500) private String reason;
    @AssertTrue(message="必须完成二次确认") private Boolean confirmed;
}
