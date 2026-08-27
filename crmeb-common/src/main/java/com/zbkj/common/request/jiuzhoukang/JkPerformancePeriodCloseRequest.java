package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class JkPerformancePeriodCloseRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    @NotBlank(message = "关闭说明不能为空") private String remark;
}
