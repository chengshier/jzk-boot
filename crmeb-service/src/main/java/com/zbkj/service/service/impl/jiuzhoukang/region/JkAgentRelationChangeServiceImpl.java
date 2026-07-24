package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.region.*;
import com.zbkj.service.service.jiuzhoukang.support.JkDictLabelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class JkAgentRelationChangeServiceImpl implements JkAgentRelationChangeService {
    @Autowired private JkAgentRelationChangeApplyDao applyDao;
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private JkUserBusinessRoleDao userBusinessRoleDao;
    @Autowired private JkStockTransferDao transferDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkStockItemDao stockItemDao;
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkWithdrawApplyDao withdrawDao;
    @Autowired private UserService userService;
    @Autowired private JkAgentRelationService relationService;
    @Autowired private JkRelationChangeBlockerService blockerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkAgentRelationChangeApply apply(Long userId, JkAgentRelationChangeApplyRequest request) {
        JkAgentRelationChangeApply old = applyDao.selectOne(new LambdaQueryWrapper<JkAgentRelationChangeApply>()
                .eq(JkAgentRelationChangeApply::getRequestNo, request.getRequestNo()).last("limit 1"));
        if (old != null) {
            if (!userId.equals(old.getUserId())) throw new IllegalArgumentException("requestNo 已被其他用户使用");
            return enrich(old);
        }
        JkAgentRelation current = currentRelation(userId);
        if (current == null || current.getParentUserId() == null) throw new IllegalArgumentException("当前没有可换绑的有效上级关系");
        if (request.getTargetParentUserId().equals(userId)) throw new IllegalArgumentException("不能绑定自己为上级");
        if (request.getTargetParentUserId().equals(current.getParentUserId())) throw new IllegalArgumentException("目标上级与当前上级相同");
        validateTargetParent(userId, request.getTargetParentUserId());
        assertNoPendingApply(userId);
        assertNoBusinessBlockers(userId);
        Date now = new Date();
        JkAgentRelationChangeApply entity = new JkAgentRelationChangeApply()
                .setApplyNo("RC" + com.baomidou.mybatisplus.core.toolkit.IdWorker.getIdStr())
                .setRequestNo(request.getRequestNo()).setUserId(userId).setCurrentRelationId(current.getId())
                .setCurrentParentUserId(current.getParentUserId()).setTargetParentUserId(request.getTargetParentUserId())
                .setApplyReason(request.getApplyReason()).setStatus("PENDING").setIsDeleted(false)
                .setCreateUserId(userId).setUpdateUserId(userId).setCreateTime(now).setUpdateTime(now);
        applyDao.insert(entity);
        return enrich(entity);
    }

    @Override
    public PageInfo<JkAgentRelationChangeApply> listMine(Long userId, PageParamRequest pageParam) {
        Page<JkAgentRelationChangeApply> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        List<JkAgentRelationChangeApply> rows = applyDao.selectList(new LambdaQueryWrapper<JkAgentRelationChangeApply>()
                .eq(JkAgentRelationChangeApply::getUserId, userId).eq(JkAgentRelationChangeApply::getIsDeleted, false)
                .orderByDesc(JkAgentRelationChangeApply::getId));
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public PageInfo<JkAgentRelationChangeApply> listAdmin(String status, Long userId, PageParamRequest pageParam) {
        Page<JkAgentRelationChangeApply> page = PageHelper.startPage(pageParam.getPage(), pageParam.getLimit());
        LambdaQueryWrapper<JkAgentRelationChangeApply> q = new LambdaQueryWrapper<JkAgentRelationChangeApply>()
                .eq(JkAgentRelationChangeApply::getIsDeleted, false).orderByDesc(JkAgentRelationChangeApply::getId);
        if (StrUtil.isNotBlank(status)) q.eq(JkAgentRelationChangeApply::getStatus, status);
        if (userId != null) q.eq(JkAgentRelationChangeApply::getUserId, userId);
        List<JkAgentRelationChangeApply> rows = applyDao.selectList(q);
        rows.forEach(this::enrich);
        return CommonPage.copyPageInfo(page, rows);
    }

    @Override
    public JkAgentRelationChangeApply detail(Long viewerUserId, Long id, boolean admin) {
        JkAgentRelationChangeApply value = require(id);
        if (!admin && !viewerUserId.equals(value.getUserId())) throw new IllegalArgumentException("无权查看该换绑申请");
        return enrichWithBlockers(value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkAgentRelationChangeApply audit(Long operatorId, JkAgentRelationChangeAuditRequest request) {
        JkAgentRelationChangeApply entity = require(request.getApplyId());
        if (!"PENDING".equals(entity.getStatus())) return enrich(entity);
        Date now = new Date();
        if (!Boolean.TRUE.equals(request.getApproved())) {
            entity.setStatus("REJECTED").setRejectReason(request.getRemark()).setAuditRemark(request.getRemark())
                    .setAuditUserId(operatorId).setAuditTime(now).setUpdateUserId(operatorId).setUpdateTime(now);
            applyDao.updateById(entity);
            return enrich(entity);
        }
        blockerService.assertNoBlockers(entity);
        validateTargetParent(entity.getUserId(), entity.getTargetParentUserId());
        JkAgentRelation current = currentRelation(entity.getUserId());
        if (current == null || !Objects.equals(current.getId(), entity.getCurrentRelationId())) {
            throw new IllegalArgumentException("当前关系已变化，请驳回后重新提交换绑申请");
        }
        JkAgentRelationBindRequest bind = new JkAgentRelationBindRequest();
        bind.setUserId(entity.getUserId());
        bind.setParentUserId(entity.getTargetParentUserId());
        bind.setRelationType("DIRECT");
        bind.setBindSource("CHANGE_APPLY");
        bind.setSourceCode(entity.getApplyNo());
        bind.setChangeReason(entity.getApplyReason());
        bind.setRemark(StrUtil.blankToDefault(request.getRemark(), "换绑申请审核通过"));
        relationService.bind(bind, operatorId);
        JkAgentRelation newRelation = currentRelation(entity.getUserId());
        entity.setStatus("APPROVED").setAuditRemark(request.getRemark()).setNewRelationId(newRelation == null ? null : newRelation.getId())
                .setAuditUserId(operatorId).setAuditTime(now).setUpdateUserId(operatorId).setUpdateTime(now);
        applyDao.updateById(entity);
        return enrich(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkAgentRelationChangeApply cancel(Long userId, Long id, String requestNo, String reason) {
        JkAgentRelationChangeApply entity = require(id);
        if (!userId.equals(entity.getUserId())) throw new IllegalArgumentException("无权取消该换绑申请");
        if (!"PENDING".equals(entity.getStatus())) throw new IllegalArgumentException("当前状态不能取消");
        entity.setStatus("CANCELLED").setAuditRemark(reason).setUpdateUserId(userId).setUpdateTime(new Date());
        applyDao.updateById(entity);
        return enrich(entity);
    }

    /**
     * 服务端强制校验换绑目标，不能只依赖 App 选择器。
     * 目标必须具备有效且未冻结的创客/合伙人/区县代身份；申请人已绑定区域时，
     * 目标上级必须属于同一区域。审核通过前再次执行，避免申请期间目标身份失效。
     */
    private void validateTargetParent(Long applicantUserId, Long targetParentUserId) {
        if (targetParentUserId == null) throw new IllegalArgumentException("请选择目标上级");
        if (Objects.equals(applicantUserId, targetParentUserId)) throw new IllegalArgumentException("不能绑定自己为上级");
        if (userService.getById(targetParentUserId.intValue()) == null) throw new IllegalArgumentException("目标上级用户不存在");

        List<String> eligibleRoles = Arrays.asList(
                JkBizConstants.ROLE_MAKER,
                JkBizConstants.ROLE_PARTNER,
                JkBizConstants.ROLE_COUNTY_AGENT
        );
        JkUserBusinessRole targetRole = userBusinessRoleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, targetParentUserId)
                .in(JkUserBusinessRole::getRoleCode, eligibleRoles)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getEffectiveStatus, JkBizConstants.EFFECTIVE_STATUS_ENABLED)
                .eq(JkUserBusinessRole::getFreezeStatus, false)
                .eq(JkUserBusinessRole::getStatus, true)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId)
                .last("limit 1"));
        if (targetRole == null) throw new IllegalArgumentException("目标上级身份无效、已冻结或已停用");

        JkUserBusinessRole applicantRole = userBusinessRoleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getUserId, applicantUserId)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getEffectiveStatus, JkBizConstants.EFFECTIVE_STATUS_ENABLED)
                .eq(JkUserBusinessRole::getFreezeStatus, false)
                .eq(JkUserBusinessRole::getStatus, true)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId)
                .last("limit 1"));
        if (applicantRole == null) throw new IllegalArgumentException("当前业务身份无效、已冻结或已停用，不能换绑");
        if (StrUtil.isNotBlank(applicantRole.getRegionCode())
                && !Objects.equals(applicantRole.getRegionCode(), targetRole.getRegionCode())) {
            throw new IllegalArgumentException("目标上级与当前用户不在同一区域，不能跨区域换绑");
        }
    }

    private void assertNoPendingApply(Long userId) {
        Integer count = applyDao.selectCount(new LambdaQueryWrapper<JkAgentRelationChangeApply>()
                .eq(JkAgentRelationChangeApply::getUserId, userId).eq(JkAgentRelationChangeApply::getStatus, "PENDING")
                .eq(JkAgentRelationChangeApply::getIsDeleted, false));
        if (count != null && count > 0) throw new IllegalArgumentException("已有待审核换绑申请，请勿重复提交");
    }

    private void assertNoBusinessBlockers(Long userId) {
        Integer activeTransfers = transferDao.selectCount(new LambdaQueryWrapper<JkStockTransfer>()
                .eq(JkStockTransfer::getUserId, userId).eq(JkStockTransfer::getIsDeleted, false)
                .notIn(JkStockTransfer::getStatus, Arrays.asList("STOCK_IN", "CLOSED", "CANCELLED", "AUDIT_REJECTED")));
        if (activeTransfers != null && activeTransfers > 0) throw new IllegalArgumentException("存在未完成调拨单，暂不能换绑");
        List<JkStockAccount> accounts = stockAccountDao.selectList(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getOwnerUserId, userId).eq(JkStockAccount::getStatus, true).eq(JkStockAccount::getIsDeleted, false));
        for (JkStockAccount account : accounts) {
            List<JkStockItem> items = stockItemDao.selectList(new LambdaQueryWrapper<JkStockItem>()
                    .eq(JkStockItem::getStockAccountId, account.getId()).eq(JkStockItem::getIsDeleted, false));
            for (JkStockItem item : items) {
                if (safe(item.getAvailableQty()) > 0 || safe(item.getFrozenQty()) > 0) throw new IllegalArgumentException("当前仍有库存余额或冻结库存，暂不能换绑");
            }
        }
        for (JkCommissionAccount account : commissionAccountDao.selectList(new LambdaQueryWrapper<JkCommissionAccount>()
                .eq(JkCommissionAccount::getUserId, userId).eq(JkCommissionAccount::getIsDeleted, false))) {
            if (money(account.getPendingSettleAmount()).signum() > 0) throw new IllegalArgumentException("存在未结算佣金，暂不能换绑");
        }
        Integer withdrawing = withdrawDao.selectCount(new LambdaQueryWrapper<JkWithdrawApply>()
                .eq(JkWithdrawApply::getUserId, userId).eq(JkWithdrawApply::getIsDeleted, false)
                .in(JkWithdrawApply::getStatus, Arrays.asList("SUBMITTED", "AUDITING", "APPROVED")));
        if (withdrawing != null && withdrawing > 0) throw new IllegalArgumentException("存在处理中提现申请，暂不能换绑");
    }

    private JkAgentRelation currentRelation(Long userId) {
        return relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>().eq(JkAgentRelation::getUserId, userId)
                .eq(JkAgentRelation::getStatus, true).eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
    }

    private JkAgentRelationChangeApply require(Long id) {
        JkAgentRelationChangeApply value = applyDao.selectById(id);
        if (value == null || Boolean.TRUE.equals(value.getIsDeleted())) throw new IllegalArgumentException("换绑申请不存在");
        return value;
    }

    private JkAgentRelationChangeApply enrichWithBlockers(JkAgentRelationChangeApply row) {
        enrich(row);
        return blockerService.fill(row);
    }

    private JkAgentRelationChangeApply enrich(JkAgentRelationChangeApply row) {
        User user = row.getUserId() == null ? null : userService.getById(row.getUserId().intValue());
        User current = row.getCurrentParentUserId() == null ? null : userService.getById(row.getCurrentParentUserId().intValue());
        User target = row.getTargetParentUserId() == null ? null : userService.getById(row.getTargetParentUserId().intValue());
        if (user != null) { row.setUserName(StrUtil.blankToDefault(user.getRealName(), user.getNickname())); row.setUserPhone(user.getPhone()); }
        if (current != null) row.setCurrentParentName(StrUtil.blankToDefault(current.getRealName(), current.getNickname()));
        if (target != null) row.setTargetParentName(StrUtil.blankToDefault(target.getRealName(), target.getNickname()));
        row.setStatusText(JkDictLabelHelper.label("relation_change_status", row.getStatus()));
        row.setStatusTag(statusTag(row.getStatus()));
        return row;
    }

    private int safe(Integer v) { return v == null ? 0 : v; }
    private BigDecimal money(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private String statusTag(String s) { if ("APPROVED".equals(s)) return "success"; if ("REJECTED".equals(s) || "CANCELLED".equals(s)) return "danger"; return "warning"; }
}
