package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class JkHealthLifestyleSaveRequest {
    private String dataType;
    private Date occurredAt;
    @Size(max = 2000) private String content;
    @Size(max = 128) private String category;
    private Integer durationMinutes;
    private BigDecimal calories;
    @Size(max = 500) private String remark;
    private String requestNo;
}
