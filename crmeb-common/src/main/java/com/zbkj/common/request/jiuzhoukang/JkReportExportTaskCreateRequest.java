package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JkReportExportTaskCreateRequest {
    @NotNull private String reportType;
    @NotNull private String requestNo;
    private String requestJson;
}
