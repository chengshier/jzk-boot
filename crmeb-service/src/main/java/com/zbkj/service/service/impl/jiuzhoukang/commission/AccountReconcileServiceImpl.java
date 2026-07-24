package com.zbkj.service.service.impl.jiuzhoukang.commission;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.*;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.commission.AccountReconcileService;
import com.zbkj.service.service.jiuzhoukang.support.JkDictLabelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AccountReconcileServiceImpl implements AccountReconcileService {
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkFundAccountDao fundAccountDao;
    @Autowired private JkCommissionRecordDao recordDao;
    @Autowired private JkCommissionReverseDao reverseDao;
    @Autowired private JkWithdrawApplyDao withdrawDao;
    @Autowired private JkAccountReconcileRecordDao reconcileDao;
    @Autowired private UserService userService;
    @Autowired private JkBusinessRoleDao businessRoleDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<JkAccountReconcileRecord> reconcile(Long userId, String roleCode, Long operatorId, String batchNo) {
        String batch = StrUtil.blankToDefault(batchNo, "AR" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr());
        LambdaQueryWrapper<JkCommissionAccount> q = new LambdaQueryWrapper<JkCommissionAccount>()
                .eq(JkCommissionAccount::getIsDeleted, false).orderByAsc(JkCommissionAccount::getUserId);
        if (userId != null) q.eq(JkCommissionAccount::getUserId, userId);
        if (StrUtil.isNotBlank(roleCode)) q.eq(JkCommissionAccount::getRoleCode, roleCode);
        List<JkAccountReconcileRecord> result = new ArrayList<>();
        for (JkCommissionAccount ca : commissionAccountDao.selectList(q)) {
            JkAccountReconcileRecord existed = reconcileDao.selectOne(new LambdaQueryWrapper<JkAccountReconcileRecord>()
                    .eq(JkAccountReconcileRecord::getBatchNo, batch).eq(JkAccountReconcileRecord::getUserId, ca.getUserId())
                    .eq(JkAccountReconcileRecord::getRoleCode, ca.getRoleCode()).last("limit 1"));
            if (existed != null) { enrich(existed); result.add(existed); continue; }
            JkFundAccount fa = fundAccountDao.selectOne(new LambdaQueryWrapper<JkFundAccount>()
                    .eq(JkFundAccount::getUserId, ca.getUserId()).eq(JkFundAccount::getRoleCode, ca.getRoleCode())
                    .eq(JkFundAccount::getIsDeleted, false).last("limit 1"));
            if (fa == null) fa = emptyFund(ca);
            List<JkCommissionRecord> records = recordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                    .eq(JkCommissionRecord::getReceiverUserId, ca.getUserId()).eq(JkCommissionRecord::getReceiverRoleCode, ca.getRoleCode())
                    .eq(JkCommissionRecord::getIsDeleted, false));
            Set<Long> recordIds = records.stream().map(JkCommissionRecord::getId).collect(Collectors.toSet());
            List<JkCommissionReverse> reverses = recordIds.isEmpty() ? Collections.emptyList() : reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>()
                    .in(JkCommissionReverse::getOriginalCommissionRecordId, recordIds).eq(JkCommissionReverse::getStatus, "SUCCESS"));
            Map<Long, BigDecimal> reversedByRecord = reverses.stream().collect(Collectors.groupingBy(JkCommissionReverse::getOriginalCommissionRecordId,
                    Collectors.reducing(BigDecimal.ZERO, r -> money(r.getReverseAmount()), BigDecimal::add)));
            BigDecimal expectedPending = BigDecimal.ZERO;
            BigDecimal expectedTotal = BigDecimal.ZERO;
            for (JkCommissionRecord record : records) {
                expectedTotal = expectedTotal.add(money(record.getCommissionAmount()));
                if ("PENDING_SETTLE".equals(record.getStatus()) || "CREATED".equals(record.getStatus())) {
                    expectedPending = expectedPending.add(money(record.getCommissionAmount()).subtract(reversedByRecord.getOrDefault(record.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO));
                }
            }
            BigDecimal expectedReversed = reverses.stream().map(r -> money(r.getReverseAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<JkWithdrawApply> withdraws = withdrawDao.selectList(new LambdaQueryWrapper<JkWithdrawApply>()
                    .eq(JkWithdrawApply::getUserId, ca.getUserId()).eq(JkWithdrawApply::getRoleCode, ca.getRoleCode())
                    .eq(JkWithdrawApply::getIsDeleted, false));
            BigDecimal expectedWithdrawing = sumWithdraw(withdraws, Arrays.asList("SUBMITTED", "AUDITING", "APPROVED"));
            BigDecimal expectedWithdrawn = sumWithdraw(withdraws, Collections.singletonList("PAID"));
            BigDecimal commissionNet = money(ca.getSettledAmount()).add(money(ca.getFrozenCommissionAmount())).subtract(money(ca.getNegativeOffsetAmount()));
            BigDecimal fundNet = money(fa.getAvailableAmount()).add(money(fa.getWithdrawingAmount())).add(money(fa.getWithdrawnAmount()))
                    .add(money(fa.getFrozenAmount())).subtract(money(fa.getNegativeOffsetAmount()));
            JkAccountReconcileRecord row = new JkAccountReconcileRecord().setBatchNo(batch).setUserId(ca.getUserId()).setRoleCode(ca.getRoleCode())
                    .setExpectedPendingAmount(expectedPending).setActualPendingAmount(money(ca.getPendingSettleAmount())).setPendingDifference(money(ca.getPendingSettleAmount()).subtract(expectedPending))
                    .setExpectedTotalCommissionAmount(expectedTotal).setActualTotalCommissionAmount(money(ca.getTotalCommissionAmount())).setTotalCommissionDifference(money(ca.getTotalCommissionAmount()).subtract(expectedTotal))
                    .setExpectedReversedAmount(expectedReversed).setActualReversedAmount(money(ca.getReversedAmount())).setReversedDifference(money(ca.getReversedAmount()).subtract(expectedReversed))
                    .setExpectedWithdrawingAmount(expectedWithdrawing).setActualWithdrawingAmount(money(fa.getWithdrawingAmount())).setWithdrawingDifference(money(fa.getWithdrawingAmount()).subtract(expectedWithdrawing))
                    .setExpectedWithdrawnAmount(expectedWithdrawn).setActualWithdrawnAmount(money(fa.getWithdrawnAmount())).setWithdrawnDifference(money(fa.getWithdrawnAmount()).subtract(expectedWithdrawn))
                    .setCommissionNetBalance(commissionNet).setFundNetBalance(fundNet).setCrossAccountDifference(fundNet.subtract(commissionNet))
                    .setOperatorId(operatorId).setReconcileTime(new Date()).setCreateTime(new Date());
            List<String> issues = issues(row);
            row.setReconcileStatus(issues.isEmpty() ? "BALANCED" : "DIFFERENCE").setIssueSummary(String.join("；", issues));
            reconcileDao.insert(row);
            enrich(row);
            result.add(row);
        }
        return result;
    }

    @Override
    public PageInfo<JkAccountReconcileRecord> list(String batchNo, String status, Long userId, PageParamRequest pageParam) {
        Page<JkAccountReconcileRecord> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkAccountReconcileRecord> q = new LambdaQueryWrapper<JkAccountReconcileRecord>().orderByDesc(JkAccountReconcileRecord::getId);
        if (StrUtil.isNotBlank(batchNo)) q.eq(JkAccountReconcileRecord::getBatchNo, batchNo);
        if (StrUtil.isNotBlank(status)) q.eq(JkAccountReconcileRecord::getReconcileStatus, status);
        if (userId != null) q.eq(JkAccountReconcileRecord::getUserId, userId);
        List<JkAccountReconcileRecord> rows = reconcileDao.selectList(q);
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    private List<String> issues(JkAccountReconcileRecord r) {
        List<String> v = new ArrayList<>();
        if (nonZero(r.getPendingDifference())) v.add("待结算佣金不一致");
        if (nonZero(r.getTotalCommissionDifference())) v.add("累计佣金不一致");
        if (nonZero(r.getReversedDifference())) v.add("累计冲正不一致");
        if (nonZero(r.getWithdrawingDifference())) v.add("提现中金额不一致");
        if (nonZero(r.getWithdrawnDifference())) v.add("已提现金额不一致");
        if (nonZero(r.getCrossAccountDifference())) v.add("佣金账户与资金账户镜像不一致");
        return v;
    }
    private BigDecimal sumWithdraw(List<JkWithdrawApply> rows, List<String> statuses) { return rows.stream().filter(r -> statuses.contains(r.getStatus())).map(r -> money(r.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private boolean nonZero(BigDecimal v) { return money(v).abs().compareTo(new BigDecimal("0.01")) >= 0; }
    private BigDecimal money(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private JkFundAccount emptyFund(JkCommissionAccount ca) { return new JkFundAccount().setUserId(ca.getUserId()).setRoleCode(ca.getRoleCode()).setAvailableAmount(BigDecimal.ZERO).setWithdrawingAmount(BigDecimal.ZERO).setWithdrawnAmount(BigDecimal.ZERO).setFrozenAmount(BigDecimal.ZERO).setNegativeOffsetAmount(BigDecimal.ZERO); }
    private void enrich(JkAccountReconcileRecord row) {
        if (row.getUserId() != null) {
            User user = userService.getById(row.getUserId().intValue());
            if (user != null) {
                row.setApplicantName(StrUtil.blankToDefault(user.getRealName(), user.getNickname()));
                row.setApplicantPhone(user.getPhone());
            }
        }
        if (StrUtil.isNotBlank(row.getRoleCode())) {
            JkBusinessRole role = businessRoleDao.selectOne(new LambdaQueryWrapper<JkBusinessRole>()
                    .eq(JkBusinessRole::getRoleCode, row.getRoleCode()).eq(JkBusinessRole::getIsDeleted, false).last("limit 1"));
            row.setRoleName(role == null ? row.getRoleCode() : role.getRoleName());
        }
        row.setStatusText(JkDictLabelHelper.label("account_reconcile_status", row.getReconcileStatus()));
        row.setStatusTag("BALANCED".equals(row.getReconcileStatus()) ? "success" : "danger");
    }
}
