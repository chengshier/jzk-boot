package com.zbkj.service.service.impl.jiuzhoukang.stock;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.model.jiuzhoukang.JkStockCheck;
import com.zbkj.common.model.jiuzhoukang.JkStockCheckItem;
import com.zbkj.common.model.jiuzhoukang.JkStockCheckLog;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCountRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCreateRequest;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockBatchDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockCheckDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockCheckItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockCheckLogDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.service.jiuzhoukang.stock.JkStockCheckService;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 盘点采用“创建快照 -> 实盘 -> 提交 -> 后台审核 -> 流水调整”的闭环。
 * 审核前若账面库存相对快照已变化，整单拒绝应用，避免用过期盘点覆盖正常业务流转。
 */
@Service
public class JkStockCheckServiceImpl implements JkStockCheckService {
    @Autowired private JkStockCheckDao checkDao;
    @Autowired private JkStockCheckItemDao itemDao;
    @Autowired private JkStockCheckLogDao logDao;
    @Autowired private JkStockAccountDao accountDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkStockBatchDao batchDao;
    @Autowired private StockFlowService stockFlowService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockCheck create(Long operatorId, JkStockCheckCreateRequest request, boolean admin) {
        JkStockCheck old = checkDao.selectOne(new LambdaQueryWrapper<JkStockCheck>()
                .eq(JkStockCheck::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) return detail(operatorId, old.getId(), admin);
        JkStockAccount account = requireAccount(request.getStockAccountId());
        assertOwner(operatorId, account, admin);
        Integer active = checkDao.selectCount(new LambdaQueryWrapper<JkStockCheck>()
                .eq(JkStockCheck::getStockAccountId, account.getId()).eq(JkStockCheck::getIsDeleted, false)
                .in(JkStockCheck::getStatus, "COUNTING", "SUBMITTED", "AUDITING"));
        if (active != null && active > 0) throw new CrmebException("该库存账户已有未完成盘点单");
        Date now = new Date();
        JkStockCheck check = new JkStockCheck().setCheckNo("SC" + IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo()).setStockAccountId(account.getId()).setOwnerUserId(account.getOwnerUserId())
                .setOwnerRoleCode(account.getRoleCode()).setRegionCode(account.getRegionCode())
                .setCheckType(StrUtil.blankToDefault(request.getCheckType(), "FULL")).setStatus("COUNTING")
                .setSnapshotTime(now).setDifferenceQuantity(0).setDifferenceAmount(BigDecimal.ZERO)
                .setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId)
                .setCreateTime(now).setUpdateTime(now).setVersion(0);
        checkDao.insert(check);
        List<JkStockItem> stocks = stockItemDao.selectList(new LambdaQueryWrapper<JkStockItem>()
                .eq(JkStockItem::getStockAccountId, account.getId()).eq(JkStockItem::getIsDeleted, false)
                .orderByAsc(JkStockItem::getProductId).orderByAsc(JkStockItem::getSkuId));
        for (JkStockItem stock : stocks) {
            int book = safe(stock.getAvailableQty()) + safe(stock.getFrozenQty());
            itemDao.insert(new JkStockCheckItem().setCheckId(check.getId()).setProductId(stock.getProductId())
                    .setSkuId(stock.getSkuId()).setSkuCode(stock.getSkuCode()).setBookQuantity(book)
                    .setDifferenceQuantity(0).setUnitCost(weightedCost(account.getId(), stock.getProductId(), stock.getSkuId()))
                    .setDifferenceAmount(BigDecimal.ZERO).setAdjustStatus("PENDING").setIsDeleted(false)
                    .setCreateTime(now).setUpdateTime(now));
        }
        writeLog(check.getId(), "CREATE", null, "COUNTING", operatorId, admin ? "ADMIN" : "APP", request.getRequestNo(), request.getRemark());
        return detail(operatorId, check.getId(), admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockCheck count(Long operatorId, Long checkId, JkStockCheckCountRequest request, boolean admin) {
        JkStockCheck check = require(checkId);
        assertCheckOwner(operatorId, check, admin);
        if (!"COUNTING".equals(check.getStatus())) throw new CrmebException("当前盘点单不能录入数量");
        JkStockCheckItem item = itemDao.selectById(request.getItemId());
        if (item == null || Boolean.TRUE.equals(item.getIsDeleted()) || !checkId.equals(item.getCheckId())) throw new CrmebException("盘点明细不存在");
        int difference = request.getActualQuantity() - safe(item.getBookQuantity());
        item.setActualQuantity(request.getActualQuantity()).setDifferenceQuantity(difference)
                .setDifferenceAmount(money(item.getUnitCost()).multiply(new BigDecimal(difference)).setScale(2, RoundingMode.HALF_UP))
                .setCountRemark(request.getRemark()).setUpdateTime(new Date());
        itemDao.updateById(item);
        return detail(operatorId, checkId, admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockCheck submit(Long operatorId, Long checkId, JkStockCheckActionRequest request, boolean admin) {
        JkStockCheck check = require(checkId);
        assertCheckOwner(operatorId, check, admin);
        if ("SUBMITTED".equals(check.getStatus())) return detail(operatorId, checkId, admin);
        if (!"COUNTING".equals(check.getStatus())) throw new CrmebException("当前盘点单不能提交");
        List<JkStockCheckItem> items = items(checkId);
        if (items.isEmpty()) throw new CrmebException("盘点单没有库存明细");
        int diffQty = 0;
        BigDecimal diffAmount = BigDecimal.ZERO;
        for (JkStockCheckItem item : items) {
            if (item.getActualQuantity() == null) throw new CrmebException("存在未盘点商品：" + item.getSkuCode());
            diffQty += safe(item.getDifferenceQuantity());
            diffAmount = diffAmount.add(money(item.getDifferenceAmount()));
        }
        Date now = new Date();
        int updated = checkDao.update(null, new UpdateWrapper<JkStockCheck>()
                .eq("id", checkId).eq("status", "COUNTING").set("status", "SUBMITTED")
                .set("submitted_at", now).set("difference_quantity", diffQty).set("difference_amount", diffAmount)
                .set("update_user_id", operatorId).set("update_time", now));
        if (updated != 1) throw new CrmebException("盘点单状态已变化，请刷新后重试");
        writeLog(checkId, "SUBMIT", "COUNTING", "SUBMITTED", operatorId, admin ? "ADMIN" : "APP", request.getRequestNo(), request.getRemark());
        return detail(operatorId, checkId, admin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockCheck audit(Long operatorId, JkStockCheckAuditRequest request) {
        JkStockCheck check = require(request.getCheckId());
        JkStockCheckLog old = logDao.selectOne(new LambdaQueryWrapper<JkStockCheckLog>()
                .eq(JkStockCheckLog::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) return detail(operatorId, check.getId(), true);
        if (!"SUBMITTED".equals(check.getStatus())) throw new CrmebException("当前盘点单不能审核");
        if (!Boolean.TRUE.equals(request.getApproved())) {
            Date now = new Date();
            check.setStatus("REJECTED").setAuditUserId(operatorId).setAuditTime(now).setAuditRemark(request.getRemark())
                    .setUpdateUserId(operatorId).setUpdateTime(now);
            checkDao.updateById(check);
            writeLog(check.getId(), "AUDIT_REJECT", "SUBMITTED", "REJECTED", operatorId, "ADMIN", request.getRequestNo(), request.getRemark());
            return detail(operatorId, check.getId(), true);
        }
        JkStockAccount account = requireAccount(check.getStockAccountId());
        List<JkStockCheckItem> items = items(check.getId());
        for (JkStockCheckItem item : items) assertSnapshotUnchanged(account.getId(), item);
        for (JkStockCheckItem item : items) applyDifference(check, item, account, operatorId, request.getRequestNo());
        Date now = new Date();
        check.setStatus("COMPLETED").setAuditUserId(operatorId).setAuditTime(now).setAuditRemark(request.getRemark())
                .setUpdateUserId(operatorId).setUpdateTime(now);
        checkDao.updateById(check);
        writeLog(check.getId(), "AUDIT_APPLY", "SUBMITTED", "COMPLETED", operatorId, "ADMIN", request.getRequestNo(), request.getRemark());
        return detail(operatorId, check.getId(), true);
    }

    @Override
    public JkStockCheck detail(Long viewerUserId, Long id, boolean admin) {
        JkStockCheck check = require(id);
        assertCheckOwner(viewerUserId, check, admin);
        check.setItems(items(id));
        check.setLogs(logDao.selectList(new LambdaQueryWrapper<JkStockCheckLog>()
                .eq(JkStockCheckLog::getCheckId, id).orderByAsc(JkStockCheckLog::getId)));
        enrich(check);
        return check;
    }

    @Override
    public PageInfo<JkStockCheck> list(Long ownerUserId, String status, PageParamRequest pageParam) {
        Page<JkStockCheck> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkStockCheck> query = new LambdaQueryWrapper<JkStockCheck>()
                .eq(JkStockCheck::getIsDeleted, false).orderByDesc(JkStockCheck::getId);
        if (ownerUserId != null) query.eq(JkStockCheck::getOwnerUserId, ownerUserId);
        if (StrUtil.isNotBlank(status)) query.eq(JkStockCheck::getStatus, status);
        List<JkStockCheck> rows = checkDao.selectList(query);
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    private void applyDifference(JkStockCheck check, JkStockCheckItem item, JkStockAccount account, Long operatorId, String requestNo) {
        int difference = safe(item.getDifferenceQuantity());
        if (difference == 0) {
            item.setAdjustStatus("NO_DIFFERENCE").setUpdateTime(new Date()); itemDao.updateById(item); return;
        }
        JkStockActionRequest action = new JkStockActionRequest().setBusinessType("STOCK_CHECK")
                .setBusinessId(check.getId()).setBusinessNo(check.getCheckNo()).setStockAccountId(account.getId())
                .setProductId(item.getProductId()).setSkuId(item.getSkuId()).setSkuCode(item.getSkuCode())
                .setQuantity(Math.abs(difference)).setUnitCost(item.getUnitCost()).setOperatorUserId(operatorId)
                .setRemark("库存盘点差异调整，requestNo=" + requestNo);
        if (difference > 0) stockFlowService.inboundStock(action);
        else { stockFlowService.freezeStock(action); stockFlowService.outboundFrozenStock(action); }
        item.setAdjustStatus("APPLIED").setUpdateTime(new Date()); itemDao.updateById(item);
    }

    private void assertSnapshotUnchanged(Long accountId, JkStockCheckItem item) {
        JkStockItem current = stockItemDao.selectOne(new LambdaQueryWrapper<JkStockItem>()
                .eq(JkStockItem::getStockAccountId, accountId).eq(JkStockItem::getProductId, item.getProductId())
                .eq(JkStockItem::getSkuId, item.getSkuId()).eq(JkStockItem::getIsDeleted, false).last("limit 1"));
        int currentQty = current == null ? 0 : safe(current.getAvailableQty()) + safe(current.getFrozenQty());
        if (currentQty != safe(item.getBookQuantity())) throw new CrmebException("盘点快照后库存已发生业务变更，请作废并重新盘点：" + item.getSkuCode());
    }

    private BigDecimal weightedCost(Long accountId, Integer productId, Integer skuId) {
        List<JkStockBatch> batches = batchDao.selectList(new LambdaQueryWrapper<JkStockBatch>()
                .eq(JkStockBatch::getStockAccountId, accountId).eq(JkStockBatch::getProductId, productId)
                .eq(JkStockBatch::getSkuId, skuId).eq(JkStockBatch::getIsDeleted, false));
        int qty = 0; BigDecimal cost = BigDecimal.ZERO;
        for (JkStockBatch batch : batches) {
            int batchQty = safe(batch.getAvailableQty()) + safe(batch.getFrozenQty());
            if (batchQty <= 0 || batch.getUnitCost() == null) continue;
            qty += batchQty; cost = cost.add(batch.getUnitCost().multiply(new BigDecimal(batchQty)));
        }
        return qty <= 0 ? BigDecimal.ZERO : cost.divide(new BigDecimal(qty), 6, RoundingMode.HALF_UP);
    }

    private JkStockAccount requireAccount(Long id) { JkStockAccount account = accountDao.selectById(id); if (account == null || Boolean.TRUE.equals(account.getIsDeleted()) || !Boolean.TRUE.equals(account.getStatus())) throw new CrmebException("库存账户不存在或已停用"); return account; }
    private JkStockCheck require(Long id) { JkStockCheck check = checkDao.selectById(id); if (check == null || Boolean.TRUE.equals(check.getIsDeleted())) throw new CrmebException("盘点单不存在"); return check; }
    private List<JkStockCheckItem> items(Long checkId) { return itemDao.selectList(new LambdaQueryWrapper<JkStockCheckItem>().eq(JkStockCheckItem::getCheckId, checkId).eq(JkStockCheckItem::getIsDeleted, false).orderByAsc(JkStockCheckItem::getId)); }
    private void assertOwner(Long userId, JkStockAccount account, boolean admin) { if (!admin && (account.getOwnerUserId() == null || !account.getOwnerUserId().equals(userId))) throw new CrmebException("无权盘点该库存账户"); }
    private void assertCheckOwner(Long userId, JkStockCheck check, boolean admin) { if (!admin && (check.getOwnerUserId() == null || !check.getOwnerUserId().equals(userId))) throw new CrmebException("无权查看或操作该盘点单"); }
    private void writeLog(Long checkId,String action,String before,String after,Long operator,String operatorType,String requestNo,String remark){ logDao.insert(new JkStockCheckLog().setCheckId(checkId).setAction(action).setBeforeStatus(before).setAfterStatus(after).setOperatorUserId(operator).setOperatorType(operatorType).setRequestNo(requestNo).setRemark(remark).setCreateTime(new Date())); }
    private void enrich(JkStockCheck check) { if (check == null) return; String s=check.getStatus(); if("COUNTING".equals(s)){check.setStatusText("盘点中").setStatusTag("warning");}else if("SUBMITTED".equals(s)){check.setStatusText("待审核").setStatusTag("warning");}else if("COMPLETED".equals(s)){check.setStatusText("已完成").setStatusTag("success");}else if("REJECTED".equals(s)){check.setStatusText("已驳回").setStatusTag("danger");}else{check.setStatusText(s).setStatusTag("info");} }
    private int safe(Integer v){return v==null?0:v;} private BigDecimal money(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
}
