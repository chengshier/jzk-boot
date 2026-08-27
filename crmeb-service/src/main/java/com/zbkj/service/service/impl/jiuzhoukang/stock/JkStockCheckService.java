package com.zbkj.service.service.impl.jiuzhoukang.stock;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockCheck;
import com.zbkj.common.model.jiuzhoukang.JkStockCheckAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkStockCheckItem;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockCheckSubmitRequest;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockCheckAuditLogDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockCheckDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockCheckItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V3.1 库存盘点。
 * 提交实盘后逻辑冻结库存账户；审核通过仅通过 StockFlowService 写调整流水。
 */
@Service
public class JkStockCheckService {
    @Autowired private JkStockCheckDao checkDao;
    @Autowired private JkStockCheckItemDao itemDao;
    @Autowired private JkStockCheckAuditLogDao auditDao;
    @Autowired private JkStockAccountDao accountDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private StockFlowService stockFlowService;

    @Transactional(rollbackFor = Exception.class)
    public JkStockCheck create(Long operatorId, JkStockCheckCreateRequest request, boolean ownerOnly) {
        JkStockCheck old = checkDao.selectOne(new LambdaQueryWrapper<JkStockCheck>()
                .eq(JkStockCheck::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) return detail(old.getId(), operatorId, ownerOnly);
        JkStockAccount account = requireAccount(request.getStockAccountId());
        if (ownerOnly && !operatorId.equals(account.getOwnerUserId())) throw new CrmebException("只能盘点本人库存");
        if (hasFrozenCheck(account.getId(), null)) throw new CrmebException("该库存账户已有进行中的盘点");
        Date now = new Date();
        JkStockCheck check = new JkStockCheck().setCheckNo("SC" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo()).setStockAccountId(account.getId()).setOwnerUserId(account.getOwnerUserId())
                .setScopeType(request.getScopeType() == null ? "ACCOUNT" : request.getScopeType())
                .setStatus("DRAFT").setFreezeStatus("NOT_FROZEN").setBookTotalQty(0).setActualTotalQty(0)
                .setProfitQty(0).setLossQty(0).setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId)
                .setCreateTime(now).setUpdateTime(now);
        checkDao.insert(check);
        int total = snapshotItems(check, now);
        check.setBookTotalQty(total).setUpdateTime(now);
        checkDao.updateById(check);
        log(check, "CREATE", null, "DRAFT", operatorId, request.getRemark(), null);
        return detail(check.getId(), operatorId, ownerOnly);
    }

    public PageInfo<JkStockCheck> list(Long ownerUserId, Long stockAccountId, String status, PageParamRequest pageParam) {
        Page<JkStockCheck> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkStockCheck> query = new LambdaQueryWrapper<JkStockCheck>()
                .eq(JkStockCheck::getIsDeleted, false).orderByDesc(JkStockCheck::getId);
        if (ownerUserId != null) query.eq(JkStockCheck::getOwnerUserId, ownerUserId);
        if (stockAccountId != null) query.eq(JkStockCheck::getStockAccountId, stockAccountId);
        if (status != null && !status.trim().isEmpty()) query.eq(JkStockCheck::getStatus, status);
        List<JkStockCheck> rows = checkDao.selectList(query);
        return CommonPage.copyPageInfo(page, rows);
    }

    public JkStockCheck detail(Long id, Long viewerUserId, boolean ownerOnly) {
        JkStockCheck check = require(id);
        if (ownerOnly && !viewerUserId.equals(check.getOwnerUserId())) throw new CrmebException("无权查看该盘点单");
        check.setItems(itemDao.selectList(new LambdaQueryWrapper<JkStockCheckItem>()
                .eq(JkStockCheckItem::getCheckId, id).eq(JkStockCheckItem::getIsDeleted, false).orderByAsc(JkStockCheckItem::getId)));
        check.setAuditLogs(auditDao.selectList(new LambdaQueryWrapper<JkStockCheckAuditLog>()
                .eq(JkStockCheckAuditLog::getCheckId, id).orderByAsc(JkStockCheckAuditLog::getId)));
        return check;
    }

    @Transactional(rollbackFor = Exception.class)
    public JkStockCheck submit(Long operatorId, JkStockCheckSubmitRequest request, boolean ownerOnly) {
        JkStockCheck check = require(request.getCheckId());
        if (ownerOnly && !operatorId.equals(check.getOwnerUserId())) throw new CrmebException("无权提交该盘点单");
        if (!"DRAFT".equals(check.getStatus())) throw new CrmebException("当前状态不能提交实盘");
        if (hasFrozenCheck(check.getStockAccountId(), check.getId())) throw new CrmebException("该库存账户已有其他进行中的盘点");
        Map<Long, JkStockCheckSubmitRequest.Item> actualMap = new HashMap<Long, JkStockCheckSubmitRequest.Item>();
        for (JkStockCheckSubmitRequest.Item value : request.getItems()) actualMap.put(value.getCheckItemId(), value);
        List<JkStockCheckItem> rows = itemDao.selectList(new LambdaQueryWrapper<JkStockCheckItem>()
                .eq(JkStockCheckItem::getCheckId, check.getId()).eq(JkStockCheckItem::getIsDeleted, false));
        if (actualMap.size() != rows.size()) throw new CrmebException("必须提交盘点范围内的全部库存明细");
        int bookTotal = 0, actualTotal = 0, profit = 0, loss = 0;
        Date now = new Date();
        for (JkStockCheckItem row : rows) {
            JkStockCheckSubmitRequest.Item input = actualMap.get(row.getId());
            if (input == null) throw new CrmebException("缺少盘点明细：" + row.getId());
            JkStockItem stock = stockItemDao.selectById(row.getStockItemId());
            if (stock == null || Boolean.TRUE.equals(stock.getIsDeleted())) throw new CrmebException("库存明细已失效，请重新创建盘点");
            int book = nvl(stock.getAvailableQty());
            int actual = input.getActualAvailableQty();
            int diff = actual - book;
            row.setBookAvailableQty(book).setBookFrozenQty(nvl(stock.getFrozenQty())).setVersionSnapshot(stock.getVersion())
                    .setActualAvailableQty(actual).setDifferenceQty(diff)
                    .setDifferenceType(diff > 0 ? "PROFIT" : diff < 0 ? "LOSS" : "NONE")
                    .setRemark(input.getRemark()).setUpdateTime(now);
            itemDao.updateById(row);
            bookTotal += book; actualTotal += actual;
            if (diff > 0) profit += diff; else loss += -diff;
        }
        check.setBookTotalQty(bookTotal).setActualTotalQty(actualTotal).setProfitQty(profit).setLossQty(loss)
                .setStatus("SUBMITTED").setFreezeStatus("FROZEN").setUpdateUserId(operatorId).setUpdateTime(now);
        checkDao.updateById(check);
        log(check, "SUBMIT", "DRAFT", "SUBMITTED", operatorId, request.getRemark(), summary(check));
        return detail(check.getId(), operatorId, ownerOnly);
    }

    @Transactional(rollbackFor = Exception.class)
    public JkStockCheck audit(Long operatorId, JkStockCheckAuditRequest request) {
        JkStockCheck check = require(request.getCheckId());
        if (!"SUBMITTED".equals(check.getStatus()) || !"FROZEN".equals(check.getFreezeStatus())) {
            throw new CrmebException("当前状态不能审核盘点");
        }
        Date now = new Date();
        if (!Boolean.TRUE.equals(request.getApproved())) {
            check.setStatus("REJECTED").setFreezeStatus("RELEASED").setAuditUserId(operatorId).setAuditTime(now)
                    .setAuditRemark(request.getRemark()).setUpdateUserId(operatorId).setUpdateTime(now);
            checkDao.updateById(check);
            log(check, "AUDIT_REJECT", "SUBMITTED", "REJECTED", operatorId, request.getRemark(), summary(check));
            return detail(check.getId(), operatorId, false);
        }
        List<JkStockCheckItem> rows = itemDao.selectList(new LambdaQueryWrapper<JkStockCheckItem>()
                .eq(JkStockCheckItem::getCheckId, check.getId()).eq(JkStockCheckItem::getIsDeleted, false));
        for (JkStockCheckItem row : rows) verifySnapshot(row);
        String actionKey = "STOCK_CHECK_ADJUST:" + check.getId();
        for (JkStockCheckItem row : rows) adjust(check, row, operatorId);
        check.setStatus("COMPLETED").setFreezeStatus("RELEASED").setAuditUserId(operatorId).setAuditTime(now)
                .setAuditRemark(request.getRemark()).setAdjustActionKey(actionKey).setCompletedTime(now)
                .setUpdateUserId(operatorId).setUpdateTime(now);
        checkDao.updateById(check);
        log(check, "AUDIT_APPROVE_AND_ADJUST", "SUBMITTED", "COMPLETED", operatorId, request.getRemark(), summary(check));
        return detail(check.getId(), operatorId, false);
    }

    public boolean isAccountFrozen(Long stockAccountId) {
        return hasFrozenCheck(stockAccountId, null);
    }

    private int snapshotItems(JkStockCheck check, Date now) {
        List<JkStockItem> stocks = stockItemDao.selectList(new LambdaQueryWrapper<JkStockItem>()
                .eq(JkStockItem::getStockAccountId, check.getStockAccountId())
                .eq(JkStockItem::getStatus, true).eq(JkStockItem::getIsDeleted, false).orderByAsc(JkStockItem::getId));
        int total = 0;
        for (JkStockItem stock : stocks) {
            int available = nvl(stock.getAvailableQty());
            total += available;
            itemDao.insert(new JkStockCheckItem().setCheckId(check.getId()).setStockItemId(stock.getId())
                    .setProductId(stock.getProductId()).setSkuId(stock.getSkuId()).setSkuCode(stock.getSkuCode())
                    .setBookAvailableQty(available).setBookFrozenQty(nvl(stock.getFrozenQty()))
                    .setDifferenceQty(0).setDifferenceType("NONE").setVersionSnapshot(stock.getVersion())
                    .setAdjusted(false).setIsDeleted(false).setCreateTime(now).setUpdateTime(now));
        }
        if (stocks.isEmpty()) throw new CrmebException("库存账户暂无可盘点明细");
        return total;
    }

    private void verifySnapshot(JkStockCheckItem row) {
        JkStockItem stock = stockItemDao.selectById(row.getStockItemId());
        if (stock == null || Boolean.TRUE.equals(stock.getIsDeleted())) throw new CrmebException("库存明细已失效");
        if (nvl(stock.getAvailableQty()) != nvl(row.getBookAvailableQty())
                || nvl(stock.getFrozenQty()) != nvl(row.getBookFrozenQty())
                || !equalsInt(stock.getVersion(), row.getVersionSnapshot())) {
            throw new CrmebException("盘点冻结后库存发生变化，请驳回并重新盘点：" + row.getStockItemId());
        }
    }

    private void adjust(JkStockCheck check, JkStockCheckItem row, Long operatorId) {
        int diff = nvl(row.getDifferenceQty());
        if (diff == 0) {
            row.setAdjusted(true).setUpdateTime(new Date()); itemDao.updateById(row); return;
        }
        JkStockActionRequest action = new JkStockActionRequest().setBusinessType("STOCK_CHECK").setBusinessId(check.getId())
                .setBusinessNo(check.getCheckNo()).setStockAccountId(check.getStockAccountId())
                .setProductId(row.getProductId()).setSkuId(row.getSkuId()).setSkuCode(row.getSkuCode())
                .setQuantity(Math.abs(diff)).setOperatorUserId(operatorId)
                .setRemark(diff > 0 ? "库存盘盈审核调整" : "库存盘亏审核调整")
                .setBatchNo("CHECK-" + check.getCheckNo() + "-" + row.getId());
        if (diff > 0) {
            stockFlowService.inboundStock(action);
        } else {
            stockFlowService.freezeStock(action);
            stockFlowService.outboundFrozenStock(action);
        }
        row.setAdjusted(true).setUpdateTime(new Date());
        itemDao.updateById(row);
    }

    private boolean hasFrozenCheck(Long accountId, Long excludeId) {
        LambdaQueryWrapper<JkStockCheck> query = new LambdaQueryWrapper<JkStockCheck>()
                .eq(JkStockCheck::getStockAccountId, accountId).eq(JkStockCheck::getFreezeStatus, "FROZEN")
                .eq(JkStockCheck::getIsDeleted, false);
        if (excludeId != null) query.ne(JkStockCheck::getId, excludeId);
        return checkDao.selectCount(query) > 0;
    }

    private JkStockAccount requireAccount(Long id) {
        JkStockAccount account = accountDao.selectById(id);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted()) || !Boolean.TRUE.equals(account.getStatus())) {
            throw new CrmebException("库存账户不存在或已停用");
        }
        return account;
    }

    private JkStockCheck require(Long id) {
        JkStockCheck check = checkDao.selectById(id);
        if (check == null || Boolean.TRUE.equals(check.getIsDeleted())) throw new CrmebException("库存盘点单不存在");
        return check;
    }

    private void log(JkStockCheck check, String action, String before, String after, Long operator, String remark, String snapshot) {
        auditDao.insert(new JkStockCheckAuditLog().setCheckId(check.getId()).setAction(action).setBeforeStatus(before)
                .setAfterStatus(after).setOperatorUserId(operator).setRemark(remark).setSnapshotJson(snapshot).setCreateTime(new Date()));
    }

    private String summary(JkStockCheck check) {
        return "{\"bookTotalQty\":" + nvl(check.getBookTotalQty()) + ",\"actualTotalQty\":" + nvl(check.getActualTotalQty())
                + ",\"profitQty\":" + nvl(check.getProfitQty()) + ",\"lossQty\":" + nvl(check.getLossQty()) + "}";
    }
    private int nvl(Integer value) { return value == null ? 0 : value; }
    private boolean equalsInt(Integer a, Integer b) { return a == null ? b == null : a.equals(b); }
}
