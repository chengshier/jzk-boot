package com.zbkj.common.exception.jiuzhoukang;

import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.result.CommonResultCode;

public class JkForbiddenException extends CrmebException {

    public JkForbiddenException(String message) {
        super(CommonResultCode.FORBIDDEN, message);
    }
}