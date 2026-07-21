package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class JkTradeCreateRequest {
    @NotBlank private String requestNo;
    @Valid @NotEmpty private List<JkTradeLineRequest> items;
}
