package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkCommissionFlow;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionFlowDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Service
public class CommissionAccountServiceImpl extends ServiceImpl<JkCommissionAccountDao, JkCommissionAccount> implements CommissionAccountService {
    @Autowired private JkCommissionFlowDao flowDao;

    @Override public JkCommissionAccount initialize(Long userId, String roleCode, String regionCode) {
        JkCommissionAccount account = getOne(new LambdaQueryWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getUserId, userId).eq(JkCommissionAccount::getRoleCode, roleCode).eq(JkCommissionAccount::getIsDeleted, false));
        if (account != null) return account;
        Date now = new Date(); account = new JkCommissionAccount().setAccountNo("CA" + id()).setUserId(userId).setRoleCode(roleCode).setRegionCode(regionCode).setPendingSettleAmount(BigDecimal.ZERO).setSettledAmount(BigDecimal.ZERO).setFrozenCommissionAmount(BigDecimal.ZERO).setReversedAmount(BigDecimal.ZERO).setTotalCommissionAmount(BigDecimal.ZERO).setNegativeOffsetAmount(BigDecimal.ZERO).setStatus(true).setIsDeleted(false).setVersion(0).setCreateTime(now).setUpdateTime(now); save(account); return account;
    }

    @Override @Transactional public JkCommissionAccount creditPending(Long userId, String roleCode, BigDecimal amount, String requestNo, String key) {
        requirePositive(amount, "佣金金额非法"); JkCommissionFlow old = findFlow(key); if (old != null) return getById(old.getAccountId());
        JkCommissionAccount account = initialize(userId, roleCode, null); BigDecimal before = account.getPendingSettleAmount(); account.setPendingSettleAmount(before.add(amount)).setTotalCommissionAmount(account.getTotalCommissionAmount().add(amount)).setUpdateTime(new Date()); optimisticUpdate(account); writeFlow(account, null, "CREDIT_PENDING", amount, before, account.getPendingSettleAmount(), "RETAIL_ORDER", null, requestNo, key, "佣金待结算入账"); return account;
    }

    @Override @Transactional public JkCommissionAccount settle(Long userId, String roleCode, BigDecimal amount, String requestNo, String key) {
        requirePositive(amount, "结算金额非法"); JkCommissionFlow old = findFlow(key); if (old != null) return getById(old.getAccountId());
        JkCommissionAccount account = initialize(userId, roleCode, null); BigDecimal before = account.getPendingSettleAmount(); CommissionAccountSupport.Balance balance = CommissionAccountSupport.settle(before, account.getSettledAmount(), account.getTotalCommissionAmount(), amount); account.setPendingSettleAmount(balance.getPendingSettleAmount()).setSettledAmount(balance.getSettledAmount()).setUpdateTime(new Date()); optimisticUpdate(account); writeFlow(account, null, "SETTLE", amount.negate(), before, account.getPendingSettleAmount(), "COMMISSION_SETTLE", null, requestNo, key, "待结算佣金转可提现资金"); return account;
    }

    @Override @Transactional public JkCommissionAccount reversePending(Long userId, String roleCode, BigDecimal amount, String requestNo, String key) { return reverse(userId, roleCode, amount, requestNo, key, false, "REVERSE_PENDING", "待结算佣金冲正"); }
    @Override @Transactional public JkCommissionAccount reverseSettled(Long userId, String roleCode, BigDecimal amount, String requestNo, String key) { return reverse(userId, roleCode, amount, requestNo, key, true, "REVERSE_SETTLED", "已结算佣金冲正"); }
    @Override @Transactional public JkCommissionAccount reverseFrozen(Long userId, String roleCode, BigDecimal amount, String requestNo, String key) {
        requirePositive(amount, "冲正金额非法"); JkCommissionFlow old = findFlow(key); if (old != null) return getById(old.getAccountId()); JkCommissionAccount account = initialize(userId, roleCode, null); BigDecimal before = account.getFrozenCommissionAmount(); if (before.compareTo(amount) < 0) throw new IllegalArgumentException("冻结佣金不足，需转负向待抵扣"); account.setFrozenCommissionAmount(before.subtract(amount)).setReversedAmount(account.getReversedAmount().add(amount)).setUpdateTime(new Date()); optimisticUpdate(account); writeFlow(account, null, "REVERSE_FROZEN", amount.negate(), before, account.getFrozenCommissionAmount(), "COMMISSION_REVERSE", null, requestNo, key, "冻结佣金冲正"); return account;
    }

    @Override @Transactional public JkCommissionAccount freezeSettled(Long userId, String roleCode, BigDecimal amount, String sourceType, Long sourceId, String requestNo, String key, String reason) { return moveFreeze(userId, roleCode, amount, sourceType, sourceId, requestNo, key, reason, true); }
    @Override @Transactional public JkCommissionAccount releaseFrozen(Long userId, String roleCode, BigDecimal amount, String sourceType, Long sourceId, String requestNo, String key, String reason) { return moveFreeze(userId, roleCode, amount, sourceType, sourceId, requestNo, key, reason, false); }

    private JkCommissionAccount reverse(Long userId, String roleCode, BigDecimal amount, String requestNo, String key, boolean settled, String flowType, String remark) { requirePositive(amount, "冲正金额非法"); JkCommissionFlow old=findFlow(key); if(old!=null)return getById(old.getAccountId()); JkCommissionAccount account=initialize(userId,roleCode,null); BigDecimal before=settled?account.getSettledAmount():account.getPendingSettleAmount(); if(before.compareTo(amount)<0)throw new IllegalArgumentException(settled?"已结算佣金不足，需转负向待抵扣":"待结算佣金不足，需转负向待抵扣"); if(settled)account.setSettledAmount(before.subtract(amount)); else account.setPendingSettleAmount(before.subtract(amount)); account.setReversedAmount(account.getReversedAmount().add(amount)).setUpdateTime(new Date()); optimisticUpdate(account); writeFlow(account,null,flowType,amount.negate(),before,settled?account.getSettledAmount():account.getPendingSettleAmount(),"COMMISSION_REVERSE",null,requestNo,key,remark); return account; }
    private JkCommissionAccount moveFreeze(Long userId, String roleCode, BigDecimal amount, String sourceType, Long sourceId, String requestNo, String key, String reason, boolean freeze) { requirePositive(amount, freeze?"冻结金额非法":"解冻金额非法"); JkCommissionFlow old=findFlow(key); if(old!=null)return getById(old.getAccountId()); JkCommissionAccount account=initialize(userId,roleCode,null); BigDecimal before=freeze?account.getSettledAmount():account.getFrozenCommissionAmount(); CommissionAccountSupport.FreezeBalance balance=freeze?CommissionAccountSupport.freeze(account.getSettledAmount(),account.getFrozenCommissionAmount(),amount):CommissionAccountSupport.release(account.getSettledAmount(),account.getFrozenCommissionAmount(),amount); account.setSettledAmount(balance.getSettledAmount()).setFrozenCommissionAmount(balance.getFrozenCommissionAmount()).setUpdateTime(new Date()); optimisticUpdate(account); writeFlow(account,null,freeze?"FREEZE":"RELEASE",amount.negate(),before,freeze?account.getSettledAmount():account.getFrozenCommissionAmount(),sourceType,sourceId,requestNo,key,reason); return account; }
    private JkCommissionFlow findFlow(String key) { return flowDao.selectOne(new LambdaQueryWrapper<JkCommissionFlow>().eq(JkCommissionFlow::getIdempotencyKey, key)); }
    private void writeFlow(JkCommissionAccount account, Long recordId, String type, BigDecimal change, BigDecimal before, BigDecimal after, String sourceType, Long sourceId, String requestNo, String key, String remark) { flowDao.insert(new JkCommissionFlow().setFlowNo("CF" + id()).setAccountId(account.getId()).setCommissionRecordId(recordId).setFlowType(type).setChangeAmount(change).setBeforeAmount(before).setAfterAmount(after).setSourceType(sourceType).setSourceId(sourceId).setRequestNo(requestNo).setIdempotencyKey(key).setRemark(remark).setCreateTime(new Date())); }
    private void requirePositive(BigDecimal amount, String message) { if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException(message); }
    private void optimisticUpdate(JkCommissionAccount account) { int version=account.getVersion()==null?0:account.getVersion(); account.setVersion(version+1); boolean updated=update(account,new LambdaUpdateWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getId,account.getId()).eq(JkCommissionAccount::getVersion,version)); if(!updated)throw new IllegalStateException("佣金账户版本冲突，请重试"); }
    private String id() { return UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
}