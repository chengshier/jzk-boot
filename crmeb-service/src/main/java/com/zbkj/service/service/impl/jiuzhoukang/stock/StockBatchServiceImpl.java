package com.zbkj.service.service.impl.jiuzhoukang.stock;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.*;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockBatchUpdateRequest;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.service.dao.StoreProductDao;
import com.zbkj.service.dao.StoreProductAttrValueDao;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.jiuzhoukang.stock.StockBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * 批次库存和 FIFO 分配实现。
 * <p>库存明细保存总量，批次表保存总量的组成；任一批次动作失败会回滚同事务中的库存总账动作。</p>
 */
@Service
public class StockBatchServiceImpl implements StockBatchService {
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private JkStockBatchFlowDao flowDao;
    @Autowired private JkStockBatchReservationDao reservationDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private StoreProductDao productDao;
    @Autowired private StoreProductAttrValueDao skuDao;

    @Override @Transactional(rollbackFor=Exception.class)
    public void freeze(JkStockActionRequest r) {
        int remaining=r.getQuantity();
        LambdaQueryWrapper<JkStockBatch> batchQuery=new LambdaQueryWrapper<JkStockBatch>()
                .eq(JkStockBatch::getStockAccountId,r.getStockAccountId()).eq(JkStockBatch::getProductId,r.getProductId())
                .gt(JkStockBatch::getAvailableQty,0).eq(JkStockBatch::getStatus,"ACTIVE").eq(JkStockBatch::getIsDeleted,false)
                .orderByAsc(JkStockBatch::getExpireTime).orderByAsc(JkStockBatch::getInboundTime).orderByAsc(JkStockBatch::getId).last("for update");
        applySku(batchQuery,r.getSkuId());
        List<JkStockBatch> batches=batchDao.selectList(batchQuery);
        for(JkStockBatch b:batches){if(remaining<=0)break;int take=Math.min(remaining,nvl(b.getAvailableQty()));int beforeA=nvl(b.getAvailableQty()),beforeF=nvl(b.getFrozenQty());
            int updated=batchDao.update(null,new UpdateWrapper<JkStockBatch>().eq("id",b.getId()).ge("available_qty",take).setSql("available_qty=available_qty-"+take+", frozen_qty=frozen_qty+"+take+", version=version+1, update_time=NOW()"));
            if(updated!=1)throw new CrmebException("批次库存并发变化，请重试");
            JkStockBatchReservation res=findReservation(r,b.getId());if(res==null){res=new JkStockBatchReservation().setReservationNo("BR"+IdWorker.getIdStr()).setBusinessType(r.getBusinessType()).setBusinessId(r.getBusinessId()).setBusinessNo(r.getBusinessNo()).setBatchId(b.getId()).setStockAccountId(r.getStockAccountId()).setProductId(r.getProductId()).setSkuId(r.getSkuId()).setFrozenQty(take).setReleasedQty(0).setOutboundQty(0).setStatus("ACTIVE").setIsDeleted(false).setCreateUserId(r.getOperatorUserId()).setCreateTime(new Date()).setUpdateTime(new Date()).setVersion(0);reservationDao.insert(res);}else{reservationDao.update(null,new UpdateWrapper<JkStockBatchReservation>().eq("id",res.getId()).setSql("frozen_qty=frozen_qty+"+take+", version=version+1, update_time=NOW()"));}
            writeFlow(r,b,b.getId(),"FREEZE",take,beforeA,beforeA-take,beforeF,beforeF+take,"FREEZE:"+b.getId());remaining-=take;}
        if(remaining>0)throw new CrmebException("可用批次库存不足；请先执行库存批次初始化或补充入库批次");
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public void release(JkStockActionRequest r){consumeReservation(r,false);}
    @Override @Transactional(rollbackFor=Exception.class)
    public void outbound(JkStockActionRequest r){consumeReservation(r,true);}

    private void consumeReservation(JkStockActionRequest r,boolean outbound){int remaining=r.getQuantity();LambdaQueryWrapper<JkStockBatchReservation> reservationQuery=new LambdaQueryWrapper<JkStockBatchReservation>().eq(JkStockBatchReservation::getBusinessType,r.getBusinessType()).eq(JkStockBatchReservation::getBusinessId,r.getBusinessId()).eq(JkStockBatchReservation::getStockAccountId,r.getStockAccountId()).eq(JkStockBatchReservation::getProductId,r.getProductId()).eq(JkStockBatchReservation::getIsDeleted,false).orderByAsc(JkStockBatchReservation::getId).last("for update");applyReservationSku(reservationQuery,r.getSkuId());List<JkStockBatchReservation> rows=reservationDao.selectList(reservationQuery);
        for(JkStockBatchReservation res:rows){if(remaining<=0)break;int unused=nvl(res.getFrozenQty())-nvl(res.getReleasedQty())-nvl(res.getOutboundQty());if(unused<=0)continue;int take=Math.min(remaining,unused);JkStockBatch b=batchDao.selectOne(new LambdaQueryWrapper<JkStockBatch>().eq(JkStockBatch::getId,res.getBatchId()).last("for update"));if(b==null)throw new CrmebException("批次冻结记录对应的批次不存在");int beforeA=nvl(b.getAvailableQty()),beforeF=nvl(b.getFrozenQty());String sql=outbound?"frozen_qty=frozen_qty-"+take+", outbound_qty=outbound_qty+"+take:"frozen_qty=frozen_qty-"+take+", available_qty=available_qty+"+take;int updated=batchDao.update(null,new UpdateWrapper<JkStockBatch>().eq("id",b.getId()).ge("frozen_qty",take).setSql(sql+", version=version+1, update_time=NOW()"));if(updated!=1)throw new CrmebException("批次冻结数量不足");
            String field=outbound?"outbound_qty":"released_qty";reservationDao.update(null,new UpdateWrapper<JkStockBatchReservation>().eq("id",res.getId()).setSql(field+"="+field+"+"+take+", status=CASE WHEN frozen_qty<=released_qty+outbound_qty+"+take+" THEN 'FINISHED' ELSE 'ACTIVE' END, version=version+1, update_time=NOW()"));
            writeFlow(r,b,b.getId(),outbound?"OUTBOUND":"RELEASE",take,beforeA,outbound?beforeA:beforeA+take,beforeF,beforeF-take,(outbound?"OUTBOUND:":"RELEASE:")+b.getId());remaining-=take;}
        if(remaining>0)throw new CrmebException("未找到足够的业务批次冻结记录；升级前必须先完成历史冻结单据");}

    @Override @Transactional(rollbackFor=Exception.class)
    public void inbound(JkStockActionRequest r){
        LambdaQueryWrapper<JkStockBatchFlow> sourceQuery=new LambdaQueryWrapper<JkStockBatchFlow>().eq(JkStockBatchFlow::getBusinessType,r.getBusinessType()).eq(JkStockBatchFlow::getBusinessId,r.getBusinessId()).eq(JkStockBatchFlow::getProductId,r.getProductId()).eq(JkStockBatchFlow::getFlowType,"OUTBOUND").eq(JkStockBatchFlow::getIsDeleted,false).orderByAsc(JkStockBatchFlow::getId);applyFlowSku(sourceQuery,r.getSkuId());List<JkStockBatchFlow> sources=flowDao.selectList(sourceQuery);
        int remaining=r.getQuantity();
        if(!sources.isEmpty())for(JkStockBatchFlow sf:sources){if(remaining<=0)break;int take=Math.min(remaining,sf.getChangeQty());JkStockBatch source=batchDao.selectById(sf.getBatchId());if(source==null)throw new CrmebException("来源批次不存在");inboundOne(r,source.getRootBatchNo(),source.getId(),source.getProductionDate(),source.getExpireTime(),source.getUnitCost(),take);remaining-=take;}
        if(remaining>0){String no=r.getBatchNo();if(no==null||no.trim().isEmpty())no="B-"+r.getBusinessType()+"-"+r.getBusinessId()+"-"+r.getProductId()+"-"+(r.getSkuId()==null?0:r.getSkuId());inboundOne(r,no,null,r.getProductionDate(),r.getExpireTime(),r.getUnitCost(),remaining);}
    }

    private void inboundOne(JkStockActionRequest r,String rootNo,Long sourceBatchId,Date production,Date expire,java.math.BigDecimal cost,int qty){String localNo=rootNo;JkStockBatch b=batchDao.selectOne(new LambdaQueryWrapper<JkStockBatch>().eq(JkStockBatch::getStockAccountId,r.getStockAccountId()).eq(JkStockBatch::getBatchNo,localNo).eq(JkStockBatch::getIsDeleted,false).last("limit 1 for update"));if(b==null){b=new JkStockBatch().setBatchNo(localNo).setRootBatchNo(rootNo).setSourceBatchId(sourceBatchId).setStockAccountId(r.getStockAccountId()).setProductId(r.getProductId()).setSkuId(r.getSkuId()).setSkuCode(r.getSkuCode()).setInboundQty(0).setAvailableQty(0).setFrozenQty(0).setOutboundQty(0).setUnitCost(cost).setProductionDate(production).setExpireTime(expire).setInboundTime(new Date()).setSourceType(r.getBusinessType()).setSourceId(r.getBusinessId()).setSourceNo(r.getBusinessNo()).setStatus("ACTIVE").setIsDeleted(false).setCreateUserId(r.getOperatorUserId()).setUpdateUserId(r.getOperatorUserId()).setCreateTime(new Date()).setUpdateTime(new Date()).setVersion(0);try{batchDao.insert(b);}catch(DuplicateKeyException ignored){b=batchDao.selectOne(new LambdaQueryWrapper<JkStockBatch>().eq(JkStockBatch::getStockAccountId,r.getStockAccountId()).eq(JkStockBatch::getBatchNo,localNo).eq(JkStockBatch::getIsDeleted,false).last("limit 1 for update"));}}
        int before=nvl(b.getAvailableQty());String idem="BATCH:"+r.getBusinessType()+":"+r.getBusinessId()+":INBOUND:"+r.getStockAccountId()+":"+r.getProductId()+":"+(r.getSkuId()==null?0:r.getSkuId())+":"+(sourceBatchId==null?0:sourceBatchId);JkStockBatchFlow exists=flowDao.selectOne(new LambdaQueryWrapper<JkStockBatchFlow>().eq(JkStockBatchFlow::getIdempotencyKey,idem).last("limit 1"));if(exists!=null)return;int updated=batchDao.update(null,new UpdateWrapper<JkStockBatch>().eq("id",b.getId()).setSql("inbound_qty=inbound_qty+"+qty+", available_qty=available_qty+"+qty+", version=version+1, update_time=NOW()"));if(updated!=1)throw new CrmebException("批次入库失败");writeFlowWithKey(r,b,sourceBatchId,"INBOUND",qty,before,before+qty,nvl(b.getFrozenQty()),nvl(b.getFrozenQty()),idem);}

    @Override
    public PageInfo<JkStockBatch> list(Long accountId,Integer productId,Integer skuId,String agingLevel,PageParamRequest pageParam){
        Page<JkStockBatch> page=PageHelper.startPage(pageParam.getPage(),pageParam.getLimit());
        LambdaQueryWrapper<JkStockBatch> q=new LambdaQueryWrapper<JkStockBatch>().eq(JkStockBatch::getIsDeleted,false).orderByAsc(JkStockBatch::getExpireTime).orderByAsc(JkStockBatch::getInboundTime);
        if(accountId!=null)q.eq(JkStockBatch::getStockAccountId,accountId);if(productId!=null)q.eq(JkStockBatch::getProductId,productId);if(skuId!=null)q.eq(JkStockBatch::getSkuId,skuId);
        List<JkStockBatch> rows=batchDao.selectList(q);Map<Long,JkStockAccount> accounts=new HashMap<Long,JkStockAccount>();Map<Integer,StoreProduct> products=new HashMap<Integer,StoreProduct>();Map<Integer,StoreProductAttrValue> skus=new HashMap<Integer,StoreProductAttrValue>();
        for(JkStockBatch b:rows){int age=ageDays(b.getInboundTime());b.setAgeDays(age).setAgeLevel(ageLevel(age));JkStockAccount a=accounts.get(b.getStockAccountId());if(a==null){a=stockAccountDao.selectById(b.getStockAccountId());if(a!=null)accounts.put(a.getId(),a);}b.setAccountName(a==null?null:a.getOwnerName());StoreProduct product=products.get(b.getProductId());if(product==null){product=productDao.selectById(b.getProductId());if(product!=null)products.put(product.getId(),product);}b.setProductName(product==null?null:product.getStoreName());if(b.getSkuId()!=null){StoreProductAttrValue sku=skus.get(b.getSkuId());if(sku==null){sku=skuDao.selectById(b.getSkuId());if(sku!=null)skus.put(sku.getId(),sku);}b.setSkuName(sku==null?b.getSkuCode():sku.getSuk());}else b.setSkuName("单规格");}
        if(agingLevel!=null&&!agingLevel.trim().isEmpty()){Iterator<JkStockBatch> it=rows.iterator();while(it.hasNext())if(!agingLevel.equals(it.next().getAgeLevel()))it.remove();}
        return CommonPage.copyPageInfo(page,rows);
    }

    @Override @Transactional(rollbackFor=Exception.class)
    public JkStockBatch updateMetadata(Long operatorId,JkStockBatchUpdateRequest request){JkStockBatch b=batchDao.selectById(request.getId());if(b==null||Boolean.TRUE.equals(b.getIsDeleted()))throw new CrmebException("库存批次不存在");if(request.getUnitCost()!=null&&request.getUnitCost().signum()<0)throw new CrmebException("批次成本不能为负数");if(request.getProductionDate()!=null&&request.getExpireTime()!=null&&!request.getExpireTime().after(request.getProductionDate()))throw new CrmebException("有效期必须晚于生产日期");b.setUnitCost(request.getUnitCost()).setProductionDate(request.getProductionDate()).setExpireTime(request.getExpireTime()).setUpdateUserId(operatorId).setUpdateTime(new Date());batchDao.updateById(b);return b;}

    @Override @Transactional(rollbackFor=Exception.class)
    public int openingFromStockItems(Long operatorId){List<JkStockItem> items=stockItemDao.selectList(new LambdaQueryWrapper<JkStockItem>().gt(JkStockItem::getAvailableQty,0).eq(JkStockItem::getFrozenQty,0).eq(JkStockItem::getIsDeleted,false));int count=0;for(JkStockItem item:items){String no="OPENING-"+item.getStockAccountId()+"-"+item.getProductId()+"-"+(item.getSkuId()==null?0:item.getSkuId());JkStockBatch exists=batchDao.selectOne(new LambdaQueryWrapper<JkStockBatch>().eq(JkStockBatch::getStockAccountId,item.getStockAccountId()).eq(JkStockBatch::getBatchNo,no).last("limit 1"));if(exists!=null)continue;JkStockBatch b=new JkStockBatch().setBatchNo(no).setRootBatchNo(no).setStockAccountId(item.getStockAccountId()).setProductId(item.getProductId()).setSkuId(item.getSkuId()).setSkuCode(item.getSkuCode()).setInboundQty(item.getAvailableQty()).setAvailableQty(item.getAvailableQty()).setFrozenQty(0).setOutboundQty(0).setInboundTime(new Date()).setSourceType("OPENING").setSourceId(item.getId()).setSourceNo(item.getBusinessNo()).setStatus("ACTIVE").setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId).setCreateTime(new Date()).setUpdateTime(new Date()).setVersion(0);batchDao.insert(b);count++;}return count;}

    @Override public int ageDays(Date inbound){if(inbound==null)return 0;return (int)Math.max(0,(System.currentTimeMillis()-inbound.getTime())/(24L*3600L*1000L));}
    private String ageLevel(int days){if(days>=90)return "SERIOUS";if(days>=60)return "WARNING";if(days>=30)return "ATTENTION";return "NORMAL";}
    private JkStockBatchReservation findReservation(JkStockActionRequest r,Long batchId){return reservationDao.selectOne(new LambdaQueryWrapper<JkStockBatchReservation>().eq(JkStockBatchReservation::getBusinessType,r.getBusinessType()).eq(JkStockBatchReservation::getBusinessId,r.getBusinessId()).eq(JkStockBatchReservation::getBatchId,batchId).eq(JkStockBatchReservation::getIsDeleted,false).last("limit 1"));}
    private void writeFlow(JkStockActionRequest r,JkStockBatch b,Long source,String type,int qty,int ba,int aa,int bf,int af,String suffix){String key="BATCH:"+r.getBusinessType()+":"+r.getBusinessId()+":"+suffix+":"+r.getStockAccountId()+":"+r.getProductId()+":"+(r.getSkuId()==null?0:r.getSkuId());writeFlowWithKey(r,b,source,type,qty,ba,aa,bf,af,key);}
    private void writeFlowWithKey(JkStockActionRequest r,JkStockBatch b,Long source,String type,int qty,int ba,int aa,int bf,int af,String key){JkStockBatchFlow f=new JkStockBatchFlow().setFlowNo("BF"+IdWorker.getIdStr()).setIdempotencyKey(key).setBatchId(b.getId()).setSourceBatchId(source).setBusinessType(r.getBusinessType()).setBusinessId(r.getBusinessId()).setBusinessNo(r.getBusinessNo()).setStockAccountId(r.getStockAccountId()).setProductId(r.getProductId()).setSkuId(r.getSkuId()).setFlowType(type).setChangeQty(qty).setBeforeAvailableQty(ba).setAfterAvailableQty(aa).setBeforeFrozenQty(bf).setAfterFrozenQty(af).setRemark(r.getRemark()).setIsDeleted(false).setCreateUserId(r.getOperatorUserId()).setCreateTime(new Date());try{flowDao.insert(f);}catch(DuplicateKeyException ignored){}}
    private void applySku(LambdaQueryWrapper<JkStockBatch> q,Integer skuId){if(skuId==null)q.isNull(JkStockBatch::getSkuId);else q.eq(JkStockBatch::getSkuId,skuId);}
    private void applyReservationSku(LambdaQueryWrapper<JkStockBatchReservation> q,Integer skuId){if(skuId==null)q.isNull(JkStockBatchReservation::getSkuId);else q.eq(JkStockBatchReservation::getSkuId,skuId);}
    private void applyFlowSku(LambdaQueryWrapper<JkStockBatchFlow> q,Integer skuId){if(skuId==null)q.isNull(JkStockBatchFlow::getSkuId);else q.eq(JkStockBatchFlow::getSkuId,skuId);}
    private int nvl(Integer v){return v==null?0:v;}
}
