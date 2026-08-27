package com.zbkj.service.service.impl.jiuzhoukang.trade;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkReceiveExceptionResolution;
import com.zbkj.common.model.jiuzhoukang.JkReceiveExceptionResolutionItem;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveExceptionItem;
import com.zbkj.common.request.jiuzhoukang.JkReceiveExceptionResolutionActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkReceiveExceptionResolutionCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeReceiveExceptionHandleRequest;
import com.zbkj.service.dao.jiuzhoukang.JkReceiveExceptionResolutionDao;
import com.zbkj.service.dao.jiuzhoukang.JkReceiveExceptionResolutionItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkTradeReceiveExceptionDao;
import com.zbkj.service.dao.jiuzhoukang.JkTradeReceiveExceptionItemDao;
import com.zbkj.service.service.jiuzhoukang.trade.JkReceiveExceptionResolutionService;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeReceiveExceptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 异常收货 V2 处理方案。
 *
 * <p>本服务只在有明确数量依据时登记补发、退款、退回或接受方案。纯补发方案全部完成后，
 * 原业务恢复待收货；涉及退款、退回或线下接受的方案继续锁定原业务，直到对应资金、退货或
 * 差额入库动作真正完成，避免仅修改状态造成库存、业绩和佣金失真。</p>
 */
@Service
public class JkReceiveExceptionResolutionServiceImpl implements JkReceiveExceptionResolutionService {
    private static final String STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> TYPES = new HashSet<String>(Arrays.asList("RESHIP", "REFUND", "RETURN", "ACCEPT", "MIXED"));

    @Autowired private JkReceiveExceptionResolutionDao resolutionDao;
    @Autowired private JkReceiveExceptionResolutionItemDao resolutionItemDao;
    @Autowired private JkTradeReceiveExceptionDao exceptionDao;
    @Autowired private JkTradeReceiveExceptionItemDao exceptionItemDao;
    @Autowired private JkTradeReceiveExceptionService exceptionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkReceiveExceptionResolution create(Long operatorUserId, JkReceiveExceptionResolutionCreateRequest request) {
        JkReceiveExceptionResolution old = resolutionDao.selectOne(new LambdaQueryWrapper<JkReceiveExceptionResolution>()
                .eq(JkReceiveExceptionResolution::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) return enrich(old);

        JkTradeReceiveException exception = requireException(request.getExceptionId());
        if ("RESOLVED".equals(exception.getStatus()) || "REJECTED".equals(exception.getStatus())) {
            throw new CrmebException("收货异常已经结束，不能新增处理方案");
        }
        String type = normalizeType(request.getResolutionType());
        Map<Long, JkTradeReceiveExceptionItem> exceptionItems = exceptionItemDao.selectList(
                new LambdaQueryWrapper<JkTradeReceiveExceptionItem>()
                        .eq(JkTradeReceiveExceptionItem::getExceptionId, exception.getId())
                        .eq(JkTradeReceiveExceptionItem::getIsDeleted, false))
                .stream().collect(Collectors.toMap(JkTradeReceiveExceptionItem::getId, item -> item));
        if (exceptionItems.isEmpty()) throw new CrmebException("收货异常没有可处理商品明细");

        Map<Long, Integer> allocated = allocatedQuantities(exception.getId());
        Set<Long> requestIds = new HashSet<Long>();
        List<JkReceiveExceptionResolutionItem> items = new ArrayList<JkReceiveExceptionResolutionItem>();
        int acceptedTotal = 0;
        int reshipTotal = 0;
        int refundQtyTotal = 0;
        int returnQtyTotal = 0;
        for (JkReceiveExceptionResolutionCreateRequest.Item requestItem : request.getItems()) {
            if (!requestIds.add(requestItem.getExceptionItemId())) throw new CrmebException("同一异常商品不能重复提交");
            JkTradeReceiveExceptionItem source = exceptionItems.get(requestItem.getExceptionItemId());
            if (source == null) throw new CrmebException("异常商品明细不存在或不属于当前异常单");
            int accepted = safe(requestItem.getAcceptedQty());
            int reship = safe(requestItem.getReshipQty());
            int refund = safe(requestItem.getRefundQty());
            int returned = safe(requestItem.getReturnQty());
            int current = accepted + reship + refund + returned;
            if (current <= 0) throw new CrmebException(source.getProductName() + "至少填写一种处理数量");
            int issue = safe(source.getShortageQty()) + safe(source.getDamagedQty());
            int remaining = issue - safe(allocated.get(source.getId()));
            if (current > remaining) {
                throw new CrmebException(source.getProductName() + "处理数量超过剩余异常数量，剩余可处理 " + Math.max(remaining, 0));
            }
            validateTypeQuantities(type, accepted, reship, refund, returned);
            acceptedTotal += accepted;
            reshipTotal += reship;
            refundQtyTotal += refund;
            returnQtyTotal += returned;
            items.add(new JkReceiveExceptionResolutionItem()
                    .setExceptionItemId(source.getId()).setBusinessItemId(source.getBusinessItemId())
                    .setProductId(source.getProductId()).setSkuId(source.getSkuId())
                    .setAcceptedQty(accepted).setReshipQty(reship).setRefundQty(refund).setReturnQty(returned)
                    .setLogisticsCompany(requestItem.getLogisticsCompany()).setLogisticsNo(requestItem.getLogisticsNo())
                    .setItemRemark(requestItem.getItemRemark()).setIsDeleted(false));
        }
        validateAmounts(type, refundQtyTotal, request.getRefundAmount(), request.getClaimAmount());

        Date now = new Date();
        JkReceiveExceptionResolution resolution = new JkReceiveExceptionResolution()
                .setExceptionId(exception.getId()).setResolutionNo("RR" + IdWorker.getIdStr())
                .setResolutionType(type).setResolutionStatus(STATUS_PENDING_CONFIRM)
                .setAcceptedQuantity(acceptedTotal).setReshipQuantity(reshipTotal)
                .setRefundAmount(money(request.getRefundAmount())).setClaimAmount(money(request.getClaimAmount()))
                .setResponsibilityParty(request.getResponsibilityParty())
                .setEvidenceUrls(JSONUtil.toJsonStr(request.getEvidenceUrls() == null ? Collections.emptyList() : request.getEvidenceUrls()))
                .setResolutionJson(summaryJson(acceptedTotal, reshipTotal, refundQtyTotal, returnQtyTotal))
                .setOperatorUserId(operatorUserId).setRequestNo(request.getRequestNo()).setRemark(request.getRemark())
                .setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        try {
            resolutionDao.insert(resolution);
        } catch (DuplicateKeyException duplicate) {
            JkReceiveExceptionResolution duplicateValue = resolutionDao.selectOne(new LambdaQueryWrapper<JkReceiveExceptionResolution>()
                    .eq(JkReceiveExceptionResolution::getRequestNo, request.getRequestNo()).last("limit 1"));
            if (duplicateValue != null) return enrich(duplicateValue);
            throw new CrmebException("处理方案正在提交，请勿重复操作");
        }
        for (JkReceiveExceptionResolutionItem item : items) {
            item.setResolutionId(resolution.getId()).setCreateTime(now).setUpdateTime(now);
            resolutionItemDao.insert(item);
        }
        if ("PENDING".equals(exception.getStatus())) {
            exception.setStatus("PROCESSING").setHandleAction("CREATE_RESOLUTION")
                    .setHandleRemark("已创建异常收货 V2 处理方案").setHandleUserId(operatorUserId)
                    .setHandleTime(now).setUpdateUserId(operatorUserId).setUpdateTime(now);
            exceptionDao.updateById(exception);
        }
        return enrich(resolution);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkReceiveExceptionResolution complete(Long operatorUserId, JkReceiveExceptionResolutionActionRequest request) {
        JkReceiveExceptionResolution resolution = requireResolution(request.getResolutionId());
        if (STATUS_COMPLETED.equals(resolution.getResolutionStatus())) return enrich(resolution);
        if (STATUS_CANCELLED.equals(resolution.getResolutionStatus())) throw new CrmebException("已取消的处理方案不能完成");
        if (!STATUS_PENDING_CONFIRM.equals(resolution.getResolutionStatus())) throw new CrmebException("当前处理方案状态不能完成");
        List<JkReceiveExceptionResolutionItem> items = items(resolution.getId());
        if ("RESHIP".equals(resolution.getResolutionType())) {
            for (JkReceiveExceptionResolutionItem item : items) {
                if (safe(item.getReshipQty()) > 0 && (StrUtil.isBlank(item.getLogisticsCompany()) || StrUtil.isBlank(item.getLogisticsNo()))) {
                    throw new CrmebException("补发方案必须填写物流公司和物流单号");
                }
            }
        }
        Date now = new Date();
        resolution.setResolutionStatus(STATUS_COMPLETED).setOperatorUserId(operatorUserId)
                .setCompletedAt(now).setRemark(request.getRemark()).setUpdateTime(now);
        resolutionDao.updateById(resolution);
        refreshExceptionAfterComplete(operatorUserId, resolution.getExceptionId(), request.getRequestNo());
        return enrich(resolution);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkReceiveExceptionResolution cancel(Long operatorUserId, JkReceiveExceptionResolutionActionRequest request) {
        JkReceiveExceptionResolution resolution = requireResolution(request.getResolutionId());
        if (STATUS_CANCELLED.equals(resolution.getResolutionStatus())) return enrich(resolution);
        if (!STATUS_PENDING_CONFIRM.equals(resolution.getResolutionStatus())) throw new CrmebException("只有待确认方案可以取消");
        resolution.setResolutionStatus(STATUS_CANCELLED).setOperatorUserId(operatorUserId)
                .setRemark(request.getRemark()).setUpdateTime(new Date());
        resolutionDao.updateById(resolution);
        return enrich(resolution);
    }

    @Override
    public List<JkReceiveExceptionResolution> list(Long exceptionId) {
        requireException(exceptionId);
        List<JkReceiveExceptionResolution> rows = resolutionDao.selectList(new LambdaQueryWrapper<JkReceiveExceptionResolution>()
                .eq(JkReceiveExceptionResolution::getExceptionId, exceptionId)
                .eq(JkReceiveExceptionResolution::getIsDeleted, false).orderByAsc(JkReceiveExceptionResolution::getId));
        rows.forEach(this::enrich);
        return rows;
    }

    private void refreshExceptionAfterComplete(Long operatorUserId, Long exceptionId, String actionRequestNo) {
        JkTradeReceiveException exception = requireException(exceptionId);
        List<JkReceiveExceptionResolution> completed = resolutionDao.selectList(new LambdaQueryWrapper<JkReceiveExceptionResolution>()
                .eq(JkReceiveExceptionResolution::getExceptionId, exceptionId)
                .eq(JkReceiveExceptionResolution::getResolutionStatus, STATUS_COMPLETED)
                .eq(JkReceiveExceptionResolution::getIsDeleted, false));
        List<JkTradeReceiveExceptionItem> sourceItems = exceptionItemDao.selectList(new LambdaQueryWrapper<JkTradeReceiveExceptionItem>()
                .eq(JkTradeReceiveExceptionItem::getExceptionId, exceptionId)
                .eq(JkTradeReceiveExceptionItem::getIsDeleted, false));
        int issue = sourceItems.stream().mapToInt(item -> safe(item.getShortageQty()) + safe(item.getDamagedQty())).sum();
        int covered = 0;
        boolean pureReship = !completed.isEmpty();
        for (JkReceiveExceptionResolution value : completed) {
            if (!"RESHIP".equals(value.getResolutionType())) pureReship = false;
            for (JkReceiveExceptionResolutionItem item : items(value.getId())) {
                covered += safe(item.getAcceptedQty()) + safe(item.getReshipQty()) + safe(item.getRefundQty()) + safe(item.getReturnQty());
            }
        }
        if (issue > 0 && covered >= issue && pureReship) {
            exceptionService.handle(operatorUserId, new JkTradeReceiveExceptionHandleRequest()
                    .setExceptionId(exceptionId).setAction("RESOLVED")
                    .setRemark("补发方案已全部完成，恢复原业务待收货；requestNo=" + actionRequestNo));
            return;
        }
        exception.setStatus("PROCESSING").setHandleAction(covered > 0 ? "PARTIAL_RESOLUTION_COMPLETED" : "RESOLUTION_COMPLETED")
                .setHandleRemark("已完成处理数量 " + covered + "/" + issue + "；涉及退款、退回或接受的方案须等待对应真实业务动作完成")
                .setHandleUserId(operatorUserId).setHandleTime(new Date()).setUpdateUserId(operatorUserId).setUpdateTime(new Date());
        exceptionDao.updateById(exception);
    }

    private Map<Long, Integer> allocatedQuantities(Long exceptionId) {
        List<JkReceiveExceptionResolution> active = resolutionDao.selectList(new LambdaQueryWrapper<JkReceiveExceptionResolution>()
                .eq(JkReceiveExceptionResolution::getExceptionId, exceptionId)
                .in(JkReceiveExceptionResolution::getResolutionStatus, Arrays.asList(STATUS_PENDING_CONFIRM, STATUS_COMPLETED))
                .eq(JkReceiveExceptionResolution::getIsDeleted, false));
        if (active.isEmpty()) return new HashMap<Long, Integer>();
        List<Long> ids = active.stream().map(JkReceiveExceptionResolution::getId).collect(Collectors.toList());
        Map<Long, Integer> result = new HashMap<Long, Integer>();
        for (JkReceiveExceptionResolutionItem item : resolutionItemDao.selectList(new LambdaQueryWrapper<JkReceiveExceptionResolutionItem>()
                .in(JkReceiveExceptionResolutionItem::getResolutionId, ids)
                .eq(JkReceiveExceptionResolutionItem::getIsDeleted, false))) {
            int quantity = safe(item.getAcceptedQty()) + safe(item.getReshipQty()) + safe(item.getRefundQty()) + safe(item.getReturnQty());
            result.put(item.getExceptionItemId(), safe(result.get(item.getExceptionItemId())) + quantity);
        }
        return result;
    }

    private void validateTypeQuantities(String type, int accepted, int reship, int refund, int returned) {
        if ("RESHIP".equals(type) && (reship <= 0 || accepted + refund + returned > 0)) throw new CrmebException("补发方案只能填写补发数量");
        if ("REFUND".equals(type) && (refund <= 0 || accepted + reship + returned > 0)) throw new CrmebException("退款方案只能填写退款数量");
        if ("RETURN".equals(type) && (returned <= 0 || accepted + reship + refund > 0)) throw new CrmebException("退回方案只能填写退回数量");
        if ("ACCEPT".equals(type) && (accepted <= 0 || reship + refund + returned > 0)) throw new CrmebException("接受方案只能填写接受数量");
    }

    private void validateAmounts(String type, int refundQty, BigDecimal refundAmount, BigDecimal claimAmount) {
        if (("REFUND".equals(type) || refundQty > 0) && money(refundAmount).signum() <= 0) throw new CrmebException("包含退款数量时必须填写退款金额");
        if (money(refundAmount).signum() < 0 || money(claimAmount).signum() < 0) throw new CrmebException("退款或索赔金额不能小于0");
    }

    private JkTradeReceiveException requireException(Long id) {
        JkTradeReceiveException value = exceptionDao.selectById(id);
        if (value == null || Boolean.TRUE.equals(value.getIsDeleted())) throw new CrmebException("收货异常记录不存在");
        return value;
    }

    private JkReceiveExceptionResolution requireResolution(Long id) {
        JkReceiveExceptionResolution value = resolutionDao.selectById(id);
        if (value == null || Boolean.TRUE.equals(value.getIsDeleted())) throw new CrmebException("异常处理方案不存在");
        return value;
    }

    private List<JkReceiveExceptionResolutionItem> items(Long resolutionId) {
        return resolutionItemDao.selectList(new LambdaQueryWrapper<JkReceiveExceptionResolutionItem>()
                .eq(JkReceiveExceptionResolutionItem::getResolutionId, resolutionId)
                .eq(JkReceiveExceptionResolutionItem::getIsDeleted, false).orderByAsc(JkReceiveExceptionResolutionItem::getId));
    }

    private JkReceiveExceptionResolution enrich(JkReceiveExceptionResolution value) {
        value.setItems(items(value.getId())).setResolutionTypeText(typeText(value.getResolutionType()))
                .setResolutionStatusText(statusText(value.getResolutionStatus()));
        return value;
    }

    private String normalizeType(String type) {
        String value = StrUtil.blankToDefault(type, "").trim().toUpperCase();
        if (!TYPES.contains(value)) throw new CrmebException("不支持的异常处理类型");
        return value;
    }

    private String typeText(String type) {
        if ("RESHIP".equals(type)) return "补发";
        if ("REFUND".equals(type)) return "差额退款";
        if ("RETURN".equals(type)) return "退回异常商品";
        if ("ACCEPT".equals(type)) return "接受现状";
        return "组合处理";
    }

    private String statusText(String status) {
        if (STATUS_PENDING_CONFIRM.equals(status)) return "待确认";
        if (STATUS_COMPLETED.equals(status)) return "已完成";
        if (STATUS_CANCELLED.equals(status)) return "已取消";
        return status;
    }

    private String summaryJson(int accepted, int reship, int refund, int returned) {
        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("acceptedQty", accepted);
        summary.put("reshipQty", reship);
        summary.put("refundQty", refund);
        summary.put("returnQty", returned);
        return JSONUtil.toJsonStr(summary);
    }

    private int safe(Integer value) { return value == null ? 0 : value; }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
