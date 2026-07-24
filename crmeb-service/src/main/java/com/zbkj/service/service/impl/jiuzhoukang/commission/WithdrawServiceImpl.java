package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkWithdrawApply;
import com.zbkj.common.model.jiuzhoukang.JkWithdrawAuditLog;
import com.zbkj.service.dao.jiuzhoukang.JkWithdrawApplyDao;
import com.zbkj.service.dao.jiuzhoukang.JkWithdrawAuditLogDao;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.jiuzhoukang.commission.FundAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.WithdrawService;
import com.zbkj.service.service.jiuzhoukang.commission.WithdrawStateSupport;
import com.zbkj.service.service.jiuzhoukang.commission.WithdrawValidationSupport;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * 九州康独立提现状态机。
 * <p>申请时冻结资金，驳回时释放，确认打款时转入已提现。每个审核与打款节点都会重新检查身份状态，
 * 防止身份冻结后仍继续完成已有提现申请。</p>
 */
@Service
public class WithdrawServiceImpl implements WithdrawService {
    /** 九州康独立提现门槛，避免复用/污染 CRMEB 原分销配置。 */
    public static final String CONFIG_KEY_JK_WITHDRAW_MINIMUM_AMOUNT = "jk_withdraw_minimum_amount";

    @Autowired private JkWithdrawApplyDao withdrawDao;
    @Autowired private JkWithdrawAuditLogDao auditLogDao;
    @Autowired private FundAccountService fundAccountService;
    @Autowired private JkUserContextService userContextService;
    @Autowired private SystemConfigService systemConfigService;

    /** 申请提现时立即把可提现资金转入提现中，避免同一余额被并发申请多次。 */
    @Override @Transactional
    public JkWithdrawApply apply(Long userId, String roleCode, BigDecimal amount, String requestNo, String payeeSnapshotJson) {
        if (requestNo == null || requestNo.trim().isEmpty()) throw new IllegalArgumentException("requestNo不能为空");
        JkWithdrawApply old = withdrawDao.selectOne(new LambdaQueryWrapper<JkWithdrawApply>().eq(JkWithdrawApply::getRequestNo, requestNo));
        if (old != null) return old;
        JkUserContext context = userContextService.getFrontContext(userId);
        if (context == null || Boolean.TRUE.equals(context.getFreezeStatus())) throw new IllegalArgumentException("身份已冻结，暂不允许提现");
        if (roleCode == null || roleCode.trim().isEmpty()) throw new IllegalArgumentException("当前身份不支持提现");
        if (!roleCode.equals(context.getPrimaryRoleCode())) throw new IllegalArgumentException("提现身份与当前生效主身份不一致");
        WithdrawValidationSupport.validateAmount(amount, configuredMinimumAmount());
        fundAccountService.freezeForWithdraw(userId, roleCode, amount, requestNo, "WITHDRAW_APPLY:" + requestNo);
        Date now = new Date();
        JkWithdrawApply apply = new JkWithdrawApply().setWithdrawNo("WD" + id()).setRequestNo(requestNo).setUserId(userId)
                .setRoleCode(roleCode).setAmount(amount).setStatus("SUBMITTED").setPayeeSnapshotJson(payeeSnapshotJson)
                .setIsDeleted(false).setVersion(0).setCreateTime(now).setUpdateTime(now);
        withdrawDao.insert(apply);
        log(apply, "APPLY", null, "SUBMITTED", userId, requestNo, "提交提现申请");
        return apply;
    }

    @Override @Transactional
    public JkWithdrawApply audit(Long id, Long operatorId, boolean approved, String requestNo, String remark) {
        JkWithdrawApply apply = withdrawDao.selectById(id);
        if (apply == null) throw new IllegalArgumentException("提现申请不存在");
        String action = approved ? "APPROVE" : "REJECT";
        if (hasLog(action, requestNo) || (!"SUBMITTED".equals(apply.getStatus()) && !"AUDITING".equals(apply.getStatus()))) return apply;
        if (approved) requirePayableIdentity(apply);
        String before = apply.getStatus();
        if ("SUBMITTED".equals(before)) apply.setStatus("AUDITING");
        String after = approved ? "APPROVED" : "REJECTED";
        if (!WithdrawStateSupport.canTransit(apply.getStatus(), after)) throw new IllegalArgumentException("提现审核状态非法");
        if (!approved) fundAccountService.releaseWithdraw(apply.getUserId(), apply.getRoleCode(), apply.getAmount(), requestNo, "WITHDRAW_REJECT:" + apply.getWithdrawNo());
        apply.setStatus(after).setAuditUserId(operatorId).setAuditTime(new Date()).setRejectReason(approved ? null : remark).setUpdateTime(new Date());
        withdrawDao.updateById(apply);
        log(apply, action, before, after, operatorId, requestNo, remark);
        return apply;
    }

    /** 确认打款是不可逆财务节点，必须校验审核状态、身份有效性和请求幂等。 */
    @Override @Transactional
    public JkWithdrawApply confirmPaid(Long id, Long operatorId, String requestNo, String remark) {
        JkWithdrawApply apply = withdrawDao.selectById(id);
        if (apply == null) throw new IllegalArgumentException("提现申请不存在");
        if (hasLog("CONFIRM_PAID", requestNo) || "PAID".equals(apply.getStatus())) return apply;
        requirePayableIdentity(apply);
        if (!WithdrawStateSupport.canTransit(apply.getStatus(), "PAID")) throw new IllegalArgumentException("当前状态不能确认打款");
        fundAccountService.confirmPaid(apply.getUserId(), apply.getRoleCode(), apply.getAmount(), requestNo, "WITHDRAW_PAID:" + apply.getWithdrawNo());
        apply.setStatus("PAID").setPaidUserId(operatorId).setPaidTime(new Date()).setUpdateTime(new Date());
        withdrawDao.updateById(apply);
        log(apply, "CONFIRM_PAID", "APPROVED", "PAID", operatorId, requestNo, remark);
        return apply;
    }

    private void requirePayableIdentity(JkWithdrawApply apply) {
        JkUserContext context = userContextService.getFrontContext(apply.getUserId());
        if (context == null || Boolean.TRUE.equals(context.getFreezeStatus())) {
            throw new IllegalArgumentException("身份已冻结，不能提现审核通过或确认打款");
        }
        if (context.getPrimaryRoleCode() == null || !context.getPrimaryRoleCode().equals(apply.getRoleCode())) {
            throw new IllegalArgumentException("提现身份已失效或已变更，请驳回后重新申请");
        }
    }

    private BigDecimal configuredMinimumAmount() {
        String value = systemConfigService.getValueByKey(CONFIG_KEY_JK_WITHDRAW_MINIMUM_AMOUNT);
        if (value == null || value.trim().isEmpty()) return null;
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException e) { throw new IllegalStateException("九州康最低提现金额配置非法"); }
    }
    private boolean hasLog(String action, String requestNo) { return requestNo != null && auditLogDao.selectOne(new LambdaQueryWrapper<JkWithdrawAuditLog>().eq(JkWithdrawAuditLog::getIdempotencyKey, action + ":" + requestNo)) != null; }
    private void log(JkWithdrawApply apply, String action, String before, String after, Long operatorId, String requestNo, String remark) { auditLogDao.insert(new JkWithdrawAuditLog().setWithdrawApplyId(apply.getId()).setWithdrawNo(apply.getWithdrawNo()).setAction(action).setBeforeStatus(before).setAfterStatus(after).setOperatorId(operatorId).setRemark(remark).setRequestNo(requestNo).setIdempotencyKey(action + ":" + requestNo).setCreateTime(new Date())); }
    private String id() { return UUID.randomUUID().toString().replace("-", "").substring(0, 16); }
}