package com.zbkj.admin.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.request.jiuzhoukang.JkWithdrawAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionSettleRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionReverseRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.commission.WithdrawService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionSettleService;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionReverseService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("api/admin/jk")
@Api(tags = "九州康分佣提现管理")
public class JkWithdrawAdminController {
    @Autowired
    private WithdrawService withdrawService;
    @Autowired
    private CommissionReverseService reverseService;
    @Autowired
    private CommissionSettleService settleService;
    @Autowired
    private JkCommissionSettleTaskDao settleTaskDao;
    @Autowired
    private JkAdminActorService adminActorService;
    @Autowired
    private JkWithdrawApplyDao withdrawDao;
    @Autowired
    private JkCommissionRecordDao recordDao;
    @Autowired
    private JkCommissionFlowDao commissionFlowDao;
    @Autowired
    private JkCommissionAccountDao commissionAccountDao;
    @Autowired
    private JkFundAccountDao fundAccountDao;
    @Autowired
    private JkFundFlowDao fundFlowDao;
    @Autowired
    private JkCommissionReverseDao reverseDao;
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;
    @Autowired
    private JkUserContextService userContextService;

    @GetMapping("/withdraw/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_VIEW_SELF, checkDataScope = true)
    public CommonResult<List<JkWithdrawApply>> withdrawList(@RequestParam(required = false) String status, @RequestParam(required = false) String roleCode, @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<JkWithdrawApply> q = new LambdaQueryWrapper<JkWithdrawApply>().eq(JkWithdrawApply::getIsDeleted, false).orderByDesc(JkWithdrawApply::getId);
        if (status != null && !status.trim().isEmpty()) q.eq(JkWithdrawApply::getStatus, status);
        if (roleCode != null && !roleCode.trim().isEmpty()) q.eq(JkWithdrawApply::getRoleCode, roleCode);
        Long scopedUserId = scopedUserId(userId);
        if (scopedUserId != null) q.eq(JkWithdrawApply::getUserId, scopedUserId);
        List<JkWithdrawApply> list = withdrawDao.selectList(q);
        displayEnrichmentSupport.enrichWithdrawApplies(list);
        return CommonResult.success(list);
    }

    @GetMapping("/withdraw/detail/{id}")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_VIEW_SELF, checkDataScope = true)
    public CommonResult<JkWithdrawApply> withdrawDetail(@PathVariable Long id) {
        JkWithdrawApply detail = withdrawDao.selectById(id);
        assertOwnOrPlatform(detail == null ? null : detail.getUserId());
        displayEnrichmentSupport.enrichWithdrawApplies(Collections.singletonList(detail));
        return CommonResult.success(detail);
    }

    @PostMapping("/withdraw/audit")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_AUDIT + "')")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_AUDIT, checkDataScope = false)
    public CommonResult<JkWithdrawApply> audit(@RequestBody JkWithdrawAuditRequest r) {
        return CommonResult.success(withdrawService.audit(r.getId(), operator(), Boolean.TRUE.equals(r.getApproved()), r.getRequestNo(), r.getRemark()));
    }

    @PostMapping("/withdraw/confirm-paid")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_CONFIRM_PAID + "')")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_CONFIRM_PAID, checkDataScope = false)
    public CommonResult<JkWithdrawApply> paid(@RequestBody JkWithdrawAuditRequest r) {
        return CommonResult.success(withdrawService.confirmPaid(r.getId(), operator(), r.getRequestNo(), r.getRemark()));
    }

    @GetMapping("/commission/record/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RECORD_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_RECORD_VIEW, checkDataScope = true)
    public CommonResult<List<JkCommissionRecord>> records(@RequestParam(required = false) String status, @RequestParam(required = false) String sourceType, @RequestParam(required = false) String receiverRoleCode, @RequestParam(required = false) Long receiverUserId) {
        LambdaQueryWrapper<JkCommissionRecord> q = new LambdaQueryWrapper<JkCommissionRecord>().eq(JkCommissionRecord::getIsDeleted, false).orderByDesc(JkCommissionRecord::getId);
        if (status != null && !status.trim().isEmpty()) q.eq(JkCommissionRecord::getStatus, status);
        if (sourceType != null && !sourceType.trim().isEmpty()) q.eq(JkCommissionRecord::getSourceType, sourceType);
        if (receiverRoleCode != null && !receiverRoleCode.trim().isEmpty()) q.eq(JkCommissionRecord::getReceiverRoleCode, receiverRoleCode);
        Long scopedReceiverUserId = scopedUserId(receiverUserId);
        if (scopedReceiverUserId != null) q.eq(JkCommissionRecord::getReceiverUserId, scopedReceiverUserId);
        List<JkCommissionRecord> list = recordDao.selectList(q);
        displayEnrichmentSupport.enrichCommissionRecords(list);
        return CommonResult.success(list);
    }

    @GetMapping("/commission/flow/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_FLOW_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_ACCOUNT_VIEW, checkDataScope = true)
    public CommonResult<List<JkCommissionFlow>> commissionFlows(@RequestParam(required = false) String flowType,
                                                                  @RequestParam(required = false) String requestNo) {
        LambdaQueryWrapper<JkCommissionFlow> q = new LambdaQueryWrapper<JkCommissionFlow>().orderByDesc(JkCommissionFlow::getId);
        if (flowType != null && !flowType.trim().isEmpty()) q.eq(JkCommissionFlow::getFlowType, flowType.trim());
        if (requestNo != null && !requestNo.trim().isEmpty()) q.eq(JkCommissionFlow::getRequestNo, requestNo.trim());
        JkUserContext context = userContextService.getAdminContext();
        if (!isPlatformAll(context)) {
            List<JkCommissionAccount> accounts = commissionAccountDao.selectList(new LambdaQueryWrapper<JkCommissionAccount>()
                    .eq(JkCommissionAccount::getUserId, context.getUserId()).eq(JkCommissionAccount::getIsDeleted, false));
            List<Long> accountIds = accounts.stream().map(JkCommissionAccount::getId).collect(java.util.stream.Collectors.toList());
            if (accountIds.isEmpty()) return CommonResult.success(Collections.<JkCommissionFlow>emptyList());
            q.in(JkCommissionFlow::getAccountId, accountIds);
        }
        return CommonResult.success(commissionFlowDao.selectList(q));
    }

    @GetMapping("/commission/account/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_ACCOUNT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_ACCOUNT_VIEW, checkDataScope = true)
    public CommonResult<List<JkCommissionAccount>> commissionAccounts(@RequestParam(required = false) String roleCode, @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<JkCommissionAccount> q = new LambdaQueryWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getIsDeleted, false).orderByDesc(JkCommissionAccount::getId);
        if (roleCode != null && !roleCode.trim().isEmpty()) q.eq(JkCommissionAccount::getRoleCode, roleCode);
        Long scopedUserId = scopedUserId(userId);
        if (scopedUserId != null) q.eq(JkCommissionAccount::getUserId, scopedUserId);
        List<JkCommissionAccount> list = commissionAccountDao.selectList(q);
        displayEnrichmentSupport.enrichCommissionAccounts(list);
        return CommonResult.success(list);
    }

    @GetMapping("/commission/account/{id}/detail")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_ACCOUNT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_ACCOUNT_VIEW, checkDataScope = true)
    public CommonResult<Map<String, Object>> commissionAccountDetail(@PathVariable Long id) {
        JkCommissionAccount account = commissionAccountDao.selectById(id);
        if (account == null || Boolean.TRUE.equals(account.getIsDeleted())) throw new IllegalArgumentException("佣金账户不存在");
        assertOwnOrPlatform(account.getUserId());

        return CommonResult.success(buildAccountDetail(account, null));
    }

    @GetMapping("/fund/account/{id}/detail")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_FUND_ACCOUNT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.FUND_ACCOUNT_VIEW, checkDataScope = true)
    public CommonResult<Map<String, Object>> fundAccountDetail(@PathVariable Long id) {
        JkFundAccount fundAccount = fundAccountDao.selectById(id);
        if (fundAccount == null || Boolean.TRUE.equals(fundAccount.getIsDeleted())) throw new IllegalArgumentException("资金账户不存在");
        assertOwnOrPlatform(fundAccount.getUserId());
        JkCommissionAccount commissionAccount = commissionAccountDao.selectOne(new LambdaQueryWrapper<JkCommissionAccount>()
                .eq(JkCommissionAccount::getUserId, fundAccount.getUserId()).eq(JkCommissionAccount::getRoleCode, fundAccount.getRoleCode())
                .eq(JkCommissionAccount::getIsDeleted, false).last("limit 1"));
        if (commissionAccount == null) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("account", null);
            result.put("commissionRecords", Collections.emptyList());
            result.put("commissionFlows", Collections.emptyList());
            result.put("reverses", Collections.emptyList());
            result.put("fundAccount", fundAccount);
            result.put("fundFlows", fundFlowDao.selectList(new LambdaQueryWrapper<JkFundFlow>()
                    .eq(JkFundFlow::getAccountId, fundAccount.getId()).orderByDesc(JkFundFlow::getId)));
            return CommonResult.success(result);
        }
        return CommonResult.success(buildAccountDetail(commissionAccount, fundAccount));
    }

    private Map<String, Object> buildAccountDetail(JkCommissionAccount account, JkFundAccount providedFundAccount) {
        List<JkCommissionRecord> records = recordDao.selectList(new LambdaQueryWrapper<JkCommissionRecord>()
                .eq(JkCommissionRecord::getIsDeleted, false)
                .eq(JkCommissionRecord::getReceiverUserId, account.getUserId())
                .eq(JkCommissionRecord::getReceiverRoleCode, account.getRoleCode())
                .orderByDesc(JkCommissionRecord::getId));
        List<JkCommissionFlow> commissionFlows = commissionFlowDao.selectList(new LambdaQueryWrapper<JkCommissionFlow>()
                .eq(JkCommissionFlow::getAccountId, account.getId()).orderByDesc(JkCommissionFlow::getId));
        Set<Long> recordIds = records.stream().map(JkCommissionRecord::getId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        List<JkCommissionReverse> reverses = recordIds.isEmpty() ? Collections.<JkCommissionReverse>emptyList()
                : reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>()
                .in(JkCommissionReverse::getOriginalCommissionRecordId, recordIds).orderByDesc(JkCommissionReverse::getId));
        JkFundAccount fundAccount = providedFundAccount == null ? fundAccountDao.selectOne(new LambdaQueryWrapper<JkFundAccount>()
                .eq(JkFundAccount::getUserId, account.getUserId()).eq(JkFundAccount::getRoleCode, account.getRoleCode())
                .eq(JkFundAccount::getIsDeleted, false).last("limit 1")) : providedFundAccount;
        List<JkFundFlow> fundFlows = fundAccount == null ? Collections.<JkFundFlow>emptyList()
                : fundFlowDao.selectList(new LambdaQueryWrapper<JkFundFlow>()
                .eq(JkFundFlow::getAccountId, fundAccount.getId()).orderByDesc(JkFundFlow::getId));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("account", account);
        result.put("commissionRecords", records);
        result.put("commissionFlows", commissionFlows);
        result.put("reverses", reverses);
        result.put("fundAccount", fundAccount);
        result.put("fundFlows", fundFlows);
        return result;
    }

    @GetMapping("/fund/account/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_FUND_ACCOUNT_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.FUND_ACCOUNT_VIEW, checkDataScope = true)
    public CommonResult<List<JkFundAccount>> fundAccounts(@RequestParam(required = false) String roleCode, @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<JkFundAccount> q = new LambdaQueryWrapper<JkFundAccount>().eq(JkFundAccount::getIsDeleted, false).orderByDesc(JkFundAccount::getId);
        if (roleCode != null && !roleCode.trim().isEmpty()) q.eq(JkFundAccount::getRoleCode, roleCode);
        Long scopedUserId = scopedUserId(userId);
        if (scopedUserId != null) q.eq(JkFundAccount::getUserId, scopedUserId);
        List<JkFundAccount> list = fundAccountDao.selectList(q);
        displayEnrichmentSupport.enrichFundAccounts(list);
        return CommonResult.success(list);
    }

    @GetMapping("/fund/flow/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_FUND_FLOW_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.FUND_FLOW_VIEW, checkDataScope = true)
    public CommonResult<List<JkFundFlow>> fundFlows(@RequestParam(required = false) String flowType, @RequestParam(required = false) String requestNo) {
        LambdaQueryWrapper<JkFundFlow> q = new LambdaQueryWrapper<JkFundFlow>().orderByDesc(JkFundFlow::getId);
        if (flowType != null && !flowType.trim().isEmpty()) {
            List<String> flowTypes = resolveFundFlowTypes(flowType);
            if (flowTypes.size() == 1) {
                q.eq(JkFundFlow::getFlowType, flowTypes.get(0));
            } else {
                q.in(JkFundFlow::getFlowType, flowTypes);
            }
        }
        if (requestNo != null && !requestNo.trim().isEmpty()) q.eq(JkFundFlow::getRequestNo, requestNo);
        JkUserContext scopeContext = userContextService.getAdminContext();
        if (!isPlatformAll(scopeContext)) {
            List<JkFundAccount> ownAccounts = fundAccountDao.selectList(new LambdaQueryWrapper<JkFundAccount>()
                    .eq(JkFundAccount::getUserId, scopeContext.getUserId()).eq(JkFundAccount::getIsDeleted, false));
            List<Long> ownAccountIds = ownAccounts.stream().map(JkFundAccount::getId).collect(java.util.stream.Collectors.toList());
            if (ownAccountIds.isEmpty()) return CommonResult.success(Collections.<JkFundFlow>emptyList());
            q.in(JkFundFlow::getAccountId, ownAccountIds);
        }
        List<JkFundFlow> list = fundFlowDao.selectList(q);
        Set<Long> accountIds = list.stream().map(JkFundFlow::getAccountId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, JkFundAccount> accountMap = accountIds.isEmpty() ? Collections.emptyMap() : fundAccountDao.selectBatchIds(accountIds).stream()
                .collect(java.util.stream.Collectors.toMap(JkFundAccount::getId, value -> value, (a, b) -> a));
        displayEnrichmentSupport.enrichFundFlows(list, accountMap);
        return CommonResult.success(list);
    }

    static List<String> resolveFundFlowTypes(String flowType) {
        if ("REVERSE_DEDUCT".equals(flowType) || "COMMISSION_REVERSE_OUT".equals(flowType)) {
            return Arrays.asList("REVERSE_DEDUCT", "COMMISSION_REVERSE_OUT");
        }
        return Collections.singletonList(flowType);
    }

    @GetMapping("/commission/settle/task/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_SETTLE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_SETTLE_VIEW, checkDataScope = false)
    public CommonResult<List<JkCommissionSettleTask>> settleTasks() {
        List<JkCommissionSettleTask> list = settleTaskDao.selectList(new LambdaQueryWrapper<JkCommissionSettleTask>().orderByDesc(JkCommissionSettleTask::getId));
        displayEnrichmentSupport.enrichCommissionSettleTasks(list);
        return CommonResult.success(list);
    }

    @PostMapping("/commission/settle/manual")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_SETTLE_MANUAL + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_SETTLE_MANAGE, checkDataScope = false)
    public CommonResult<JkCommissionSettleTask> settle(@RequestBody JkCommissionSettleRequest r) {
        return CommonResult.success(settleService.settleRecords(r.getCommissionRecordIds(), operator(), r.getRequestNo(), r.getRemark()));
    }

    @PostMapping("/commission/reverse/manual")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_REVERSE_MANUAL + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_REVERSE_MANAGE, checkDataScope = false)
    public CommonResult<JkCommissionReverse> reverse(@RequestBody JkCommissionReverseRequest r) {
        JkCommissionRecord record = recordDao.selectById(r.getCommissionRecordId());
        if (record == null) throw new IllegalArgumentException("佣金记录不存在");
        JkCommissionReverse reverse = reverseService.reverse(record.getId(), record.getSourceType(), record.getSourceId(), record.getSourceNo(), r.getReverseType(), r.getAmount(), r.getRequestNo(), operator(), r.getReason());
        displayEnrichmentSupport.enrichCommissionReverses(Collections.singletonList(reverse), Collections.singletonMap(record.getId(), record));
        return CommonResult.success(reverse);
    }

    @GetMapping("/commission/reverse/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_REVERSE_LIST + "')")
    @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_REVERSE_VIEW, checkDataScope = false)
    public CommonResult<List<JkCommissionReverse>> reverses() {
        List<JkCommissionReverse> list = reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>().orderByDesc(JkCommissionReverse::getId));
        Set<Long> recordIds = list.stream().map(JkCommissionReverse::getOriginalCommissionRecordId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, JkCommissionRecord> recordMap = recordIds.isEmpty() ? Collections.emptyMap() : recordDao.selectBatchIds(recordIds).stream()
                .collect(java.util.stream.Collectors.toMap(JkCommissionRecord::getId, value -> value, (a, b) -> a));
        displayEnrichmentSupport.enrichCommissionReverses(list, recordMap);
        return CommonResult.success(list);
    }

    private Long scopedUserId(Long requestedUserId) {
        JkUserContext context = userContextService.getAdminContext();
        if (isPlatformAll(context)) return requestedUserId;
        if (requestedUserId != null && !requestedUserId.equals(context.getUserId())) {
            throw new IllegalArgumentException("无权查询其他业务用户数据");
        }
        return context.getUserId();
    }

    private void assertOwnOrPlatform(Long targetUserId) {
        JkUserContext context = userContextService.getAdminContext();
        if (!isPlatformAll(context) && (targetUserId == null || !targetUserId.equals(context.getUserId()))) {
            throw new IllegalArgumentException("无权访问该业务数据");
        }
    }

    private boolean isPlatformAll(JkUserContext context) {
        return context != null && context.getPermissions() != null && context.getPermissions().contains("platform.all");
    }

    private Long operator() {
        Long id = adminActorService.getLinkedFrontUserId(adminActorService.getCurrentAdmin());
        if (id != null) return id;
        if (adminActorService.isPlatformSuperAdmin(adminActorService.getCurrentAdmin())) {
            return -Long.valueOf(adminActorService.getCurrentAdmin().getId());
        }
        throw new IllegalStateException("后台管理员未绑定业务用户");
    }
}
