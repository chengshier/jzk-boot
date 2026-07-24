package com.zbkj.service.service.impl.jiuzhoukang.event;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkBusinessEvent;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessEventDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionTriggerService;
import com.zbkj.service.service.jiuzhoukang.event.JkBusinessEventService;
import com.zbkj.service.service.jiuzhoukang.support.JkDictLabelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 跨事务业务事件与补偿服务。
 * <p>库存/订单主事务提交后再执行业绩或佣金动作；事件先原子抢占为 PROCESSING，成功、失败和死亡状态均落库。
 * 这样事件失败不会回滚已经完成的库存动作，同时可通过自动或人工补偿恢复。</p>
 */
@Service
public class JkBusinessEventServiceImpl implements JkBusinessEventService {
    @Autowired private JkBusinessEventDao dao;
    @Autowired private JkBusinessEventPersistenceService persistence;
    @Autowired private CommissionTriggerService commissionTriggerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkBusinessEvent prepare(String eventKey, String eventType, Long businessId, String businessNo, String payloadJson) {
        JkBusinessEvent existing = dao.selectOne(new LambdaQueryWrapper<JkBusinessEvent>()
                .eq(JkBusinessEvent::getEventKey, eventKey).last("limit 1"));
        if (existing != null) return existing;
        Date now = new Date();
        JkBusinessEvent event = new JkBusinessEvent().setEventKey(eventKey).setEventType(eventType)
                .setBusinessId(businessId).setBusinessNo(businessNo).setPayloadJson(payloadJson)
                .setEventStatus("PENDING").setRetryCount(0).setMaxRetryCount(8).setNextRetryTime(now)
                .setOccurredTime(now).setCreateTime(now).setUpdateTime(now);
        try { dao.insert(event); }
        catch (DuplicateKeyException e) { return dao.selectOne(new LambdaQueryWrapper<JkBusinessEvent>().eq(JkBusinessEvent::getEventKey, eventKey).last("limit 1")); }
        return event;
    }

    /**
     * 在独立事务中抢占并执行已准备事件；主业务事务已经提交，因此这里失败只改变事件状态，不回滚主业务。
     */
    @Override
    public void executePrepared(Long eventId, Runnable action, Long operatorId) {
        JkBusinessEvent event = dao.selectById(eventId);
        if (event == null || "SUCCESS".equals(event.getEventStatus())) return;
        boolean manualRetry = operatorId != null;
        if (!persistence.claim(eventId, operatorId, manualRetry)) return;
        try { action.run(); persistence.success(eventId, operatorId); }
        catch (Throwable error) { persistence.failure(eventId, error, operatorId); }
    }

    @Override
    public JkBusinessEvent retry(Long eventId, Long operatorId) {
        JkBusinessEvent event = require(eventId);
        if ("SUCCESS".equals(event.getEventStatus())) return enrich(event);
        executePrepared(eventId, () -> dispatch(event), operatorId);
        return enrich(require(eventId));
    }

    @Override
    public int retryDue(int limit) {
        persistence.releaseStaleProcessing(30);
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
        List<JkBusinessEvent> events = dao.selectList(new LambdaQueryWrapper<JkBusinessEvent>()
                .in(JkBusinessEvent::getEventStatus, Arrays.asList("PENDING", "FAILED"))
                .le(JkBusinessEvent::getNextRetryTime, new Date()).orderByAsc(JkBusinessEvent::getId)
                .last("limit " + safeLimit));
        for (JkBusinessEvent event : events) retry(event.getId(), null);
        return events.size();
    }

    @Override
    public PageInfo<JkBusinessEvent> list(String eventType, String eventStatus, PageParamRequest pageParam) {
        Page<JkBusinessEvent> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkBusinessEvent> q = new LambdaQueryWrapper<JkBusinessEvent>().orderByDesc(JkBusinessEvent::getId);
        if (StrUtil.isNotBlank(eventType)) q.eq(JkBusinessEvent::getEventType, eventType);
        if (StrUtil.isNotBlank(eventStatus)) q.eq(JkBusinessEvent::getEventStatus, eventStatus);
        List<JkBusinessEvent> rows = dao.selectList(q);
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    private void dispatch(JkBusinessEvent event) {
        String requestNo = "EVENT_RETRY:" + event.getId() + ":" + (event.getRetryCount() == null ? 0 : event.getRetryCount());
        if ("PLATFORM_ORDER_STOCK_IN".equals(event.getEventType())) {
            commissionTriggerService.onPlatformOrderStockIn(event.getBusinessId(), event.getBusinessNo(), requestNo);
        } else if ("STOCK_TRANSFER_COMPLETED".equals(event.getEventType())) {
            commissionTriggerService.onStockTransferCompleted(event.getBusinessId(), event.getBusinessNo(), requestNo);
        } else if ("STOCK_TRANSFER_RETURN_COMPLETED".equals(event.getEventType())) {
            commissionTriggerService.onTransferReturnCompleted(event.getBusinessId(), event.getBusinessNo(), requestNo);
        } else {
            throw new IllegalArgumentException("当前事件类型没有可重试处理器：" + event.getEventType());
        }
    }

    private JkBusinessEvent require(Long id) {
        JkBusinessEvent event = dao.selectById(id);
        if (event == null) throw new IllegalArgumentException("业务事件不存在");
        return event;
    }

    private JkBusinessEvent enrich(JkBusinessEvent event) {
        event.setEventStatusText(JkDictLabelHelper.label("business_event_status", event.getEventStatus()));
        event.setStatusTag("SUCCESS".equals(event.getEventStatus()) ? "success"
                : (("DEAD".equals(event.getEventStatus()) || "FAILED".equals(event.getEventStatus())) ? "danger" : "warning"));
        return event;
    }
}
