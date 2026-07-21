package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkCommissionReverse;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionReverseDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseSupport;
import com.zbkj.service.service.jiuzhoukang.commission.FundAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service public class CommissionReverseServiceImpl implements CommissionReverseService {
 @Autowired private JkCommissionReverseDao reverseDao; @Autowired private JkCommissionRecordDao recordDao; @Autowired private JkCommissionAccountDao accountDao; @Autowired private CommissionAccountService accountService; @Autowired private FundAccountService fundAccountService;
 @Override @Transactional public JkCommissionReverse reverse(Long recordId,String sourceType,Long sourceId,String sourceNo,String reverseType,BigDecimal amount,String requestNo,Long operatorId,String reason){
  if(amount==null||amount.signum()<=0)throw new IllegalArgumentException("冲正金额必须大于零");
  JkCommissionReverse old=reverseDao.selectOne(new LambdaQueryWrapper<JkCommissionReverse>().eq(JkCommissionReverse::getRequestNo,requestNo));if(old!=null)return old;
  JkCommissionRecord record=recordDao.selectById(recordId);if(record==null||Boolean.TRUE.equals(record.getIsDeleted()))throw new IllegalArgumentException("佣金记录不存在");
  BigDecimal priorReversed = priorReversed(recordId); BigDecimal before=CommissionReverseSupport.remaining(record.getCommissionAmount(),priorReversed); CommissionReverseSupport.requireReverseAmount(before,amount);
  if("PENDING_SETTLE".equals(record.getStatus())||"CREATED".equals(record.getStatus())) accountService.reversePending(record.getReceiverUserId(),record.getReceiverRoleCode(),amount,requestNo,"REVERSE_PENDING:"+requestNo);
  else if("SETTLED".equals(record.getStatus())) { JkCommissionAccount account=accountDao.selectOne(new LambdaQueryWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getUserId,record.getReceiverUserId()).eq(JkCommissionAccount::getRoleCode,record.getReceiverRoleCode()).eq(JkCommissionAccount::getIsDeleted,false)); if(account!=null&&account.getSettledAmount()!=null&&account.getSettledAmount().compareTo(amount)>=0) accountService.reverseSettled(record.getReceiverUserId(),record.getReceiverRoleCode(),amount,requestNo,"REVERSE_SETTLED:"+requestNo); else accountService.reverseFrozen(record.getReceiverUserId(),record.getReceiverRoleCode(),amount,requestNo,"REVERSE_FROZEN:"+requestNo); fundAccountService.reverseAvailableCommission(record.getReceiverUserId(),record.getReceiverRoleCode(),amount,recordId,requestNo,"REVERSE_FUND:"+requestNo); }
  else throw new IllegalArgumentException("当前佣金状态需进入负向待抵扣或人工审核后冲正");
  Date now=new Date();BigDecimal after=before.subtract(amount);JkCommissionReverse reverse=new JkCommissionReverse().setReverseNo("RV"+id()).setOriginalCommissionRecordId(recordId).setSourceType(sourceType).setSourceId(sourceId).setSourceNo(sourceNo).setReverseType(reverseType).setReverseAmount(amount).setBeforeAmount(before).setAfterAmount(after).setReason(reason).setStatus("SUCCESS").setRequestNo(requestNo).setOperatorId(operatorId).setCreateTime(now).setUpdateTime(now);reverseDao.insert(reverse);
  if(after.signum()==0)record.setStatus("REVERSED").setUpdateTime(now);recordDao.updateById(record);return reverse;
 }
 private BigDecimal priorReversed(Long recordId) { List<JkCommissionReverse> reverses=reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>().eq(JkCommissionReverse::getOriginalCommissionRecordId,recordId).eq(JkCommissionReverse::getStatus,"SUCCESS")); BigDecimal total=BigDecimal.ZERO; for(JkCommissionReverse reverse:reverses) total=total.add(reverse.getReverseAmount()==null?BigDecimal.ZERO:reverse.getReverseAmount()); return total; }
 private String id(){return UUID.randomUUID().toString().replace("-","").substring(0,16);}
}