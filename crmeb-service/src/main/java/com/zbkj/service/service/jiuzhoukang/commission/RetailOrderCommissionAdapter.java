package com.zbkj.service.service.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.order.StoreOrderInfo;
import com.zbkj.service.service.StoreOrderInfoService;
import java.math.BigDecimal;
import java.util.List;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** CRMEB 订单完成后的只读适配层；不改订单、支付、原分销或库存。 */
@Service public class RetailOrderCommissionAdapter {
 private static final Logger logger=LoggerFactory.getLogger(RetailOrderCommissionAdapter.class);
 @Autowired private JkAgentRelationDao relationDao; @Autowired private JkUserContextService contextService; @Autowired private CommissionTriggerService triggerService; @Autowired private StoreOrderInfoService orderInfoService;
 public void afterCrmebOrderCompleted(StoreOrder order){
  if(order==null||order.getId()==null||order.getUid()==null||order.getPayPrice()==null||order.getPayPrice().signum()<=0)return;
  JkAgentRelation relation=relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>().eq(JkAgentRelation::getUserId,order.getUid().longValue()).eq(JkAgentRelation::getStatus,true).eq(JkAgentRelation::getIsDeleted,false).isNotNull(JkAgentRelation::getParentUserId).orderByDesc(JkAgentRelation::getId).last("limit 1"));
  if(relation==null)return;
  JkUserContext receiver=contextService.getFrontContext(relation.getParentUserId());
  if(receiver==null||receiver.getPrimaryRoleCode()==null||receiver.getFreezeStatus())return;
  if(!"maker".equals(receiver.getPrimaryRoleCode())&&!"partner".equals(receiver.getPrimaryRoleCode())&&!"county_agent".equals(receiver.getPrimaryRoleCode()))return;
List<StoreOrderInfo> orderInfos=orderInfoService.getListByOrderNo(order.getOrderId());
  if(orderInfos==null||orderInfos.isEmpty())return;
  for(StoreOrderInfo info:orderInfos){BigDecimal itemAmount=(info.getPrice()==null?BigDecimal.ZERO:info.getPrice()).multiply(BigDecimal.valueOf(info.getPayNum()==null?0:info.getPayNum()));if(itemAmount.signum()>0)triggerService.onRetailOrderCompleted(order.getId().longValue(),order.getOrderId(),info.getId().longValue(),relation.getParentUserId(),receiver.getPrimaryRoleCode(),itemAmount,"RETAIL_ORDER_COMPLETED:"+order.getId()+":"+info.getId());}
  logger.info("九州康零售订单佣金事件已提交，orderId={}, receiverUserId={}",order.getId(),relation.getParentUserId());
 }
}