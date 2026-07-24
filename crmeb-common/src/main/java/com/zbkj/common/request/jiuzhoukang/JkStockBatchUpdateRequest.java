package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/** 只维护批次元数据，不允许通过该接口直接改可用量或冻结量。 */
@Data
public class JkStockBatchUpdateRequest {
    @NotNull private Long id;
    private BigDecimal unitCost;
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") private Date productionDate;
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") private Date expireTime;
}
