package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class JkOfflineSaleCreateRequest {
    @NotNull private String requestNo;
    @NotNull private String customerType;
    private Long customerUserId;
    private String customerName;
    private String customerPhone;
    private Boolean registeredCustomer;
    private String paymentMethod;
    private String voucherUrl;
    private String promotionSource;
    private Date saleTime;
    @Valid @NotEmpty @Size(max = 50) private List<Item> items;

    @Data
    public static class Item {
        @NotNull private Integer productId;
        private Integer skuId;
        @NotNull @Min(1) private Integer quantity;
        @NotNull @DecimalMin(value = "0.01") private BigDecimal unitPrice;
    }
}
