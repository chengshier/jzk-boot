package com.zbkj.service.service.jiuzhoukang.commission;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount; import java.math.BigDecimal;
public interface FundAccountService {
 JkFundAccount initialize(Long userId,String roleCode,String regionCode);
 JkFundAccount creditAvailable(Long userId,String roleCode,BigDecimal amount,String requestNo,String idempotencyKey);
 JkFundAccount freezeForWithdraw(Long userId,String roleCode,BigDecimal amount,String requestNo,String idempotencyKey);
 JkFundAccount releaseWithdraw(Long userId,String roleCode,BigDecimal amount,String requestNo,String idempotencyKey);
 JkFundAccount confirmPaid(Long userId,String roleCode,BigDecimal amount,String requestNo,String idempotencyKey);
 JkFundAccount reverseAvailableCommission(Long userId,String roleCode,BigDecimal amount,Long commissionRecordId,String requestNo,String idempotencyKey);
}
