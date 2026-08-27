package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
public class JkOfflineSaleCreateRequest {
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    @NotBlank(message = "客户类型不能为空") private String customerType;
    private Long customerUserId;
    private String customerName;
    private String customerPhone;
    private Boolean registeredCustomer;
    @NotBlank(message = "收款方式不能为空") private String payMethod;
    @NotNull(message = "销售时间不能为空") private Date saleTime;
    private List<String> voucherUrls;
    private String promotionSource;
    @Valid @NotEmpty(message = "销售商品不能为空") private List<JkOfflineSaleItemRequest> items;
}
