package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.service.jiuzhoukang.commission.*;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/** 事件适配层：仅零售完成正式计算；其余来源按本阶段保守口径保留入口。 */
@Service public class CommissionTriggerServiceImpl implements CommissionTriggerService {
 @Autowired private CommissionCalculateService calculateService; @Autowired private CommissionReverseService reverseService; @Autowired private CommissionFreezeService freezeService; @Autowired private JkCommissionRecordDao recordDao; @Autowired private JkCommissionAccountDao accountDao; @Autowired private JkAuditLogService auditLogService;
 @Override public void onRetailOrderCompleted(Long orderId,String orderNo,Long orderInfoId,Long receiverUserId,String role,BigDecimal amount,String requestNo){calculateService.calculateRetailOrder(orderId,orderNo,orderInfoId,receiverUserId,role,amount,requestNo);}
 @Override public void onPlatformOrderStockIn(Long id,String no,String requestNo){recordPerformanceEvent("PLATFORM_ORDER_STOCK_IN",id,no,requestNo,"平台订货入库完成：本阶段只留业绩事件，不生成可提现佣金");}
 @Override public void onStockTransferCompleted(Long id,String no,String requestNo){recordPerformanceEvent("STOCK_TRANSFER_COMPLETED",id,no,requestNo,"库存调拨完成：本阶段只留业绩事件，不写死上级佣金、差价或团队奖励");}
 @Override @Transactional public void onRefundCompleted(Long id,String no,String requestNo){reverseBySource("RETAIL_ORDER",id,no,"REFUND",requestNo);}
 @Override @Transactional public void onTransferReturnCompleted(Long id,String no,String requestNo){reverseBySource("STOCK_TRANSFER",id,no,"TRANSFER_RETURN",requestNo);}
 @Override @Transactional public void onIdentityFrozen(Long userId,String requestNo){for(JkCommissionAccount a:accountDao.selectList(new LambdaQueryWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getUserId,userId).eq(JkCommissionAccount::getIsDeleted,false))){if(a.getSettledAmount()!=null&&a.getSettledAmount().signum()>0)freezeService.freezeCommission(userId,a.getRoleCode(),a.getSettledAmount(),"IDENTITY_FROZEN","IDENTITY",userId,requestNo,"IDENTITY_FROZEN:"+userId+":"+a.getRoleCode(),"身份冻结");}}
 private void recordPerformanceEvent(String type,Long id,String no,String requestNo,String remark){if(auditLogService.getOne(new LambdaQueryWrapper<JkAuditLog>().eq(JkAuditLog::getBusinessType,type).eq(JkAuditLog::getRequestNo,requestNo))!=null)return;auditLogService.saveAuditLog(new JkAuditLog().setBusinessType(type).setBusinessId(id).setBusinessNo(no).setRequestNo(requestNo).setAuditAction("EVENT_RECORDED").setAuditRemark(remark).setOperateSource("COMMISSION_TRIGGER").setStatus(true).setIsDeleted(false).setCreateTime(new java.util.Date()).setUpdateTime(new java.util.Date()).setTenantId("000000"));}
 private void reverseBySource(String sourceType,Long sourceId,String sourceNo,String reverseType,String requestNo){for(JkCommissionRecord r:recordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>().eq(JkCommissionRecord::getSourceType,sourceType).eq(JkCommissionRecord::getSourceNo,sourceNo).eq(JkCommissionRecord::getIsDeleted,false))){reverseService.reverse(r.getId(),sourceType,sourceId,sourceNo,reverseType,r.getCommissionAmount(),requestNo+":"+r.getId(),null,"来源单据冲正");}}
}