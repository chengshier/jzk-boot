package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class JkHealthReportGenerateRequest {
    @NotBlank(message="报告类型不能为空") private String reportType;
    @NotNull(message="周期开始日期不能为空") private Date periodStart;
    @NotNull(message="周期结束日期不能为空") private Date periodEnd;
    @NotBlank(message="requestNo不能为空") private String requestNo;
}
