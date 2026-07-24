package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.Date;
import java.util.List;

@Data
public class JkHealthAuthorizationSaveRequest {
    @NotNull private Long granteeUserId;
    @NotEmpty private List<String> scopeCodes;
    private Date effectiveTime;
    private Date expireTime;
    private Boolean allowExport;
    private String requestNo;
}
