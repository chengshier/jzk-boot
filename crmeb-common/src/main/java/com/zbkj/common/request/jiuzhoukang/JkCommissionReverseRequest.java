package com.zbkj.common.request.jiuzhoukang;
import lombok.Data;
import java.math.BigDecimal;
@Data public class JkCommissionReverseRequest { private Long commissionRecordId; private BigDecimal amount; private String reverseType; private String requestNo; private String reason; }
