package com.zbkj.service.service.impl.jiuzhoukang.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkPhaseSixOverviewResponse;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.jiuzhoukang.report.JkReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

/**
 * 第六阶段概览报表。
 * <p>当前 V1 直接从业务账本聚合，用于验收口径；数据量增大后应切换到日快照/汇总表，不能长期用全表聚合。</p>
 */
@Service
public class JkReportServiceImpl implements JkReportService {
    @Autowired private JkUserBusinessRoleDao roleDao; @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkPlatformOrderDao orderDao; @Autowired private JkStockTransferDao transferDao; @Autowired private JkStockTransferReturnDao returnDao;
    @Autowired private JkCommissionAccountDao commissionAccountDao; @Autowired private JkWithdrawApplyDao withdrawDao;
    @Autowired private JkBusinessEventDao eventDao; @Autowired private JkAccountReconcileRecordDao reconcileDao;
    @Autowired private JkHealthAlertRecordDao healthAlertDao; @Autowired private JkHealthAccessLogDao accessLogDao; @Autowired private JkRiskEventDao riskDao;
    /**
     * 直接从真实业务表聚合概览。
     * <p>该方法适合 V1 验收和低数据量环境；正式运营数据增大后应由定时任务生成日汇总，
     * 否则全表 count/sum 会逐渐影响后台查询性能。</p>
     */
    @Override public JkPhaseSixOverviewResponse overview(){JkPhaseSixOverviewResponse r=new JkPhaseSixOverviewResponse();
        r.setActiveIdentityCount(longValue(roleDao.selectCount(new LambdaQueryWrapper<JkUserBusinessRole>().eq(JkUserBusinessRole::getAuditStatus,"EFFECTIVE").eq(JkUserBusinessRole::getEffectiveStatus,"ENABLED").eq(JkUserBusinessRole::getIsDeleted,false))));
        r.setStockAccountCount(longValue(stockAccountDao.selectCount(new LambdaQueryWrapper<JkStockAccount>().eq(JkStockAccount::getIsDeleted,false))));
        r.setPlatformOrderCount(longValue(orderDao.selectCount(new LambdaQueryWrapper<JkPlatformOrder>().eq(JkPlatformOrder::getIsDeleted,false))));
        r.setStockTransferCount(longValue(transferDao.selectCount(new LambdaQueryWrapper<JkStockTransfer>().eq(JkStockTransfer::getIsDeleted,false))));
        r.setTransferReturnCount(longValue(returnDao.selectCount(new LambdaQueryWrapper<JkStockTransferReturn>().eq(JkStockTransferReturn::getIsDeleted,false))));
        BigDecimal pending=BigDecimal.ZERO;for(JkCommissionAccount a:commissionAccountDao.selectList(new LambdaQueryWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getIsDeleted,false)))pending=pending.add(nvl(a.getPendingSettleAmount()));r.setPendingCommissionAmount(pending);
        BigDecimal withdrawing=BigDecimal.ZERO;for(JkWithdrawApply w:withdrawDao.selectList(new LambdaQueryWrapper<JkWithdrawApply>().in(JkWithdrawApply::getStatus,Arrays.asList("SUBMITTED","APPROVED","PAYING")).eq(JkWithdrawApply::getIsDeleted,false)))withdrawing=withdrawing.add(nvl(w.getAmount()));r.setWithdrawPendingAmount(withdrawing);
        r.setDeadEventCount(longValue(eventDao.selectCount(new LambdaQueryWrapper<JkBusinessEvent>().eq(JkBusinessEvent::getEventStatus,"DEAD"))));
        r.setAccountMismatchCount(longValue(reconcileDao.selectCount(new LambdaQueryWrapper<JkAccountReconcileRecord>().eq(JkAccountReconcileRecord::getReconcileStatus,"MISMATCH"))));
        r.setActiveHealthAlertCount(longValue(healthAlertDao.selectCount(new LambdaQueryWrapper<JkHealthAlertRecord>().in(JkHealthAlertRecord::getStatus,Arrays.asList("OPEN","ACKNOWLEDGED")).eq(JkHealthAlertRecord::getIsDeleted,false))));
        r.setDeniedHealthAccessCount(longValue(accessLogDao.selectCount(new LambdaQueryWrapper<JkHealthAccessLog>().eq(JkHealthAccessLog::getAccessResult,"DENIED").eq(JkHealthAccessLog::getIsDeleted,false))));
        r.setOpenRiskEventCount(longValue(riskDao.selectCount(new LambdaQueryWrapper<JkRiskEvent>().eq(JkRiskEvent::getStatus,"OPEN").eq(JkRiskEvent::getIsDeleted,false))));return r;}
    private long longValue(Integer v){return v==null?0L:v.longValue();} private BigDecimal nvl(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
}
