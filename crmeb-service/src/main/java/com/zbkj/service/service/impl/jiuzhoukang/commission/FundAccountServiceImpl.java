package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount;
import com.zbkj.common.model.jiuzhoukang.JkFundFlow;
import com.zbkj.service.dao.jiuzhoukang.JkFundAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundFlowDao;
import com.zbkj.service.service.jiuzhoukang.commission.FundAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Service
public class FundAccountServiceImpl extends ServiceImpl<JkFundAccountDao, JkFundAccount> implements FundAccountService {
    @Autowired private JkFundFlowDao flowDao;

    @Override public JkFundAccount initialize(Long userId, String roleCode, String regionCode) {
        JkFundAccount account = getOne(new LambdaQueryWrapper<JkFundAccount>().eq(JkFundAccount::getUserId, userId)
                .eq(JkFundAccount::getRoleCode, roleCode).eq(JkFundAccount::getIsDeleted, false));
        if (account != null) return account;
        Date now = new Date();
        account = new JkFundAccount().setAccountNo("FA" + id()).setUserId(userId).setRoleCode(roleCode).setRegionCode(regionCode)
                .setAvailableAmount(BigDecimal.ZERO).setWithdrawingAmount(BigDecimal.ZERO).setWithdrawnAmount(BigDecimal.ZERO)
                .setRejectedReturnAmount(BigDecimal.ZERO).setFrozenAmount(BigDecimal.ZERO).setNegativeOffsetAmount(BigDecimal.ZERO)
                .setStatus(true).setIsDeleted(false).setVersion(0).setCreateTime(now).setUpdateTime(now);
        save(account); return account;
    }
    @Override @Transactional public JkFundAccount creditAvailable(Long userId, String roleCode, BigDecimal amount, String requestNo, String key) {
        requirePositive(amount); JkFundFlow old = findFlow(key); if (old != null) return getById(old.getAccountId());
        JkFundAccount account=initialize(userId,roleCode,null); BigDecimal before=account.getAvailableAmount();
        account.setAvailableAmount(before.add(amount)).setUpdateTime(new Date()); optimisticUpdate(account);
        flow(account,null,"SETTLE_IN",amount,before,account.getAvailableAmount(),"COMMISSION_SETTLE",null,requestNo,key,"佣金结算转入可提现余额"); return account;
    }
    @Override @Transactional public JkFundAccount freezeForWithdraw(Long userId,String roleCode,BigDecimal amount,String requestNo,String key) {
        requirePositive(amount); JkFundFlow old=findFlow(key); if(old!=null)return getById(old.getAccountId()); JkFundAccount account=initialize(userId,roleCode,null);
        BigDecimal before=account.getAvailableAmount(); if(before.compareTo(amount)<0)throw new IllegalArgumentException("可提现余额不足");
        account.setAvailableAmount(before.subtract(amount)).setWithdrawingAmount(account.getWithdrawingAmount().add(amount)).setFrozenAmount(account.getFrozenAmount().add(amount)).setUpdateTime(new Date()); optimisticUpdate(account);
        flow(account,null,"WITHDRAW_FREEZE",amount.negate(),before,account.getAvailableAmount(),"WITHDRAW",null,requestNo,key,"提交提现冻结资金"); return account;
    }
    @Override @Transactional public JkFundAccount releaseWithdraw(Long userId,String roleCode,BigDecimal amount,String requestNo,String key) {
        requirePositive(amount); JkFundFlow old=findFlow(key); if(old!=null)return getById(old.getAccountId()); JkFundAccount account=initialize(userId,roleCode,null);
        if(account.getWithdrawingAmount().compareTo(amount)<0)throw new IllegalArgumentException("提现冻结金额不足"); BigDecimal before=account.getAvailableAmount();
        account.setAvailableAmount(before.add(amount)).setWithdrawingAmount(account.getWithdrawingAmount().subtract(amount)).setFrozenAmount(account.getFrozenAmount().subtract(amount)).setRejectedReturnAmount(account.getRejectedReturnAmount().add(amount)).setUpdateTime(new Date()); optimisticUpdate(account);
        flow(account,null,"WITHDRAW_RELEASE",amount,before,account.getAvailableAmount(),"WITHDRAW",null,requestNo,key,"提现驳回释放资金"); return account;
    }
    @Override @Transactional public JkFundAccount confirmPaid(Long userId,String roleCode,BigDecimal amount,String requestNo,String key) {
        requirePositive(amount); JkFundFlow old=findFlow(key); if(old!=null)return getById(old.getAccountId()); JkFundAccount account=initialize(userId,roleCode,null);
        if(account.getWithdrawingAmount().compareTo(amount)<0)throw new IllegalArgumentException("提现冻结金额不足"); BigDecimal before=account.getWithdrawingAmount();
        account.setWithdrawingAmount(before.subtract(amount)).setFrozenAmount(account.getFrozenAmount().subtract(amount)).setWithdrawnAmount(account.getWithdrawnAmount().add(amount)).setUpdateTime(new Date()); optimisticUpdate(account);
        flow(account,null,"WITHDRAW_PAID",amount.negate(),before,account.getWithdrawingAmount(),"WITHDRAW",null,requestNo,key,"线下打款确认"); return account;
    }
    @Override @Transactional public JkFundAccount reverseAvailableCommission(Long userId, String roleCode, BigDecimal amount, Long commissionRecordId, String requestNo, String key) {
        requirePositive(amount); JkFundFlow old = findFlow(key); if (old != null) return getById(old.getAccountId());
        JkFundAccount account = initialize(userId, roleCode, null); BigDecimal before = account.getAvailableAmount();
        CommissionReverseSupport.FundReverse reversed = CommissionReverseSupport.reverseFundAvailable(before, amount, account.getNegativeOffsetAmount());
        account.setAvailableAmount(reversed.getAvailableAmount()).setNegativeOffsetAmount(reversed.getNegativeOffsetAmount()).setUpdateTime(new Date()); optimisticUpdate(account);
        flow(account, null, "REVERSE_DEDUCT", amount.negate(), before, account.getAvailableAmount(), "COMMISSION_REVERSE", commissionRecordId, requestNo, key, "佣金冲正扣减可提现余额，余额不足转负向待抵扣"); return account;
    }    private JkFundFlow findFlow(String key){return flowDao.selectOne(new LambdaQueryWrapper<JkFundFlow>().eq(JkFundFlow::getIdempotencyKey,key));}
    private void flow(JkFundAccount account,Long withdrawId,String type,BigDecimal change,BigDecimal before,BigDecimal after,String sourceType,Long sourceId,String requestNo,String key,String remark){flowDao.insert(new JkFundFlow().setFlowNo("FF"+id()).setAccountId(account.getId()).setWithdrawApplyId(withdrawId).setFlowType(type).setChangeAmount(change).setBeforeAmount(before).setAfterAmount(after).setSourceType(sourceType).setSourceId(sourceId).setRequestNo(requestNo).setIdempotencyKey(key).setRemark(remark).setCreateTime(new Date()));}
    private void requirePositive(BigDecimal amount){if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("资金金额必须大于零");}
    private void optimisticUpdate(JkFundAccount account) {
        int version = account.getVersion() == null ? 0 : account.getVersion();
        account.setVersion(version + 1);
        boolean updated = update(account, new LambdaUpdateWrapper<JkFundAccount>()
                .eq(JkFundAccount::getId, account.getId()).eq(JkFundAccount::getVersion, version));
        if (!updated) throw new IllegalStateException("资金账户版本冲突，请重试");
    }    private String id(){return UUID.randomUUID().toString().replace("-","").substring(0,16);}
}
