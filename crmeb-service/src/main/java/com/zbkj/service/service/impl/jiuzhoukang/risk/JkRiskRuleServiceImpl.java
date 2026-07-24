package com.zbkj.service.service.impl.jiuzhoukang.risk;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRiskRuleSaveRequest;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.jiuzhoukang.risk.JkRiskRuleService;
import com.zbkj.service.service.jiuzhoukang.risk.JkRiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 第六阶段可配置风险扫描器。
 * <p>扫描只产生风险事件，不执行冻结、扣款或账户修改；避免阈值尚未由客户确认时误伤业务。</p>
 */
@Service
public class JkRiskRuleServiceImpl implements JkRiskRuleService {
    private static final Set<String> TYPES=new HashSet<String>(Arrays.asList("STOCK_AGING","STOCK_FROZEN_TIMEOUT","STOCK_BATCH_MISMATCH","DEAD_EVENT","ACCOUNT_MISMATCH","HEALTH_DENIED_BURST"));
    @Autowired private JkRiskRuleDao ruleDao;
    @Autowired private JkRiskService riskService;
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkStockBatchReservationDao reservationDao;
    @Autowired private JkBusinessEventDao eventDao;
    @Autowired private JkAccountReconcileRecordDao reconcileDao;
    @Autowired private JkHealthAccessLogDao healthAccessDao;

    @Override public PageInfo<JkRiskRule> list(String keyword,String scannerType,Boolean enabled,PageParamRequest p){
        Page<JkRiskRule> page=PageHelper.startPage(p.getPage(),p.getLimit());
        LambdaQueryWrapper<JkRiskRule> q=new LambdaQueryWrapper<JkRiskRule>().eq(JkRiskRule::getIsDeleted,false).orderByDesc(JkRiskRule::getId);
        if(StrUtil.isNotBlank(keyword))q.and(w->w.like(JkRiskRule::getRuleCode,keyword).or().like(JkRiskRule::getRuleName,keyword));
        if(StrUtil.isNotBlank(scannerType))q.eq(JkRiskRule::getScannerType,scannerType);
        if(enabled!=null)q.eq(JkRiskRule::getEnabled,enabled);
        return CommonPage.copyPageInfo(page,ruleDao.selectList(q));
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public JkRiskRule save(Long operator,JkRiskRuleSaveRequest r){
        String scanner=r.getScannerType().trim().toUpperCase();if(!TYPES.contains(scanner))throw new CrmebException("不支持的风险扫描类型");
        if(!Arrays.asList("LOW","MEDIUM","HIGH","CRITICAL").contains(r.getRiskLevel().toUpperCase()))throw new CrmebException("风险等级不合法");
        JkRiskRule dup=ruleDao.selectOne(new LambdaQueryWrapper<JkRiskRule>().eq(JkRiskRule::getRuleCode,r.getRuleCode()).eq(JkRiskRule::getIsDeleted,false).ne(r.getId()!=null,JkRiskRule::getId,r.getId()).last("limit 1"));
        if(dup!=null)throw new CrmebException("风险规则编码已存在");
        Date now=new Date();JkRiskRule e=r.getId()==null?new JkRiskRule().setCreateUserId(operator).setCreateTime(now).setIsDeleted(false).setVersion(0):ruleDao.selectById(r.getId());
        if(e==null||Boolean.TRUE.equals(e.getIsDeleted()))throw new CrmebException("风险规则不存在");
        e.setRuleCode(r.getRuleCode().trim()).setRuleName(r.getRuleName().trim()).setScannerType(scanner).setRiskType(r.getRiskType().trim().toUpperCase())
                .setRiskLevel(r.getRiskLevel().trim().toUpperCase()).setThresholdValue(r.getThresholdValue()).setWindowHours(r.getWindowHours())
                .setConfigJson(r.getConfigJson()).setEnabled(r.getEnabled()==null||r.getEnabled()).setRemark(r.getRemark()).setUpdateUserId(operator).setUpdateTime(now);
        if(r.getId()==null)ruleDao.insert(e);else ruleDao.updateById(e);return e;
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public JkRiskRule setEnabled(Long operator,Long id,boolean enabled){JkRiskRule e=ruleDao.selectById(id);if(e==null||Boolean.TRUE.equals(e.getIsDeleted()))throw new CrmebException("风险规则不存在");e.setEnabled(enabled).setUpdateUserId(operator).setUpdateTime(new Date());ruleDao.updateById(e);return e;}

    @Override public int runOne(Long operator,Long id){JkRiskRule r=ruleDao.selectById(id);if(r==null||Boolean.TRUE.equals(r.getIsDeleted()))throw new CrmebException("风险规则不存在");return scan(r,operator);}
    @Override public int runEnabled(Long operator,int limit){List<JkRiskRule> rows=ruleDao.selectList(new LambdaQueryWrapper<JkRiskRule>().eq(JkRiskRule::getEnabled,true).eq(JkRiskRule::getIsDeleted,false).orderByAsc(JkRiskRule::getId).last("limit "+Math.max(1,Math.min(limit,100))));int total=0;for(JkRiskRule r:rows){try{total+=scan(r,operator);}catch(Exception ignored){}}return total;}

    private int scan(JkRiskRule r,Long operator){
        Date now=new Date();try{int count;
            if("STOCK_AGING".equals(r.getScannerType()))count=scanStockAging(r,now);
            else if("STOCK_FROZEN_TIMEOUT".equals(r.getScannerType()))count=scanFrozen(r,now);
            else if("STOCK_BATCH_MISMATCH".equals(r.getScannerType()))count=scanStockBatchMismatch(r,now);
            else if("DEAD_EVENT".equals(r.getScannerType()))count=scanDeadEvents(r,now);
            else if("ACCOUNT_MISMATCH".equals(r.getScannerType()))count=scanAccountMismatch(r,now);
            else if("HEALTH_DENIED_BURST".equals(r.getScannerType()))count=scanHealthDenied(r,now);
            else throw new CrmebException("不支持的风险扫描类型");
            r.setLastScanTime(now).setLastScanStatus("SUCCESS").setLastErrorMessage(null).setUpdateUserId(operator).setUpdateTime(new Date());ruleDao.updateById(r);return count;
        }catch(Exception ex){r.setLastScanTime(now).setLastScanStatus("FAILED").setLastErrorMessage(shortText(ex.getMessage())).setUpdateUserId(operator).setUpdateTime(new Date());ruleDao.updateById(r);if(ex instanceof CrmebException)throw (CrmebException)ex;throw new CrmebException("风险规则扫描失败");}
    }

    private int scanStockAging(JkRiskRule r,Date now){int days=intThreshold(r,60);Date cutoff=beforeHours(now,days*24);List<JkStockBatch> rows=batchDao.selectList(new LambdaQueryWrapper<JkStockBatch>().gt(JkStockBatch::getAvailableQty,0).le(JkStockBatch::getInboundTime,cutoff).eq(JkStockBatch::getIsDeleted,false).orderByAsc(JkStockBatch::getInboundTime).last("limit 1000"));int n=0;for(JkStockBatch b:rows){int age=(int)((now.getTime()-b.getInboundTime().getTime())/86400000L);riskService.recordOnce(key(r,b.getId(),now),r.getRiskType(),r.getRiskLevel(),"STOCK_BATCH",b.getId(),b.getBatchNo(),null,"库存批次库龄超过阈值："+age+" 天",json("batchNo",b.getBatchNo(),"availableQty",b.getAvailableQty(),"ageDays",age,"thresholdDays",days));n++;}return n;}
    private int scanFrozen(JkRiskRule r,Date now){int hours=intThreshold(r,168);Date cutoff=beforeHours(now,hours);List<JkStockBatchReservation> rows=reservationDao.selectList(new LambdaQueryWrapper<JkStockBatchReservation>().eq(JkStockBatchReservation::getStatus,"ACTIVE").eq(JkStockBatchReservation::getIsDeleted,false).le(JkStockBatchReservation::getCreateTime,cutoff).orderByAsc(JkStockBatchReservation::getCreateTime).last("limit 1000"));int n=0;for(JkStockBatchReservation x:rows){riskService.recordOnce(key(r,x.getId(),now),r.getRiskType(),r.getRiskLevel(),"STOCK_RESERVATION",x.getId(),x.getBusinessNo(),null,"批次冻结超过阈值且业务尚未出库或释放",json("reservationNo",x.getReservationNo(),"businessType",x.getBusinessType(),"frozenQty",x.getFrozenQty(),"thresholdHours",hours));n++;}return n;}
    private int scanStockBatchMismatch(JkRiskRule r,Date now){List<JkStockItem> items=stockItemDao.selectList(new LambdaQueryWrapper<JkStockItem>().eq(JkStockItem::getIsDeleted,false).last("limit 5000"));Map<String,int[]> batchTotals=new HashMap<String,int[]>();for(JkStockBatch b:batchDao.selectList(new LambdaQueryWrapper<JkStockBatch>().eq(JkStockBatch::getIsDeleted,false))){String k=stockKey(b.getStockAccountId(),b.getProductId(),b.getSkuId());int[] v=batchTotals.get(k);if(v==null){v=new int[2];batchTotals.put(k,v);}v[0]+=b.getAvailableQty()==null?0:b.getAvailableQty();v[1]+=b.getFrozenQty()==null?0:b.getFrozenQty();}int n=0;for(JkStockItem item:items){int[] b=batchTotals.get(stockKey(item.getStockAccountId(),item.getProductId(),item.getSkuId()));int bav=b==null?0:b[0],bfr=b==null?0:b[1],tav=item.getAvailableQty()==null?0:item.getAvailableQty(),tfr=item.getFrozenQty()==null?0:item.getFrozenQty();if(tav==bav&&tfr==bfr)continue;riskService.recordOnce(key(r,item.getId(),now),r.getRiskType(),r.getRiskLevel(),"STOCK_ITEM",item.getId(),String.valueOf(item.getId()),null,"库存总账与批次账不一致",json("stockAccountId",item.getStockAccountId(),"productId",item.getProductId(),"skuId",item.getSkuId(),"totalAvailable",tav,"batchAvailable",bav,"totalFrozen",tfr,"batchFrozen",bfr));n++;}return n;}
    private int scanDeadEvents(JkRiskRule r,Date now){List<JkBusinessEvent> rows=eventDao.selectList(new LambdaQueryWrapper<JkBusinessEvent>().eq(JkBusinessEvent::getEventStatus,"DEAD").orderByAsc(JkBusinessEvent::getId).last("limit 1000"));int n=0;for(JkBusinessEvent e:rows){riskService.recordOnce(key(r,e.getId(),now),r.getRiskType(),r.getRiskLevel(),"BUSINESS_EVENT",e.getId(),e.getBusinessNo(),null,"业务事件重试耗尽，需要人工补偿",json("eventType",e.getEventType(),"eventKey",e.getEventKey(),"retryCount",e.getRetryCount()));n++;}return n;}
    private int scanAccountMismatch(JkRiskRule r,Date now){List<JkAccountReconcileRecord> rows=reconcileDao.selectList(new LambdaQueryWrapper<JkAccountReconcileRecord>().eq(JkAccountReconcileRecord::getReconcileStatus,"DIFFERENCE").orderByDesc(JkAccountReconcileRecord::getId).last("limit 1000"));int n=0;for(JkAccountReconcileRecord x:rows){riskService.recordOnce(key(r,x.getId(),now),r.getRiskType(),r.getRiskLevel(),"ACCOUNT_RECONCILE",x.getId(),x.getBatchNo(),x.getUserId(),"佣金账户与资金账户对账存在差异",json("issueSummary",x.getIssueSummary(),"crossAccountDifference",x.getCrossAccountDifference()));n++;}return n;}
    private int scanHealthDenied(JkRiskRule r,Date now){int hours=r.getWindowHours()==null?24:Math.max(1,r.getWindowHours()),threshold=intThreshold(r,5);Date start=beforeHours(now,hours);List<JkHealthAccessLog> rows=healthAccessDao.selectList(new LambdaQueryWrapper<JkHealthAccessLog>().eq(JkHealthAccessLog::getAccessResult,"DENIED").ge(JkHealthAccessLog::getAccessTime,start).eq(JkHealthAccessLog::getIsDeleted,false).orderByAsc(JkHealthAccessLog::getId).last("limit 5000"));Map<String,List<JkHealthAccessLog>> groups=new HashMap<String,List<JkHealthAccessLog>>();for(JkHealthAccessLog x:rows){String k=(x.getAdminId()!=null?"ADMIN:"+x.getAdminId():"USER:"+x.getViewerUserId());groups.computeIfAbsent(k,z->new ArrayList<JkHealthAccessLog>()).add(x);}int n=0;for(Map.Entry<String,List<JkHealthAccessLog>> en:groups.entrySet()){if(en.getValue().size()<threshold)continue;JkHealthAccessLog first=en.getValue().get(0);Long viewer=first.getViewerUserId();riskService.recordOnce(r.getRuleCode()+":"+en.getKey()+":"+period(now,hours),r.getRiskType(),r.getRiskLevel(),"HEALTH_ACCESS",first.getId(),en.getKey(),viewer,"短时间内多次健康数据越权访问",json("deniedCount",en.getValue().size(),"windowHours",hours,"threshold",threshold,"adminId",first.getAdminId(),"viewerUserId",viewer));n++;}return n;}

    private String stockKey(Long account,Integer product,Integer sku){return account+"|"+product+"|"+(sku==null?0:sku);}
    private int intThreshold(JkRiskRule r,int def){BigDecimal v=r.getThresholdValue();return v==null?def:Math.max(1,v.intValue());}
    private Date beforeHours(Date d,int hours){return new Date(d.getTime()-hours*3600000L);}
    private String key(JkRiskRule r,Long sourceId,Date now){return r.getRuleCode()+":"+sourceId+":"+new SimpleDateFormat("yyyyMMdd").format(now);}
    private String period(Date now,int hours){long bucket=now.getTime()/(Math.max(1,hours)*3600000L);return String.valueOf(bucket);}
    private String json(Object... pairs){JSONObject o=new JSONObject(true);for(int i=0;i+1<pairs.length;i+=2)o.put(String.valueOf(pairs[i]),pairs[i+1]);return JSON.toJSONString(o);}
    private String shortText(String s){if(s==null)return "未知错误";s=s.replace('\n',' ').replace('\r',' ');return s.length()>500?s.substring(0,500):s;}
}
