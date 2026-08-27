package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;

import java.math.BigDecimal;
import java.util.Date;

public interface JkCommissionLimitService {
    ReservationResult reserve(JkCommissionRule rule, Long beneficiaryUserId, Date businessTime,
                              BigDecimal requestedAmount, String actionKey);

    class ReservationResult {
        private BigDecimal requestedAmount;
        private BigDecimal approvedAmount;
        private String resultCode;
        private String resultMessage;
        private boolean duplicate;

        public BigDecimal getRequestedAmount() { return requestedAmount; }
        public ReservationResult setRequestedAmount(BigDecimal value) { this.requestedAmount = value; return this; }
        public BigDecimal getApprovedAmount() { return approvedAmount; }
        public ReservationResult setApprovedAmount(BigDecimal value) { this.approvedAmount = value; return this; }
        public String getResultCode() { return resultCode; }
        public ReservationResult setResultCode(String value) { this.resultCode = value; return this; }
        public String getResultMessage() { return resultMessage; }
        public ReservationResult setResultMessage(String value) { this.resultMessage = value; return this; }
        public boolean isDuplicate() { return duplicate; }
        public ReservationResult setDuplicate(boolean value) { this.duplicate = value; return this; }
    }
}
