package com.zbkj.common.request;

import com.zbkj.common.constants.RegularConstants;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="UserAddressRequest对象", description="新增用户地址对象")
public class UserAddressRequest implements Serializable {
    private static final long serialVersionUID=1L;

    @ApiModelProperty(value = "用户地址id") private Integer id;
    @ApiModelProperty(value = "收货人姓名", required = true)
    @NotBlank(message = "收货人姓名不能为空")
    @Length(max = 32, message = "收货人姓名不能超过32个字符")
    private String realName;
    @ApiModelProperty(value = "收货人电话", required = true)
    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = RegularConstants.PHONE_TWO, message = "请填写正确的收货人电话")
    private String phone;
    @ApiModelProperty(value = "收货人详细地址", required = true)
    @NotBlank(message = "收货人详细地址不能为空")
    @Length(max = 256, message = "收货人详细地址不能超过256个字符")
    private String detail;
    @ApiModelProperty(value = "是否默认", example = "false", required = true) private Boolean isDefault;

    @ApiModelProperty(value = "九州康标准区域编码；可为空，空时该地址不能用于区域归属兜底")
    private String jkRegionCode;

    @Valid
    @ApiModelProperty(value = "城市信息", required = true)
    private UserAddressCityRequest address;
}
