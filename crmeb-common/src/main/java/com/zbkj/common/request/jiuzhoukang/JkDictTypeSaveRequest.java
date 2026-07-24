package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JkDictTypeSaveRequest {
    private Long id;
    @NotBlank(message = "字典类型编码不能为空")
    private String dictType;
    @NotBlank(message = "字典类型名称不能为空")
    private String dictName;
    private String remark;
    private Boolean status;
}
