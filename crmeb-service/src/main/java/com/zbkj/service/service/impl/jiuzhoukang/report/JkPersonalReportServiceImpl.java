package com.zbkj.service.service.impl.jiuzhoukang.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.response.jiuzhoukang.JkPersonalReportResponse;
import com.zbkj.service.dao.StoreOrderDao;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.jiuzhoukang.report.JkPersonalReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

/** 个人经营报表严格按 userId 和订单归属快照查询，不接受前端传入他人用户 ID。 */
@Service
public class JkPersonalReportServiceImpl implements JkPersonalReportService {
    @Autowired private StoreOrderDao orderDao; @Autowired private JkRetailOrderAttributionDao attributionDao;
    @Autowired private JkRetailRefundAdjustmentDao refundAdjustmentDao;
    @Autowired private JkStockTransferDao transferDao; @Autowired private JkStockTransferReturnDao returnDao;
    @Autowired private JkStockAccountDao accountDao; @Autowired private JkStockItemDao itemDao;
    @Autowired private JkCommissionAccountDao commissionDao; @Autowired private JkFundAccountDao fundDao;
    @Override public JkPersonalReportResponse summary(Long userId,Date startDate,Date endDate){Date s=startDate==null?new Date(0):start(startDate),e=endDate==null?new Date():next(start(endDate));Set<Long> orderIds=new HashSet<Long>();for(StoreOrder o:orderDao.selectList(new LambdaQueryWrapper<StoreOrder>().eq(StoreOrder::getStatus,3).ge(StoreOrder::getUpdateTime,s).lt(StoreOrder::getUpdateTime,e)))orderIds.add(Long.valueOf(o.getId()));BigDecimal retail=BigDecimal.ZERO;long orderCount=0;if(!orderIds.isEmpty()){Set<Long> distinct=new HashSet<Long>();for(JkRetailOrderAttribution a:attributionDao.selectList(new LambdaQueryWrapper<JkRetailOrderAttribution>().in(JkRetailOrderAttribution::getOrderId,orderIds).eq(JkRetailOrderAttribution::getReceiverUserId,userId).eq(JkRetailOrderAttribution::getIsDeleted,false))){retail=retail.add(nvl(a.getItemPaidAmount()));distinct.add(a.getOrderId());}orderCount=distinct.size();}BigDecimal retailRefund=BigDecimal.ZERO;
        for(JkRetailRefundAdjustment adjustment:refundAdjustmentDao.selectList(new LambdaQueryWrapper<JkRetailRefundAdjustment>()
                .eq(JkRetailRefundAdjustment::getReceiverUserId,userId).ge(JkRetailRefundAdjustment::getOccurredTime,s)
                .lt(JkRetailRefundAdjustment::getOccurredTime,e).eq(JkRetailRefundAdjustment::getIsDeleted,false))) {
            retailRefund=retailRefund.add(nvl(adjustment.getAdjustmentAmount()));
        }
        BigDecimal transfer=BigDecimal.ZERO;for(JkStockTransfer t:transferDao.selectList(new LambdaQueryWrapper<JkStockTransfer>().eq(JkStockTransfer::getUserId,userId).eq(JkStockTransfer::getStatus,"STOCK_IN").ge(JkStockTransfer::getUpdateTime,s).lt(JkStockTransfer::getUpdateTime,e).eq(JkStockTransfer::getIsDeleted,false)))transfer=transfer.add(nvl(t.getTotalAmount()));BigDecimal returned=BigDecimal.ZERO;for(JkStockTransferReturn r:returnDao.selectList(new LambdaQueryWrapper<JkStockTransferReturn>().eq(JkStockTransferReturn::getUserId,userId).eq(JkStockTransferReturn::getStatus,"COMPLETED").ge(JkStockTransferReturn::getUpdateTime,s).lt(JkStockTransferReturn::getUpdateTime,e).eq(JkStockTransferReturn::getIsDeleted,false)))returned=returned.add(nvl(r.getReturnAmount()));int av=0,fr=0;List<JkStockAccount> accounts=accountDao.selectList(new LambdaQueryWrapper<JkStockAccount>().eq(JkStockAccount::getOwnerUserId,userId).eq(JkStockAccount::getIsDeleted,false));if(!accounts.isEmpty()){List<Long> ids=new ArrayList<Long>();for(JkStockAccount a:accounts)ids.add(a.getId());for(JkStockItem i:itemDao.selectList(new LambdaQueryWrapper<JkStockItem>().in(JkStockItem::getStockAccountId,ids).eq(JkStockItem::getIsDeleted,false))){av+=i.getAvailableQty()==null?0:i.getAvailableQty();fr+=i.getFrozenQty()==null?0:i.getFrozenQty();}}JkCommissionAccount ca=commissionDao.selectOne(new LambdaQueryWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getUserId,userId).eq(JkCommissionAccount::getIsDeleted,false).last("limit 1"));JkFundAccount fa=fundDao.selectOne(new LambdaQueryWrapper<JkFundAccount>().eq(JkFundAccount::getUserId,userId).eq(JkFundAccount::getIsDeleted,false).last("limit 1"));return new JkPersonalReportResponse().setRetailPerformanceAmount(retail).setRetailRefundAmount(retailRefund).setRetailNetPerformanceAmount(retail.subtract(retailRefund)).setRetailOrderCount(orderCount).setTransferInboundAmount(transfer).setTransferReturnAmount(returned).setStockAvailableQty(av).setStockFrozenQty(fr).setPendingCommissionAmount(ca==null?BigDecimal.ZERO:nvl(ca.getPendingSettleAmount())).setSettledCommissionAmount(ca==null?BigDecimal.ZERO:nvl(ca.getSettledAmount())).setAvailableFundAmount(fa==null?BigDecimal.ZERO:nvl(fa.getAvailableAmount())).setWithdrawingAmount(fa==null?BigDecimal.ZERO:nvl(fa.getWithdrawingAmount())).setWithdrawnAmount(fa==null?BigDecimal.ZERO:nvl(fa.getWithdrawnAmount()));}
    private Date start(Date d){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));c.setTime(d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}private Date next(Date d){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));c.setTime(d);c.add(Calendar.DAY_OF_MONTH,1);return c.getTime();}private BigDecimal nvl(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
}
