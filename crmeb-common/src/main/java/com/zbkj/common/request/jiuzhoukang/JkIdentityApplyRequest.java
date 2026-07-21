package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@ApiModel(value = "JkIdentityApplyRequest对象", description = "九州康身份申请请求")
public class JkIdentityApplyRequest implements Serializable {

    @NotBlank(message = "requestNo不能为空")
    @ApiModelProperty(value = "幂等请求号", required = true)
    private String requestNo;

    @NotBlank(message = "申请角色不能为空")
    @ApiModelProperty(value = "申请角色编码", required = true)
    private String applyRoleCode;

    @ApiModelProperty(value = "姓名")
    @Length(max = 128, message = "姓名长度不能超过128个字符")
    private String realName;

    @ApiModelProperty(value = "手机号")
    @Length(max = 32, message = "手机号长度不能超过32个字符")
    private String mobile;

    @ApiModelProperty(value = "区域编码")
    private String regionCode;

    @ApiModelProperty(value = "所属区县代用户ID")
    private Long belongCountyAgentId;

    @ApiModelProperty(value = "上级用户ID")
    private Long parentUserId;

    @ApiModelProperty(value = "申请原因")
    @Length(max = 1000, message = "申请原因长度不能超过1000个字符")
    private String applyReason;

    @ApiModelProperty(value = "资料附件JSON")
    private String certificateFiles;
}
