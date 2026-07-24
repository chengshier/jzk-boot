package com.zbkj.service.service.impl.jiuzhoukang.stock;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockBatchReservation;
import com.zbkj.common.model.jiuzhoukang.JkStockFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchReservationDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.jiuzhoukang.stock.StockActionKey;
import com.zbkj.service.service.jiuzhoukang.stock.StockBatchService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分级库存唯一写入口。
 *
 * <p>区县代、合伙人和创客库存继续由 jk_stock_item 管理；平台物理库存以 CRMEB
 * 商品/SKU 库存为主账。平台订货冻结时直接占用 CRMEB 库存，并同步九州康平台
 * 镜像的 available_qty/frozen_qty，避免运营人员在两个模块重复录入平台库存。</p>
 */
@Service
public class StockFlowServiceImpl implements StockFlowService {
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkStockFlowDao stockFlowDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkStockBatchReservationDao batchReservationDao;
    @Autowired private StockBatchService stockBatchService;
    @Autowired private StoreProductService productService;
    @Autowired private StoreProductAttrValueService skuService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeStock(JkStockActionRequest request) {
        change(request, "FREEZE", "available_qty = available_qty - {q}, frozen_qty = frozen_qty + {q}", "available_qty >= {q}");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseFrozenStock(JkStockActionRequest request) {
        change(request, "RELEASE", "available_qty = available_qty + {q}, frozen_qty = frozen_qty - {q}", "frozen_qty >= {q}");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outboundFrozenStock(JkStockActionRequest request) {
        change(request, "OUTBOUND", "frozen_qty = frozen_qty - {q}, total_out_qty = total_out_qty + {q}", "frozen_qty >= {q}");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inboundStock(JkStockActionRequest request) {
        change(request, "INBOUND", "available_qty = available_qty + {q}, total_in_qty = total_in_qty + {q}", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeStockFlow(JkStockActionRequest request, String actionType) {
        change(request, actionType, null, null);
    }

    private void change(JkStockActionRequest request, String action, String setTemplate, String conditionTemplate) {
        validate(request, action);
        String key = StockActionKey.build(request.getBusinessType(), request.getBusinessId(), action,
                request.getStockAccountId(), request.getProductId(), request.getSkuId());
        if (stockFlowDao.selectOne(new LambdaQueryWrapper<JkStockFlow>()
                .eq(JkStockFlow::getIdempotencyKey, key).last("limit 1")) != null) {
            return;
        }

        JkStockAccount account = stockAccountDao.selectById(request.getStockAccountId());
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted()) || !Boolean.TRUE.equals(account.getStatus())) {
            throw new CrmebException("库存账户不存在或已停用");
        }
        boolean platformAccount = JkBizConstants.STOCK_ACCOUNT_PLATFORM.equals(account.getAccountType());

        JkStockItem item;
        if (platformAccount && setTemplate != null) {
            int beforeActionAvailable = prepareCrmebPlatformStock(request, action);
            item = syncPlatformMirror(request, beforeActionAvailable);
        } else {
            item = findItem(request);
            if (item == null && "INBOUND".equals(action)) item = createInboundItem(request);
        }
        if (item == null) throw new CrmebException("库存明细不存在");

        JkStockFlow flow = new JkStockFlow()
                .setFlowNo("SF" + IdWorker.getIdStr())
                .setIdempotencyKey(key)
                .setBusinessNo(request.getBusinessNo())
                .setBusinessType(request.getBusinessType())
                .setBusinessId(request.getBusinessId())
                .setStockAccountId(request.getStockAccountId())
                .setStockItemId(item.getId())
                .setProductId(request.getProductId())
                .setSkuId(request.getSkuId())
                .setSkuCode(request.getSkuCode())
                .setFlowType(action)
                .setChangeQty(request.getQuantity())
                .setBeforeAvailableQty(item.getAvailableQty())
                .setBeforeFrozenQty(item.getFrozenQty())
                .setAfterAvailableQty(item.getAvailableQty())
                .setAfterFrozenQty(item.getFrozenQty())
                .setRemark(request.getRemark())
                .setStatus(true)
                .setIsDeleted(false)
                .setCreateUserId(request.getOperatorUserId())
                .setUpdateUserId(request.getOperatorUserId());
        try {
            stockFlowDao.insert(flow);
        } catch (DuplicateKeyException ignored) {
            return;
        }
        if (setTemplate == null) return;

        UpdateWrapper<JkStockItem> update = new UpdateWrapper<JkStockItem>()
                .eq("id", item.getId())
                .setSql(setTemplate.replace("{q}", String.valueOf(request.getQuantity()))
                        + ", version = version + 1, update_time = NOW()");
        if (conditionTemplate != null) update.apply(conditionTemplate.replace("{q}", String.valueOf(request.getQuantity())));
        if (stockItemDao.update(null, update) != 1) throw new CrmebException("库存不足");

        // 代理库存继续维护完整批次账。平台库存的新动作以 CRMEB 为主账，不再生成第二套平台批次；
        // 升级前已存在批次冻结的单据，仍按旧批次预留完成释放或出库，避免历史单据卡死。
        if (!platformAccount || shouldConsumeLegacyPlatformReservation(request, action)) {
            if ("FREEZE".equals(action)) stockBatchService.freeze(request);
            else if ("RELEASE".equals(action)) stockBatchService.release(request);
            else if ("OUTBOUND".equals(action)) stockBatchService.outbound(request);
            else if ("INBOUND".equals(action)) stockBatchService.inbound(request);
        }

        JkStockItem after = stockItemDao.selectById(item.getId());
        flow.setAfterAvailableQty(after.getAvailableQty()).setAfterFrozenQty(after.getFrozenQty());
        stockFlowDao.updateById(flow);
    }

    /**
     * 调整 CRMEB 主库存并返回九州康镜像执行当前动作前应设置的 available_qty。
     */
    private int prepareCrmebPlatformStock(JkStockActionRequest request, String action) {
        int quantity = request.getQuantity();
        if ("FREEZE".equals(action)) {
            changeCrmebStock(request.getProductId(), request.getSkuId(), quantity, "sub");
            return currentCrmebAvailable(request.getProductId(), request.getSkuId()) + quantity;
        }
        if ("RELEASE".equals(action) || "INBOUND".equals(action)) {
            changeCrmebStock(request.getProductId(), request.getSkuId(), quantity, "add");
            return Math.max(0, currentCrmebAvailable(request.getProductId(), request.getSkuId()) - quantity);
        }
        return currentCrmebAvailable(request.getProductId(), request.getSkuId());
    }

    private void changeCrmebStock(Integer productId, Integer skuId, int quantity, String operation) {
        StoreProduct product = productService.getById(productId);
        if (product == null || Boolean.TRUE.equals(product.getIsDel())) throw new CrmebException("商品不存在或已删除");
        if (skuId != null) {
            StoreProductAttrValue sku = skuService.getById(skuId);
            if (sku == null || Boolean.TRUE.equals(sku.getIsDel()) || !productId.equals(sku.getProductId())) {
                throw new CrmebException("商品规格不存在或不属于所选商品");
            }
            if ("sub".equals(operation) && (sku.getStock() == null || sku.getStock() < quantity)) {
                throw new CrmebException("平台商品规格库存不足");
            }
            if (!Boolean.TRUE.equals(skuService.operationStock(skuId, quantity, operation, sku.getVersion()))) {
                throw new CrmebException("平台商品规格库存发生变化，请重试");
            }
        }
        if ("sub".equals(operation) && (product.getStock() == null || product.getStock() < quantity)) {
            throw new CrmebException("平台商品库存不足");
        }
        if (!Boolean.TRUE.equals(productService.operationStock(productId, quantity, operation, product.getVersion()))) {
            throw new CrmebException("平台商品库存发生变化，请重试");
        }
    }

    private int currentCrmebAvailable(Integer productId, Integer skuId) {
        if (skuId != null) {
            StoreProductAttrValue sku = skuService.getById(skuId);
            return sku == null || sku.getStock() == null ? 0 : sku.getStock();
        }
        StoreProduct product = productService.getById(productId);
        return product == null || product.getStock() == null ? 0 : product.getStock();
    }

    private JkStockItem syncPlatformMirror(JkStockActionRequest request, int availableBeforeAction) {
        JkStockItem item = findItem(request);
        if (item == null) {
            try {
                stockItemDao.insert(new JkStockItem()
                        .setBusinessNo("CRMEB_PLATFORM_MIRROR")
                        .setStockAccountId(request.getStockAccountId())
                        .setProductId(request.getProductId())
                        .setSkuId(request.getSkuId())
                        .setSkuCode(request.getSkuCode())
                        .setAvailableQty(availableBeforeAction)
                        .setFrozenQty(0)
                        .setTotalInQty(0)
                        .setTotalOutQty(0)
                        .setStatus(true)
                        .setIsDeleted(false)
                        .setCreateUserId(request.getOperatorUserId())
                        .setUpdateUserId(request.getOperatorUserId())
                        .setVersion(0));
            } catch (DuplicateKeyException ignored) {
                // 并发创建时重新读取。
            }
            item = findItem(request);
        }
        if (item == null) throw new CrmebException("平台库存镜像初始化失败");
        stockItemDao.update(null, new UpdateWrapper<JkStockItem>()
                .eq("id", item.getId())
                .set("available_qty", availableBeforeAction)
                .set("sku_code", request.getSkuCode())
                .set("update_user_id", request.getOperatorUserId())
                .setSql("version = version + 1, update_time = NOW()"));
        return stockItemDao.selectById(item.getId());
    }

    private boolean shouldConsumeLegacyPlatformReservation(JkStockActionRequest request, String action) {
        if (!("RELEASE".equals(action) || "OUTBOUND".equals(action))) return false;
        LambdaQueryWrapper<JkStockBatchReservation> query = new LambdaQueryWrapper<JkStockBatchReservation>()
                .eq(JkStockBatchReservation::getBusinessType, request.getBusinessType())
                .eq(JkStockBatchReservation::getBusinessId, request.getBusinessId())
                .eq(JkStockBatchReservation::getStockAccountId, request.getStockAccountId())
                .eq(JkStockBatchReservation::getProductId, request.getProductId())
                .eq(JkStockBatchReservation::getIsDeleted, false);
        if (request.getSkuId() == null) query.isNull(JkStockBatchReservation::getSkuId);
        else query.eq(JkStockBatchReservation::getSkuId, request.getSkuId());
        return batchReservationDao.selectCount(query) > 0;
    }

    private JkStockItem findItem(JkStockActionRequest request) {
        LambdaQueryWrapper<JkStockItem> query = new LambdaQueryWrapper<JkStockItem>()
                .eq(JkStockItem::getStockAccountId, request.getStockAccountId())
                .eq(JkStockItem::getProductId, request.getProductId())
                .eq(JkStockItem::getIsDeleted, false);
        if (request.getSkuId() == null) query.isNull(JkStockItem::getSkuId);
        else query.eq(JkStockItem::getSkuId, request.getSkuId());
        return stockItemDao.selectOne(query.last("limit 1"));
    }

    private JkStockItem createInboundItem(JkStockActionRequest request) {
        try {
            stockItemDao.insert(new JkStockItem()
                    .setBusinessNo(request.getBusinessNo())
                    .setStockAccountId(request.getStockAccountId())
                    .setProductId(request.getProductId())
                    .setSkuId(request.getSkuId())
                    .setSkuCode(request.getSkuCode())
                    .setAvailableQty(0)
                    .setFrozenQty(0)
                    .setTotalInQty(0)
                    .setTotalOutQty(0)
                    .setStatus(true)
                    .setIsDeleted(false)
                    .setCreateUserId(request.getOperatorUserId())
                    .setUpdateUserId(request.getOperatorUserId())
                    .setVersion(0));
        } catch (DuplicateKeyException ignored) {
            // 唯一键保证同一库存主体、商品、SKU 只有一条总账。
        }
        return findItem(request);
    }

    private void validate(JkStockActionRequest request, String action) {
        if (request == null || request.getQuantity() == null || request.getQuantity() <= 0
                || request.getBusinessType() == null || request.getBusinessId() == null
                || request.getStockAccountId() == null || request.getProductId() == null || action == null) {
            throw new CrmebException("库存动作参数不完整");
        }
    }
}