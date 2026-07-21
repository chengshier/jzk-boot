package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(value = "JkIdentityStatusOperateRequest对象", description = "九州康用户业务身份状态操作请求")
public class JkIdentityStatusOperateRequest implements Serializable {

    @NotNull(message = "用户业务身份ID不能为空")
    private Long userBusinessRoleId;

    @Length(max = 500, message = "原因长度不能超过500个字符")
    private String reason;
}
