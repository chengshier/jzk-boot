package com.zbkj.service.service.impl.jiuzhoukang.performance;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkPerformancePeriod;
import com.zbkj.common.model.jiuzhoukang.JkPerformancePeriodItem;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.model.jiuzhoukang.JkPeriodRewardRecord;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleTrialRequest;
import com.zbkj.common.request.jiuzhoukang.JkPerformancePeriodBuildRequest;
import com.zbkj.common.request.jiuzhoukang.JkPerformancePeriodCloseRequest;
import com.zbkj.common.response.jiuzhoukang.JkCommissionRuleTrialResponse;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkPerformancePeriodDao;
import com.zbkj.service.dao.jiuzhoukang.JkPerformancePeriodItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkPerformanceRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkPeriodRewardRecordDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionScenarioService;
import com.zbkj.service.service.jiuzhoukang.performance.JkPerformancePeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 周期业绩只汇总有效线上零售和经核验线下终端销售；平台订货、内部库存调拨不会进入团队销售奖励。
 */
@Service
public class JkPerformancePeriodServiceImpl implements JkPerformancePeriodService {
    @Autowired private JkPerformancePeriodDao periodDao;
    @Autowired private JkPerformancePeriodItemDao itemDao;
    @Autowired private JkPerformanceRecordDao performanceDao;
    @Autowired private JkPeriodRewardRecordDao rewardDao;
    @Autowired private JkCommissionRecordDao commissionRecordDao;
    @Autowired private CommissionScenarioService scenarioService;

    @Override
    public List<JkPerformancePeriod> list(String status, String periodType) {
        LambdaQueryWrapper<JkPerformancePeriod> query = new LambdaQueryWrapper<JkPerformancePeriod>()
                .eq(JkPerformancePeriod::getIsDeleted, false).orderByDesc(JkPerformancePeriod::getId);
        if (StrUtil.isNotBlank(status)) query.eq(JkPerformancePeriod::getStatus, status.trim());
        if (StrUtil.isNotBlank(periodType)) query.eq(JkPerformancePeriod::getPeriodType, periodType.trim());
        return periodDao.selectList(query);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        JkPerformancePeriod period = require(id);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("period", period);
        result.put("items", itemDao.selectList(new LambdaQueryWrapper<JkPerformancePeriodItem>()
                .eq(JkPerformancePeriodItem::getPeriodId, id).eq(JkPerformancePeriodItem::getIsDeleted, false)
                .orderByDesc(JkPerformancePeriodItem::getNetAmount)));
        result.put("rewards", rewardDao.selectList(new LambdaQueryWrapper<JkPeriodRewardRecord>()
                .eq(JkPeriodRewardRecord::getPeriodId, id).eq(JkPeriodRewardRecord::getIsDeleted, false)
                .orderByDesc(JkPeriodRewardRecord::getApprovedRewardAmount)));
        result.put("ownerSummary", ownerSummary(id));
        result.put("locked", "CLOSED".equals(period.getStatus()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPerformancePeriod build(JkPerformancePeriodBuildRequest request, Long operatorId) {
        if (!request.getEndTime().after(request.getStartTime())) throw new IllegalArgumentException("周期结束时间必须晚于开始时间");
        JkPerformancePeriod existing = periodDao.selectOne(new LambdaQueryWrapper<JkPerformancePeriod>()
                .eq(JkPerformancePeriod::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (existing != null) return existing;

        LambdaQueryWrapper<JkPerformanceRecord> query = new LambdaQueryWrapper<JkPerformanceRecord>()
                .eq(JkPerformanceRecord::getIsDeleted, false)
                .in(JkPerformanceRecord::getSourceType, "RETAIL_ORDER", "OFFLINE_SALE")
                .in(JkPerformanceRecord::getStatus, "VALID", "SETTLED", "ACTIVE")
                .ge(JkPerformanceRecord::getOccurredAt, request.getStartTime())
                .lt(JkPerformanceRecord::getOccurredAt, request.getEndTime())
                .orderByAsc(JkPerformanceRecord::getId);
        if (request.getPlanId() != null) query.eq(JkPerformanceRecord::getPlanId, request.getPlanId());
        if (StrUtil.isNotBlank(request.getOwnerRoleCode())) query.eq(JkPerformanceRecord::getOwnerRoleCode, request.getOwnerRoleCode().trim());
        if (StrUtil.isNotBlank(request.getRegionCode())) query.eq(JkPerformanceRecord::getRegionCode, request.getRegionCode().trim());
        List<JkPerformanceRecord> records = performanceDao.selectList(query);

        Date now = new Date();
        JkPerformancePeriod period = new JkPerformancePeriod()
                .setPeriodNo("PP" + IdWorker.getIdStr()).setPeriodType(request.getPeriodType().trim())
                .setStartTime(request.getStartTime()).setEndTime(request.getEndTime())
                .setPlanId(request.getPlanId()).setRuleId(request.getRuleId())
                .setOwnerRoleCode(request.getOwnerRoleCode()).setRegionCode(request.getRegionCode())
                .setStatus("DRAFT").setTotalPerformanceAmount(BigDecimal.ZERO)
                .setTotalRefundAmount(BigDecimal.ZERO).setNetPerformanceAmount(BigDecimal.ZERO)
                .setMemberCount(0).setTrialRewardAmount(BigDecimal.ZERO).setApprovedRewardAmount(BigDecimal.ZERO)
                .setSnapshotJson(JSONUtil.toJsonStr(request)).setRequestNo(request.getRequestNo()).setCreatedBy(operatorId)
                .setVersion(0).setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
        periodDao.insert(period);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal reversed = BigDecimal.ZERO;
        Set<Long> owners = new HashSet<Long>();
        for (JkPerformanceRecord record : records) {
            BigDecimal performance = money(record.getPerformanceAmount());
            BigDecimal refund = money(record.getReversedAmount());
            BigDecimal net = performance.subtract(refund).max(BigDecimal.ZERO);
            String key = "PERFORMANCE_PERIOD:" + period.getId() + ":" + record.getId();
            JkPerformancePeriodItem item = new JkPerformancePeriodItem()
                    .setPeriodId(period.getId()).setPerformanceRecordId(record.getId())
                    .setOwnerUserId(record.getOwnerUserId()).setSourceUserId(record.getSourceUserId())
                    .setSourceType(record.getSourceType()).setSourceId(record.getSourceId()).setSourceItemId(record.getSourceItemId())
                    .setPerformanceAmount(performance).setRefundAmount(refund).setNetAmount(net)
                    .setRelationSnapshotJson(record.getRelationSnapshotJson()).setStatus("INCLUDED")
                    .setIdempotencyKey(key).setIsDeleted(false).setCreateTime(now).setUpdateTime(now);
            itemDao.insert(item);
            total = total.add(performance);
            reversed = reversed.add(refund);
            if (record.getOwnerUserId() != null) owners.add(record.getOwnerUserId());
        }
        period.setTotalPerformanceAmount(total).setTotalRefundAmount(reversed)
                .setNetPerformanceAmount(total.subtract(reversed).max(BigDecimal.ZERO))
                .setMemberCount(owners.size()).setUpdateTime(now);
        periodDao.updateById(period);
        return periodDao.selectById(period.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> trial(Long id) {
        JkPerformancePeriod period = require(id);
        if ("CLOSED".equals(period.getStatus())) throw new IllegalArgumentException("周期已关闭，不能重新试算");
        List<Map<String, Object>> ownerRows = ownerSummary(id);
        BigDecimal totalTrial = BigDecimal.ZERO;
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> owner : ownerRows) {
            Long ownerUserId = (Long) owner.get("ownerUserId");
            BigDecimal net = (BigDecimal) owner.get("netPerformanceAmount");
            JkCommissionRuleTrialRequest request = new JkCommissionRuleTrialRequest()
                    .setRuleId(period.getRuleId()).setScenario("PERFORMANCE_PERIOD_TRIAL")
                    .setSourceType("PERFORMANCE_PERIOD").setSourceId(period.getId()).setSourceItemId(ownerUserId)
                    .setSourceNo(period.getPeriodNo()).setBusinessTime(period.getEndTime())
                    .setSellerUserId(ownerUserId).setRegionCode(period.getRegionCode()).setQuantity(1)
                    .setBaseAmount(net).setRegisteredCustomer(true).setVoucherPresent(true).setAudited(true);
            List<JkCommissionRuleTrialResponse> trials = scenarioService.trial(request);
            BigDecimal ownerReward = BigDecimal.ZERO;
            for (JkCommissionRuleTrialResponse trial : trials) {
                if ("MATCHED".equals(trial.getMatchStatus())) ownerReward = ownerReward.add(money(trial.getCappedAmount()));
            }
            owner.put("trialRewardAmount", ownerReward);
            owner.put("trialResults", trials);
            totalTrial = totalTrial.add(ownerReward);
            results.add(owner);
        }
        period.setTrialRewardAmount(totalTrial).setStatus("PENDING_REVIEW").setUpdateTime(new Date());
        periodDao.updateById(period);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("period", periodDao.selectById(id));
        response.put("owners", results);
        response.put("notice", "试算不写佣金；关闭周期前仍需审核。内部订货和库存调拨未计入有效团队销售业绩。");
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPerformancePeriod close(Long id, JkPerformancePeriodCloseRequest closeRequest, Long operatorId) {
        JkPerformancePeriod period = require(id);
        if ("CLOSED".equals(period.getStatus())) {
            if (closeRequest.getRequestNo().equals(period.getRequestNo())) return period;
            throw new IllegalArgumentException("周期已关闭，禁止直接重算或覆盖");
        }
        if (!"PENDING_REVIEW".equals(period.getStatus())) throw new IllegalArgumentException("必须先试算并进入待审核状态");

        BigDecimal totalApproved = BigDecimal.ZERO;
        for (Map<String, Object> owner : ownerSummary(id)) {
            Long ownerUserId = (Long) owner.get("ownerUserId");
            BigDecimal net = (BigDecimal) owner.get("netPerformanceAmount");
            JkCommissionRuleTrialRequest dispatch = new JkCommissionRuleTrialRequest()
                    .setScenario("PERFORMANCE_PERIOD_CLOSED").setSourceType("PERFORMANCE_PERIOD")
                    .setSourceId(period.getId()).setSourceItemId(ownerUserId).setSourceNo(period.getPeriodNo())
                    .setBusinessTime(period.getEndTime()).setSellerUserId(ownerUserId).setRegionCode(period.getRegionCode())
                    .setQuantity(1).setBaseAmount(net).setRegisteredCustomer(true).setVoucherPresent(true).setAudited(true);
            String ownerRequestNo = closeRequest.getRequestNo() + ":" + ownerUserId;
            scenarioService.dispatch(dispatch, "PERFORMANCE_PERIOD:" + period.getId() + ":" + ownerUserId,
                    period.getPeriodNo(), ownerRequestNo);
            List<JkCommissionRecord> commissions = commissionRecordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                    .eq(JkCommissionRecord::getSourceType, "PERFORMANCE_PERIOD")
                    .eq(JkCommissionRecord::getSourceId, period.getId())
                    .eq(JkCommissionRecord::getSourceItemId, ownerUserId)
                    .eq(JkCommissionRecord::getIsDeleted, false));
            BigDecimal approved = BigDecimal.ZERO;
            Long firstCommissionId = null;
            for (JkCommissionRecord commission : commissions) {
                approved = approved.add(money(commission.getCommissionAmount()));
                if (firstCommissionId == null) firstCommissionId = commission.getId();
            }
            totalApproved = totalApproved.add(approved);
            if (rewardDao.selectCount(new LambdaQueryWrapper<JkPeriodRewardRecord>()
                    .eq(JkPeriodRewardRecord::getRequestNo, ownerRequestNo)) == 0) {
                JkPeriodRewardRecord reward = new JkPeriodRewardRecord()
                        .setRewardNo("PR" + IdWorker.getIdStr()).setPeriodId(period.getId())
                        .setOwnerUserId(ownerUserId).setTierRuleId(period.getRuleId())
                        .setPerformanceAmount(net).setRawRewardAmount(approved).setApprovedRewardAmount(approved)
                        .setStatus(approved.signum() > 0 ? "COMMISSION_CREATED" : "NO_ACTIVE_RULE")
                        .setCommissionRecordId(firstCommissionId)
                        .setCalculationSnapshotJson(JSONUtil.toJsonStr(owner)).setRequestNo(ownerRequestNo)
                        .setIsDeleted(false).setCreateTime(new Date()).setUpdateTime(new Date());
                rewardDao.insert(reward);
            }
        }
        period.setApprovedRewardAmount(totalApproved).setStatus("CLOSED").setClosedBy(operatorId)
                .setClosedAt(new Date()).setUpdateTime(new Date()).setVersion(period.getVersion() == null ? 1 : period.getVersion() + 1);
        periodDao.updateById(period);
        return periodDao.selectById(id);
    }

    private List<Map<String, Object>> ownerSummary(Long periodId) {
        List<JkPerformancePeriodItem> items = itemDao.selectList(new LambdaQueryWrapper<JkPerformancePeriodItem>()
                .eq(JkPerformancePeriodItem::getPeriodId, periodId).eq(JkPerformancePeriodItem::getIsDeleted, false)
                .eq(JkPerformancePeriodItem::getStatus, "INCLUDED"));
        Map<Long, BigDecimal> amounts = new HashMap<Long, BigDecimal>();
        Map<Long, Integer> counts = new HashMap<Long, Integer>();
        for (JkPerformancePeriodItem item : items) {
            if (item.getOwnerUserId() == null) continue;
            amounts.put(item.getOwnerUserId(), money(amounts.get(item.getOwnerUserId())).add(money(item.getNetAmount())));
            counts.put(item.getOwnerUserId(), counts.containsKey(item.getOwnerUserId()) ? counts.get(item.getOwnerUserId()) + 1 : 1);
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<Long, BigDecimal> entry : amounts.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("ownerUserId", entry.getKey());
            row.put("netPerformanceAmount", entry.getValue().setScale(2, RoundingMode.HALF_UP));
            row.put("recordCount", counts.get(entry.getKey()));
            result.add(row);
        }
        return result;
    }

    private JkPerformancePeriod require(Long id) {
        JkPerformancePeriod period = id == null ? null : periodDao.selectById(id);
        if (period == null || Boolean.TRUE.equals(period.getIsDeleted())) throw new IllegalArgumentException("周期业绩不存在");
        return period;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
