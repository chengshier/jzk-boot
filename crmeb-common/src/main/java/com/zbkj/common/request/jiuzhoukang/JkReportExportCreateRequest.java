package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
public class JkReportExportCreateRequest {
    @NotBlank private String reportType;
    private Date startDate;
    private Date endDate;
    private String regionCode;
    private Long userId;
    private String dataType;
}
