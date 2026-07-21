package com.zbkj.service.exception.jiuzhoukang;

import com.zbkj.common.exception.CrmebException;

public class TradeAuditRejectedException extends CrmebException {

    public TradeAuditRejectedException(String errorMsg) {
        super(errorMsg);
    }
}
