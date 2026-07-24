package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class JkAdminUserMappingSaveRequest {
    private Long id;
    @NotNull(message = "后台管理员不能为空") private Integer systemAdminId;
    @NotNull(message = "前台业务用户不能为空") private Long frontUserId;
    private String remark;
    private Boolean status;
}
