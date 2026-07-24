package com.zbkj.service.service.impl.jiuzhoukang.trade;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.pagehelper.*;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkStockTransferReturnDetailResponse;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionTriggerService;
import com.zbkj.service.service.jiuzhoukang.context.*;
import com.zbkj.service.service.jiuzhoukang.stock.StockFlowService;
import com.zbkj.service.service.jiuzhoukang.support.*;
import com.zbkj.service.service.jiuzhoukang.trade.StockTransferReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 调拨退回 V1 状态机。
 * <p>流程为申请、区县代审核、冻结申请人库存、寄回、确认收货、库存反向流转、线下退款确认和完成事件。
 * 审核时会锁定原调拨明细并重新计算累计可退数量，防止并发超退。</p>
 */
@Service
public class StockTransferReturnServiceImpl implements StockTransferReturnService {
    private static final String BUSINESS_TYPE = "STOCK_TRANSFER_RETURN";
    @Autowired private JkStockTransferReturnDao returnDao;
    @Autowired private JkStockTransferReturnItemDao returnItemDao;
    @Autowired private JkStockTransferDao transferDao;
    @Autowired private JkStockTransferItemDao transferItemDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private StockFlowService stockFlowService;
    @Autowired private JkUserContextService contextService;
    @Autowired private JkAuditLogService auditLogService;
    @Autowired private JkAfterCommitExecutor afterCommitExecutor;
    @Autowired private CommissionTriggerService commissionTriggerService;
    @Autowired private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransferReturn create(Long userId, JkStockTransferReturnCreateRequest request) {
        JkStockTransferReturn old = returnDao.selectOne(new LambdaQueryWrapper<JkStockTransferReturn>()
                .eq(JkStockTransferReturn::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) { if (!userId.equals(old.getUserId())) throw new CrmebException("requestNo 已被其他用户使用"); return enrich(old); }
        JkStockTransfer transfer = requireTransfer(request.getOriginalTransferId());
        if (!userId.equals(transfer.getUserId())) throw new CrmebException("无权申请该调拨单退回");
        if (!"STOCK_IN".equals(transfer.getStatus())) throw new CrmebException("只有已完成入库的调拨单才能申请退回");
        Map<Long, JkStockTransferItem> originals = transferItemDao.selectList(new LambdaQueryWrapper<JkStockTransferItem>()
                .eq(JkStockTransferItem::getTransferId, transfer.getId()).eq(JkStockTransferItem::getIsDeleted, false))
                .stream().collect(Collectors.toMap(JkStockTransferItem::getId, v -> v));
        if (request.getItems() == null || request.getItems().isEmpty()) throw new CrmebException("至少选择一条退回明细");
        Set<Long> keys = new HashSet<>();
        JkStockAccount from = account(accountType(transfer.getRoleCode()), userId);
        JkStockAccount to = account(JkBizConstants.STOCK_ACCOUNT_COUNTY_AGENT, transfer.getCountyAgentId());
        Date now = new Date();
        JkStockTransferReturn entity = new JkStockTransferReturn().setReturnNo("SR" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo()).setOriginalTransferId(transfer.getId()).setOriginalTransferNo(transfer.getTransferNo())
                .setUserId(userId).setRoleCode(transfer.getRoleCode()).setCountyAgentId(transfer.getCountyAgentId()).setRegionCode(transfer.getRegionCode())
                .setStatus("SUBMITTED").setAuditStatus("PENDING").setRefundStatus("UNREFUNDED")
                .setReturnAmount(BigDecimal.ZERO).setReturnReason(request.getReturnReason()).setIsDeleted(false)
                .setCreateUserId(userId).setUpdateUserId(userId).setCreateTime(now).setUpdateTime(now).setVersion(0);
        returnDao.insert(entity);
        BigDecimal total = BigDecimal.ZERO;
        for (JkStockTransferReturnCreateRequest.Item line : request.getItems()) {
            if (!keys.add(line.getOriginalTransferItemId())) throw new CrmebException("同一原调拨明细不能重复提交");
            JkStockTransferItem original = originals.get(line.getOriginalTransferItemId());
            if (original == null) throw new CrmebException("原调拨明细不存在");
            int already = returnedQuantity(original.getId());
            if (line.getQuantity() == null || line.getQuantity() <= 0 || line.getQuantity() + already > original.getQuantity()) {
                throw new CrmebException("退回数量超过原调拨可退数量：" + original.getProductName());
            }
            BigDecimal amount = money(original.getUnitPrice()).multiply(BigDecimal.valueOf(line.getQuantity()));
            returnItemDao.insert(new JkStockTransferReturnItem().setReturnId(entity.getId()).setOriginalTransferItemId(original.getId())
                    .setProductId(original.getProductId()).setSkuId(original.getSkuId()).setProductName(original.getProductName())
                    .setSkuName(original.getSkuName()).setSkuCode(original.getSkuCode()).setReturnQuantity(line.getQuantity())
                    .setUnitPrice(original.getUnitPrice()).setReturnAmount(amount).setFromStockAccountId(from.getId()).setToStockAccountId(to.getId())
                    .setIsDeleted(false).setCreateTime(now).setUpdateTime(now).setVersion(0));
            total = total.add(amount);
        }
        entity.setReturnAmount(total); returnDao.updateById(entity);
        log(entity, userId, null, "SUBMITTED", "SUBMIT", request.getReturnReason(), null, "FRONT");
        return enrich(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransferReturn cancel(Long userId, JkBusinessActionRequest request) {
        JkStockTransferReturn entity = requireReturn(request.getBusinessId());
        if (!userId.equals(entity.getUserId())) throw new CrmebException("无权取消该退回申请");
        if (!"SUBMITTED".equals(entity.getStatus())) throw new CrmebException("当前状态不能取消");
        int n = returnDao.update(null, new UpdateWrapper<JkStockTransferReturn>().eq("id", entity.getId()).eq("status", "SUBMITTED")
                .set("status", "CANCELLED").set("cancel_reason", request.getRemark()).set("update_user_id", userId).set("update_time", new Date()));
        if (n != 1) throw new CrmebException("当前状态不能取消");
        entity.setStatus("CANCELLED").setCancelReason(request.getRemark());
        log(entity, userId, "SUBMITTED", "CANCELLED", "CANCEL", request.getRemark(), null, "FRONT");
        return enrich(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransferReturn audit(Long countyUserId, JkPaymentAuditRequest request) {
        JkStockTransferReturn entity = requireReturn(request.getBusinessId());
        requireCounty(countyUserId, entity);
        if (!"SUBMITTED".equals(entity.getStatus())) throw new CrmebException("当前状态不能审核");
        Date now = new Date();
        if (!Boolean.TRUE.equals(request.getApproved())) {
            int rejected = returnDao.update(null, new UpdateWrapper<JkStockTransferReturn>()
                    .eq("id", entity.getId()).eq("status", "SUBMITTED")
                    .set("status", "AUDIT_REJECTED").set("audit_status", "REJECTED")
                    .set("reject_reason", request.getRemark()).set("audit_remark", request.getRemark())
                    .set("audit_user_id", countyUserId).set("audit_time", now)
                    .set("update_user_id", countyUserId).set("update_time", now));
            if (rejected != 1) throw new CrmebException("当前状态不能审核");
            entity.setStatus("AUDIT_REJECTED").setAuditStatus("REJECTED").setRejectReason(request.getRemark()).setAuditRemark(request.getRemark())
                    .setAuditUserId(countyUserId).setAuditTime(now).setUpdateUserId(countyUserId).setUpdateTime(now);
            log(entity, countyUserId, "SUBMITTED", "AUDIT_REJECTED", "REJECT", request.getRemark(), request.getRemark(), "ADMIN");
            return enrich(entity);
        }
        assertReturnQuantityStillAvailable(entity);
        for (JkStockTransferReturnItem item : items(entity.getId())) {
            stockFlowService.freezeStock(action(entity, item.getFromStockAccountId(), item, countyUserId, "调拨退回审核通过冻结申请人库存"));
        }
        int approved = returnDao.update(null, new UpdateWrapper<JkStockTransferReturn>()
                .eq("id", entity.getId()).eq("status", "SUBMITTED")
                .set("status", "AUDIT_APPROVED").set("audit_status", "APPROVED")
                .set("audit_remark", request.getRemark()).set("audit_user_id", countyUserId).set("audit_time", now)
                .set("update_user_id", countyUserId).set("update_time", now));
        if (approved != 1) throw new CrmebException("当前状态不能审核");
        entity.setStatus("AUDIT_APPROVED").setAuditStatus("APPROVED").setAuditRemark(request.getRemark())
                .setAuditUserId(countyUserId).setAuditTime(now).setUpdateUserId(countyUserId).setUpdateTime(now);
        log(entity, countyUserId, "SUBMITTED", "AUDIT_APPROVED", "PASS", request.getRemark(), null, "ADMIN");
        return enrich(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransferReturn ship(Long userId, Long returnId, JkStockTransferReturnShipRequest request) {
        JkStockTransferReturn entity = requireReturn(returnId);
        if (!userId.equals(entity.getUserId())) throw new CrmebException("无权操作该退回单");
        if (!"AUDIT_APPROVED".equals(entity.getStatus())) throw new CrmebException("当前状态不能确认寄回");
        int n = returnDao.update(null, new UpdateWrapper<JkStockTransferReturn>().eq("id", returnId).eq("status", "AUDIT_APPROVED")
                .set("status", "RETURN_SHIPPED").set("logistics_company", request.getLogisticsCompany())
                .set("logistics_no", request.getLogisticsNo()).set("ship_time", new Date()).set("update_user_id", userId).set("update_time", new Date()));
        if (n != 1) throw new CrmebException("当前状态不能确认寄回");
        entity.setStatus("RETURN_SHIPPED").setLogisticsCompany(request.getLogisticsCompany()).setLogisticsNo(request.getLogisticsNo()).setShipTime(new Date());
        log(entity, userId, "AUDIT_APPROVED", "RETURN_SHIPPED", "SHIP_RETURN", request.getRemark(), null, "FRONT");
        return enrich(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransferReturn receive(Long countyUserId, JkBusinessActionRequest request) {
        JkStockTransferReturn entity = requireReturn(request.getBusinessId());
        requireCounty(countyUserId, entity);
        if (!"RETURN_SHIPPED".equals(entity.getStatus())) throw new CrmebException("当前状态不能确认收到退货");
        int n = returnDao.update(null, new UpdateWrapper<JkStockTransferReturn>().eq("id", entity.getId()).eq("status", "RETURN_SHIPPED")
                .set("status", "REFUND_PENDING").set("refund_status", "REFUND_PENDING").set("receive_user_id", countyUserId).set("receive_time", new Date())
                .set("receive_remark", request.getRemark()).set("update_user_id", countyUserId).set("update_time", new Date()));
        if (n != 1) throw new CrmebException("当前状态不能确认收到退货");
        for (JkStockTransferReturnItem item : items(entity.getId())) {
            stockFlowService.outboundFrozenStock(action(entity, item.getFromStockAccountId(), item, countyUserId, "申请人退回库存出库"));
            stockFlowService.inboundStock(action(entity, item.getToStockAccountId(), item, countyUserId, "区县代确认退货入库"));
        }
        entity.setStatus("REFUND_PENDING").setRefundStatus("REFUND_PENDING").setReceiveUserId(countyUserId).setReceiveTime(new Date()).setReceiveRemark(request.getRemark());
        log(entity, countyUserId, "RETURN_SHIPPED", "REFUND_PENDING", "RECEIVE_RETURN", request.getRemark(), null, "ADMIN");
        return enrich(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransferReturn confirmRefund(Long countyUserId, JkStockTransferReturnRefundRequest request) {
        JkStockTransferReturn entity = requireReturn(request.getReturnId());
        requireCounty(countyUserId, entity);
        if (!"REFUND_PENDING".equals(entity.getStatus())) throw new CrmebException("当前状态不能确认退款");
        Date now = new Date();
        int n = returnDao.update(null, new UpdateWrapper<JkStockTransferReturn>().eq("id", entity.getId()).eq("status", "REFUND_PENDING")
                .set("status", "COMPLETED").set("refund_status", "REFUNDED").set("refund_user_id", countyUserId)
                .set("refund_time", now).set("refund_voucher_url", request.getRefundVoucherUrl()).set("refund_remark", request.getRemark())
                .set("update_user_id", countyUserId).set("update_time", now));
        if (n != 1) throw new CrmebException("当前状态不能确认退款");
        entity.setStatus("COMPLETED").setRefundStatus("REFUNDED").setRefundUserId(countyUserId).setRefundTime(now)
                .setRefundVoucherUrl(request.getRefundVoucherUrl()).setRefundRemark(request.getRemark());
        log(entity, countyUserId, "REFUND_PENDING", "COMPLETED", "CONFIRM_REFUND", request.getRemark(), null, "ADMIN");
        final Long id = entity.getId(); final String no = entity.getReturnNo();
        afterCommitExecutor.execute("STOCK_TRANSFER_RETURN_COMPLETED", id, no, "调拨退回已完成库存回退和线下退款确认",
                () -> commissionTriggerService.onTransferReturnCompleted(id, no, "STOCK_TRANSFER_RETURN_COMPLETED:" + id));
        return enrich(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransferReturn close(Long countyUserId, JkBusinessActionRequest request) {
        JkStockTransferReturn entity = requireReturn(request.getBusinessId());
        requireCounty(countyUserId, entity);
        if (!("SUBMITTED".equals(entity.getStatus()) || "AUDIT_APPROVED".equals(entity.getStatus()))) throw new CrmebException("当前状态不能关闭");
        String before = entity.getStatus();
        if ("AUDIT_APPROVED".equals(before)) {
            for (JkStockTransferReturnItem item : items(entity.getId())) {
                stockFlowService.releaseFrozenStock(action(entity, item.getFromStockAccountId(), item, countyUserId, "调拨退回关闭释放冻结库存"));
            }
        }
        Date now = new Date();
        int closed = returnDao.update(null, new UpdateWrapper<JkStockTransferReturn>()
                .eq("id", entity.getId()).eq("status", before)
                .set("status", "CLOSED").set("cancel_reason", request.getRemark())
                .set("update_user_id", countyUserId).set("update_time", now));
        if (closed != 1) throw new CrmebException("当前状态不能关闭");
        entity.setStatus("CLOSED").setCancelReason(request.getRemark()).setUpdateUserId(countyUserId).setUpdateTime(now);
        log(entity, countyUserId, before, "CLOSED", "CLOSE", request.getRemark(), null, "ADMIN");
        return enrich(entity);
    }

    @Override public PageInfo<JkStockTransferReturn> getFrontList(Long userId, String status, PageParamRequest page) { return list(userId, null, status, page); }
    @Override public PageInfo<JkStockTransferReturn> getHandleList(Long countyUserId, String status, PageParamRequest page) {
        return list(null, isPlatformOperator(countyUserId) ? null : countyUserId, status, page);
    }

    private PageInfo<JkStockTransferReturn> list(Long userId, Long countyId, String status, PageParamRequest p) {
        Page<JkStockTransferReturn> page = PageHelper.startPage(p.getPage(), p.getLimit());
        LambdaQueryWrapper<JkStockTransferReturn> q = new LambdaQueryWrapper<JkStockTransferReturn>().eq(JkStockTransferReturn::getIsDeleted, false).orderByDesc(JkStockTransferReturn::getId);
        if (userId != null) q.eq(JkStockTransferReturn::getUserId, userId);
        if (countyId != null) q.eq(JkStockTransferReturn::getCountyAgentId, countyId);
        if (StrUtil.isNotBlank(status)) q.eq(JkStockTransferReturn::getStatus, status);
        List<JkStockTransferReturn> rows = returnDao.selectList(q); rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override public JkStockTransferReturnDetailResponse getFrontDetail(Long userId, Long id) { JkStockTransferReturn r=requireReturn(id); if(!userId.equals(r.getUserId()))throw new CrmebException("无权查看该退回单");return detail(r); }
    @Override public JkStockTransferReturnDetailResponse getHandleDetail(Long countyUserId, Long id) { JkStockTransferReturn r=requireReturn(id);requireCounty(countyUserId,r);return detail(r); }

    private JkStockTransferReturnDetailResponse detail(JkStockTransferReturn entity) {
        JkStockTransferReturnDetailResponse response = new JkStockTransferReturnDetailResponse();
        response.setReturnOrder(enrich(entity)); response.setItems(items(entity.getId()));
        response.setAuditLogs(auditLogService.toResponses(auditLogService.list(new LambdaQueryWrapper<JkAuditLog>()
                .eq(JkAuditLog::getBusinessType, BUSINESS_TYPE).eq(JkAuditLog::getBusinessId, entity.getId())
                .eq(JkAuditLog::getIsDeleted, false).orderByDesc(JkAuditLog::getId))));
        return response;
    }

    /**
     * 审核时重新锁定原调拨明细并校验剩余可退数量。
     * 创建时的提示校验不能替代此处事务校验；两个退回单并发审核时，
     * 原调拨明细行锁会串行化审核，避免累计通过数量超过原调拨数量。
     */
    private void assertReturnQuantityStillAvailable(JkStockTransferReturn currentReturn) {
        List<JkStockTransferReturn> accepted = returnDao.selectList(new LambdaQueryWrapper<JkStockTransferReturn>()
                .eq(JkStockTransferReturn::getOriginalTransferId, currentReturn.getOriginalTransferId())
                .ne(JkStockTransferReturn::getId, currentReturn.getId())
                .in(JkStockTransferReturn::getStatus, Arrays.asList("AUDIT_APPROVED", "RETURN_SHIPPED", "REFUND_PENDING", "COMPLETED"))
                .eq(JkStockTransferReturn::getIsDeleted, false)
                .last("FOR UPDATE"));
        Set<Long> acceptedIds = accepted.stream().map(JkStockTransferReturn::getId).collect(Collectors.toSet());
        for (JkStockTransferReturnItem currentItem : items(currentReturn.getId())) {
            JkStockTransferItem original = transferItemDao.selectOne(new LambdaQueryWrapper<JkStockTransferItem>()
                    .eq(JkStockTransferItem::getId, currentItem.getOriginalTransferItemId())
                    .eq(JkStockTransferItem::getIsDeleted, false)
                    .last("FOR UPDATE"));
            if (original == null) throw new CrmebException("原调拨明细不存在或已删除");
            int acceptedQuantity = 0;
            if (!acceptedIds.isEmpty()) {
                acceptedQuantity = returnItemDao.selectList(new LambdaQueryWrapper<JkStockTransferReturnItem>()
                        .eq(JkStockTransferReturnItem::getOriginalTransferItemId, original.getId())
                        .in(JkStockTransferReturnItem::getReturnId, acceptedIds)
                        .eq(JkStockTransferReturnItem::getIsDeleted, false))
                        .stream().map(JkStockTransferReturnItem::getReturnQuantity).reduce(0, Integer::sum);
            }
            if (acceptedQuantity + currentItem.getReturnQuantity() > original.getQuantity()) {
                throw new CrmebException("原调拨明细剩余可退数量不足：" + original.getProductName());
            }
        }
    }

    private int returnedQuantity(Long originalItemId) {
        List<JkStockTransferReturn> valid = returnDao.selectList(new LambdaQueryWrapper<JkStockTransferReturn>()
                .notIn(JkStockTransferReturn::getStatus, Arrays.asList("AUDIT_REJECTED", "CANCELLED", "CLOSED"))
                .eq(JkStockTransferReturn::getIsDeleted, false));
        if (valid.isEmpty()) return 0;
        Set<Long> ids = valid.stream().map(JkStockTransferReturn::getId).collect(Collectors.toSet());
        Integer total = returnItemDao.selectList(new LambdaQueryWrapper<JkStockTransferReturnItem>()
                .eq(JkStockTransferReturnItem::getOriginalTransferItemId, originalItemId).in(JkStockTransferReturnItem::getReturnId, ids)
                .eq(JkStockTransferReturnItem::getIsDeleted, false)).stream().map(JkStockTransferReturnItem::getReturnQuantity).reduce(0, Integer::sum);
        return total == null ? 0 : total;
    }

    private JkStockActionRequest action(JkStockTransferReturn r, Long accountId, JkStockTransferReturnItem item, Long operator, String remark) {
        return new JkStockActionRequest().setBusinessType(BUSINESS_TYPE).setBusinessId(r.getId()).setBusinessNo(r.getReturnNo())
                .setStockAccountId(accountId).setProductId(item.getProductId()).setSkuId(item.getSkuId()).setSkuCode(item.getSkuCode())
                .setQuantity(item.getReturnQuantity()).setUnitCost(item.getUnitPrice()).setOperatorUserId(operator).setRemark(remark);
    }
    private List<JkStockTransferReturnItem> items(Long id) { return returnItemDao.selectList(new LambdaQueryWrapper<JkStockTransferReturnItem>().eq(JkStockTransferReturnItem::getReturnId,id).eq(JkStockTransferReturnItem::getIsDeleted,false)); }
    private JkStockTransferReturn requireReturn(Long id) { JkStockTransferReturn v=returnDao.selectById(id);if(v==null||Boolean.TRUE.equals(v.getIsDeleted()))throw new CrmebException("调拨退回单不存在");return v; }
    private JkStockTransfer requireTransfer(Long id) { JkStockTransfer v=transferDao.selectById(id);if(v==null||Boolean.TRUE.equals(v.getIsDeleted()))throw new CrmebException("原调拨单不存在");return v; }
    private void requireCounty(Long county, JkStockTransferReturn r) {
        if (isPlatformOperator(county)) return;
        JkUserContext c = contextService.getFrontContext(county);
        if (!county.equals(r.getCountyAgentId()) || c == null || Boolean.TRUE.equals(c.getFreezeStatus())
                || !JkBizConstants.ROLE_COUNTY_AGENT.equals(c.getPrimaryRoleCode()) || !Objects.equals(c.getRegionCode(), r.getRegionCode())) {
            throw new CrmebException("无权处理非本区县代退回单");
        }
    }
    private boolean isPlatformOperator(Long userId) {
        if (userId == null) return false;
        if (userId < 0L) return true;
        JkUserContext context = contextService.getFrontContext(userId);
        if (context == null || Boolean.TRUE.equals(context.getFreezeStatus())) return false;
        if (JkBizConstants.ROLE_PLATFORM_ADMIN.equals(context.getPrimaryRoleCode())) return true;
        return context.getDataScopes() != null && context.getDataScopes().stream()
                .anyMatch(scope -> JkBizConstants.SCOPE_PLATFORM_ALL.equals(scope.getScopeType()));
    }
    private JkStockAccount account(String type, Long owner) { JkStockAccount v=stockAccountDao.selectOne(new LambdaQueryWrapper<JkStockAccount>().eq(JkStockAccount::getAccountType,type).eq(JkStockAccount::getOwnerUserId,owner).eq(JkStockAccount::getStatus,true).eq(JkStockAccount::getIsDeleted,false).last("limit 1"));if(v==null)throw new CrmebException("库存账户不存在");return v; }
    private String accountType(String role) { return JkBizConstants.ROLE_MAKER.equals(role)?JkBizConstants.STOCK_ACCOUNT_MAKER:JkBizConstants.STOCK_ACCOUNT_PARTNER; }
    private BigDecimal money(BigDecimal v) { return v==null?BigDecimal.ZERO:v; }
    private JkStockTransferReturn enrich(JkStockTransferReturn r) { User u=r.getUserId()==null?null:userService.getById(r.getUserId().intValue());if(u!=null){r.setApplicantName(StrUtil.blankToDefault(u.getRealName(),u.getNickname()));r.setApplicantPhone(u.getPhone());}r.setStatusText(JkDictLabelHelper.label("stock_transfer_return_status",r.getStatus()));r.setAuditStatusText(JkDictLabelHelper.label("audit_status",r.getAuditStatus()));r.setRefundStatusText(JkDictLabelHelper.label("refund_status",r.getRefundStatus()));r.setStatusTag("COMPLETED".equals(r.getStatus())?"success":(("AUDIT_REJECTED".equals(r.getStatus())||"CLOSED".equals(r.getStatus())||"CANCELLED".equals(r.getStatus()))?"danger":"warning"));List<JkStockTransferReturnItem> list=items(r.getId());if(!list.isEmpty()){r.setFirstProductName(list.get(0).getProductName());r.setFirstSkuName(list.get(0).getSkuName());}return r; }
    private void log(JkStockTransferReturn r,Long user,String before,String after,String action,String remark,String reject,String source){auditLogService.saveAuditLog(new JkAuditLog().setBusinessType(BUSINESS_TYPE).setBusinessId(r.getId()).setBusinessNo(r.getReturnNo()).setRequestNo(r.getRequestNo()).setAuditUserId(user).setAuditUserType(source).setAuditAction(action).setBeforeStatus(before).setAfterStatus(after).setRejectReason(reject).setAuditRemark(remark).setOperateSource(source).setStatus(true).setIsDeleted(false).setCreateUserId(user).setUpdateUserId(user));}
}
