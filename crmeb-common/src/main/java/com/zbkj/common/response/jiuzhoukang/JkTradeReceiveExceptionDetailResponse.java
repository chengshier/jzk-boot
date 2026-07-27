package com.zbkj.common.response.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveExceptionItem;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/** 异常收货详情。 */
@Data
@Accessors(chain = true)
public class JkTradeReceiveExceptionDetailResponse {
    private JkTradeReceiveException exception;
    private List<JkTradeReceiveExceptionItem> items = new ArrayList<>();
}
