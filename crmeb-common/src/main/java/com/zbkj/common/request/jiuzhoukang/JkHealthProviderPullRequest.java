package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class JkHealthProviderPullRequest {
    @NotNull private Long providerId;
    private Boolean resetCursor;
    private Integer limit;
}
