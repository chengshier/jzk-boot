package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionFreezeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/** 冻结编排服务：金额变动统一委托 CommissionAccountService，不直接更新账户表。 */
@Service public class CommissionFreezeServiceImpl implements CommissionFreezeService {
 @Autowired private CommissionAccountService accountService;
 @Override @Transactional public void freezeCommission(Long u,String r,BigDecimal a,String type,String sourceType,Long sourceId,String requestNo,String key,String reason){accountService.freezeSettled(u,r,a,sourceType,sourceId,requestNo,key,reason);}
 @Override @Transactional public void releaseCommission(Long u,String r,BigDecimal a,String type,String sourceType,Long sourceId,String requestNo,String key,String reason){accountService.releaseFrozen(u,r,a,sourceType,sourceId,requestNo,key,reason);}
}