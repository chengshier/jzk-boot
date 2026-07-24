package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JkDictItemSaveRequest {
    private Long id;
    @NotBlank(message = "字典类型不能为空")
    private String dictType;
    @NotBlank(message = "字典项编码不能为空")
    private String itemCode;
    @NotBlank(message = "字典项名称不能为空")
    private String itemLabel;
    private String itemTag;
    private Integer sort;
    private String remark;
    private Boolean status;
}
