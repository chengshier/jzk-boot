package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelationChangeApply;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkWithdrawApply;
import com.zbkj.common.response.jiuzhoukang.JkRelationChangeBlockerResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkWithdrawApplyDao;
import com.zbkj.service.service.jiuzhoukang.region.JkRelationChangeBlockerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/** 换绑审核阻断项结构化检查实现。 */
@Service
public class JkRelationChangeBlockerServiceImpl implements JkRelationChangeBlockerService {
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private JkStockTransferDao transferDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkWithdrawApplyDao withdrawDao;

    @Override
    public List<JkRelationChangeBlockerResponse> check(JkAgentRelationChangeApply apply) {
        List<JkRelationChangeBlockerResponse> result = new ArrayList<>();
        if (apply == null || apply.getUserId() == null) return result;
        Long userId = apply.getUserId();

        JkAgentRelation current = relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getUserId, userId)
                .eq(JkAgentRelation::getStatus, true)
                .eq(JkAgentRelation::getIsDeleted, false)
                .last("limit 1"));
        boolean relationChanged = current == null || !Objects.equals(current.getId(), apply.getCurrentRelationId());
        result.add(item("RELATION_CHANGED", "当前上级关系",
                relationChanged,
                relationChanged ? "已变化" : "未变化", "",
                relationChanged ? "申请提交后当前上级关系已变化，不能直接通过原申请。" : "当前关系与申请快照一致。",
                "驳回当前申请，由用户重新提交换绑申请"));

        JkUserBusinessRole applicantRole = primaryRole(userId, null);
        JkUserBusinessRole targetRole = primaryRole(apply.getTargetParentUserId(), Arrays.asList(
                JkBizConstants.ROLE_MAKER, JkBizConstants.ROLE_PARTNER, JkBizConstants.ROLE_COUNTY_AGENT));
        boolean targetInvalid = targetRole == null;
        result.add(item("TARGET_IDENTITY_INVALID", "目标上级身份",
                targetInvalid,
                targetInvalid ? "无效" : targetRole.getRoleCode(), "",
                targetInvalid ? "目标上级身份无效、已冻结或已停用。" : "目标上级具备有效业务身份。",
                "选择有效且未冻结的创客、合伙人或区县代"));

        boolean crossRegion = applicantRole != null && targetRole != null
                && StrUtil.isNotBlank(applicantRole.getRegionCode())
                && !Objects.equals(applicantRole.getRegionCode(), targetRole.getRegionCode());
        result.add(item("CROSS_REGION", "区域一致性",
                crossRegion,
                crossRegion ? applicantRole.getRegionCode() + " → " + targetRole.getRegionCode() : "同区域", "",
                crossRegion ? "申请人与目标上级不在同一区域。" : "申请人与目标上级区域校验通过。",
                "选择同一区域的目标上级"));

        Integer activeTransfers = transferDao.selectCount(new LambdaQueryWrapper<JkStockTransfer>()
                .eq(JkStockTransfer::getUserId, userId)
                .eq(JkStockTransfer::getIsDeleted, false)
                .notIn(JkStockTransfer::getStatus,
                        Arrays.asList("STOCK_IN", "CLOSED", "CANCELLED", "AUDIT_REJECTED")));
        int transferCount = activeTransfers == null ? 0 : activeTransfers;
        result.add(item("ACTIVE_TRANSFER", "未完成调拨",
                transferCount > 0, String.valueOf(transferCount), "单",
                transferCount > 0 ? "仍有未完成调拨，换绑会影响库存责任归属。" : "没有未完成调拨单。",
                "先完成、取消或关闭全部调拨单"));

        int availableQty = 0;
        int frozenQty = 0;
        List<JkStockAccount> accounts = stockAccountDao.selectList(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getOwnerUserId, userId)
                .eq(JkStockAccount::getStatus, true)
                .eq(JkStockAccount::getIsDeleted, false));
        for (JkStockAccount account : accounts) {
            List<JkStockItem> items = stockItemDao.selectList(new LambdaQueryWrapper<JkStockItem>()
                    .eq(JkStockItem::getStockAccountId, account.getId())
                    .eq(JkStockItem::getIsDeleted, false));
            for (JkStockItem stockItem : items) {
                availableQty += safe(stockItem.getAvailableQty());
                frozenQty += safe(stockItem.getFrozenQty());
            }
        }
        int totalStock = availableQty + frozenQty;
        result.add(item("STOCK_BALANCE", "库存余额",
                totalStock > 0, availableQty + " 可用 / " + frozenQty + " 冻结", "件",
                totalStock > 0 ? "仍有可用或冻结库存，不能变更库存责任关系。" : "库存余额和冻结库存均为零。",
                "完成库存调拨、退回或人工核对后再换绑"));

        BigDecimal pendingCommission = BigDecimal.ZERO;
        List<JkCommissionAccount> commissionAccounts = commissionAccountDao.selectList(
                new LambdaQueryWrapper<JkCommissionAccount>()
                        .eq(JkCommissionAccount::getUserId, userId)
                        .eq(JkCommissionAccount::getIsDeleted, false));
        for (JkCommissionAccount account : commissionAccounts) {
            pendingCommission = pendingCommission.add(money(account.getPendingSettleAmount()));
        }
        result.add(item("PENDING_COMMISSION", "待结算佣金",
                pendingCommission.signum() > 0, pendingCommission.toPlainString(), "元",
                pendingCommission.signum() > 0 ? "仍有待结算佣金，关系变更可能影响结算责任。" : "没有待结算佣金。",
                "等待佣金结算完成或完成账务核对"));

        Integer withdrawing = withdrawDao.selectCount(new LambdaQueryWrapper<JkWithdrawApply>()
                .eq(JkWithdrawApply::getUserId, userId)
                .eq(JkWithdrawApply::getIsDeleted, false)
                .in(JkWithdrawApply::getStatus, Arrays.asList("SUBMITTED", "AUDITING", "APPROVED")));
        int withdrawCount = withdrawing == null ? 0 : withdrawing;
        result.add(item("WITHDRAW_PROCESSING", "处理中提现",
                withdrawCount > 0, String.valueOf(withdrawCount), "笔",
                withdrawCount > 0 ? "存在处理中提现申请，需先完成资金闭环。" : "没有处理中提现申请。",
                "等待提现完成、驳回或取消"));
        return result;
    }

    @Override
    public JkAgentRelationChangeApply fill(JkAgentRelationChangeApply apply) {
        List<JkRelationChangeBlockerResponse> blockers = check(apply);
        boolean passed = true;
        for (JkRelationChangeBlockerResponse blocker : blockers) {
            if (Boolean.TRUE.equals(blocker.getBlocked())) {
                passed = false;
                break;
            }
        }
        apply.setBlockerItems(blockers).setBlockerPassed(passed).setBlockerCheckTime(new Date());
        return apply;
    }

    @Override
    public void assertNoBlockers(JkAgentRelationChangeApply apply) {
        List<JkRelationChangeBlockerResponse> blockers = check(apply);
        for (JkRelationChangeBlockerResponse blocker : blockers) {
            if (Boolean.TRUE.equals(blocker.getBlocked())) {
                throw new IllegalArgumentException(blocker.getLabel() + "未通过：" + blocker.getDescription());
            }
        }
    }

    private JkUserBusinessRole primaryRole(Long userId, List<String> eligibleRoles) {
        if (userId == null) return null;
        LambdaQueryWrapper<JkUserBusinessRole> query = new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, userId)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getEffectiveStatus, JkBizConstants.EFFECTIVE_STATUS_ENABLED)
                .eq(JkUserBusinessRole::getFreezeStatus, false)
                .eq(JkUserBusinessRole::getStatus, true)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId)
                .last("limit 1");
        if (eligibleRoles != null && !eligibleRoles.isEmpty()) query.in(JkUserBusinessRole::getRoleCode, eligibleRoles);
        return userRoleDao.selectOne(query);
    }

    private JkRelationChangeBlockerResponse item(String code, String label, boolean blocked,
                                                  String value, String unit, String description, String actionHint) {
        return new JkRelationChangeBlockerResponse().setCode(code).setLabel(label).setBlocked(blocked)
                .setValue(value).setUnit(unit).setDescription(description).setActionHint(actionHint);
    }

    private int safe(Integer value) { return value == null ? 0 : value; }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
