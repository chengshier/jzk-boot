package com.zbkj.service.service.impl.jiuzhoukang.stock;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkStockFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.service.dao.jiuzhoukang.JkStockFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.service.jiuzhoukang.stock.StockActionKey;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockFlowServiceImpl implements StockFlowService {
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkStockFlowDao stockFlowDao;

    @Override @Transactional(rollbackFor = Exception.class)
    public void freezeStock(JkStockActionRequest request) { change(request, "FREEZE", "available_qty = available_qty - {q}, frozen_qty = frozen_qty + {q}", "available_qty >= {q}"); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void releaseFrozenStock(JkStockActionRequest request) { change(request, "RELEASE", "available_qty = available_qty + {q}, frozen_qty = frozen_qty - {q}", "frozen_qty >= {q}"); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void outboundFrozenStock(JkStockActionRequest request) { change(request, "OUTBOUND", "frozen_qty = frozen_qty - {q}, total_out_qty = total_out_qty + {q}", "frozen_qty >= {q}"); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void inboundStock(JkStockActionRequest request) { change(request, "INBOUND", "available_qty = available_qty + {q}, total_in_qty = total_in_qty + {q}", null); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void writeStockFlow(JkStockActionRequest request, String actionType) { change(request, actionType, null, null); }

    private void change(JkStockActionRequest request, String action, String setTemplate, String conditionTemplate) {
        validate(request, action);
        String key = StockActionKey.build(request.getBusinessType(), request.getBusinessId(), action, request.getStockAccountId(), request.getProductId(), request.getSkuId());
        JkStockItem item = findItem(request);
        if (item == null && "INBOUND".equals(action)) item = createInboundItem(request);
        if (item == null) throw new CrmebException("库存明细不存在");
        JkStockFlow flow = new JkStockFlow().setFlowNo("SF" + IdWorker.getIdStr()).setIdempotencyKey(key).setBusinessNo(request.getBusinessNo())
            .setBusinessType(request.getBusinessType()).setBusinessId(request.getBusinessId())
            .setStockAccountId(request.getStockAccountId()).setStockItemId(item.getId()).setProductId(request.getProductId()).setSkuId(request.getSkuId())
            .setSkuCode(request.getSkuCode()).setFlowType(action).setChangeQty(request.getQuantity()).setBeforeAvailableQty(item.getAvailableQty())
            .setBeforeFrozenQty(item.getFrozenQty()).setAfterAvailableQty(item.getAvailableQty()).setAfterFrozenQty(item.getFrozenQty()).setRemark(request.getRemark()).setStatus(true).setIsDeleted(false)
            .setCreateUserId(request.getOperatorUserId()).setUpdateUserId(request.getOperatorUserId());
        try { stockFlowDao.insert(flow); } catch (DuplicateKeyException ignored) { return; }
        if (setTemplate == null) return;
        UpdateWrapper<JkStockItem> update = new UpdateWrapper<JkStockItem>().eq("id", item.getId())
            .setSql(setTemplate.replace("{q}", String.valueOf(request.getQuantity())) + ", version = version + 1, update_time = NOW()");
        if (conditionTemplate != null) update.apply(conditionTemplate.replace("{q}", String.valueOf(request.getQuantity())));
        if (stockItemDao.update(null, update) != 1) throw new CrmebException("库存不足");
        JkStockItem after = stockItemDao.selectById(item.getId());
        flow.setAfterAvailableQty(after.getAvailableQty()).setAfterFrozenQty(after.getFrozenQty());
        stockFlowDao.updateById(flow);
    }

    private JkStockItem findItem(JkStockActionRequest request) {
        return stockItemDao.selectOne(new LambdaQueryWrapper<JkStockItem>()
            .eq(JkStockItem::getStockAccountId, request.getStockAccountId()).eq(JkStockItem::getProductId, request.getProductId())
            .eq(JkStockItem::getSkuId, request.getSkuId()).eq(JkStockItem::getIsDeleted, false).last("limit 1"));
    }

    private JkStockItem createInboundItem(JkStockActionRequest request) {
        try {
            stockItemDao.insert(new JkStockItem().setBusinessNo(request.getBusinessNo()).setStockAccountId(request.getStockAccountId())
                .setProductId(request.getProductId()).setSkuId(request.getSkuId()).setSkuCode(request.getSkuCode()).setAvailableQty(0).setFrozenQty(0)
                .setTotalInQty(0).setTotalOutQty(0).setStatus(true).setIsDeleted(false).setCreateUserId(request.getOperatorUserId())
                .setUpdateUserId(request.getOperatorUserId()).setVersion(0));
        } catch (DuplicateKeyException ignored) { }
        return findItem(request);
    }

    private void validate(JkStockActionRequest request, String action) {
        if (request == null || request.getQuantity() == null || request.getQuantity() <= 0 || request.getBusinessType() == null || request.getBusinessId() == null
            || request.getStockAccountId() == null || request.getProductId() == null || action == null) throw new CrmebException("库存动作参数不完整");
    }
}
