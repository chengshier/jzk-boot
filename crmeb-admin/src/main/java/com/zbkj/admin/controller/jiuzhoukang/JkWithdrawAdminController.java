package com.zbkj.admin.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.*;
import com.zbkj.common.request.jiuzhoukang.JkWithdrawAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionSettleRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionReverseRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.dao.jiuzhoukang.*;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import com.zbkj.service.service.jiuzhoukang.commission.WithdrawService;
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
    private JkCommissionAccountDao commissionAccountDao;
    @Autowired
    private JkFundAccountDao fundAccountDao;
    @Autowired
    private JkFundFlowDao fundFlowDao;
    @Autowired
    private JkCommissionReverseDao reverseDao;
    @Autowired
    private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @GetMapping("/withdraw/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_LIST + "')")
    public CommonResult<List<JkWithdrawApply>> withdrawList(@RequestParam(required = false) String status, @RequestParam(required = false) String roleCode, @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<JkWithdrawApply> q = new LambdaQueryWrapper<JkWithdrawApply>().eq(JkWithdrawApply::getIsDeleted, false).orderByDesc(JkWithdrawApply::getId);
        if (status != null && !status.trim().isEmpty()) q.eq(JkWithdrawApply::getStatus, status);
        if (roleCode != null && !roleCode.trim().isEmpty()) q.eq(JkWithdrawApply::getRoleCode, roleCode);
        if (userId != null) q.eq(JkWithdrawApply::getUserId, userId);
        List<JkWithdrawApply> list = withdrawDao.selectList(q);
        displayEnrichmentSupport.enrichWithdrawApplies(list);
        return CommonResult.success(list);
    }

    @GetMapping("/withdraw/detail/{id}")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_LIST + "')")
    public CommonResult<JkWithdrawApply> withdrawDetail(@PathVariable Long id) {
        JkWithdrawApply detail = withdrawDao.selectById(id);
        displayEnrichmentSupport.enrichWithdrawApplies(Collections.singletonList(detail));
        return CommonResult.success(detail);
    }

    @PostMapping("/withdraw/audit")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_AUDIT + "')")
    public CommonResult<JkWithdrawApply> audit(@RequestBody JkWithdrawAuditRequest r) {
        return CommonResult.success(withdrawService.audit(r.getId(), operator(), Boolean.TRUE.equals(r.getApproved()), r.getRequestNo(), r.getRemark()));
    }

    @PostMapping("/withdraw/confirm-paid")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_WITHDRAW_CONFIRM_PAID + "')")
    public CommonResult<JkWithdrawApply> paid(@RequestBody JkWithdrawAuditRequest r) {
        return CommonResult.success(withdrawService.confirmPaid(r.getId(), operator(), r.getRequestNo(), r.getRemark()));
    }

    @GetMapping("/commission/record/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_RECORD_LIST + "')")
    public CommonResult<List<JkCommissionRecord>> records(@RequestParam(required = false) String status, @RequestParam(required = false) String sourceType, @RequestParam(required = false) String receiverRoleCode, @RequestParam(required = false) Long receiverUserId) {
        LambdaQueryWrapper<JkCommissionRecord> q = new LambdaQueryWrapper<JkCommissionRecord>().eq(JkCommissionRecord::getIsDeleted, false).orderByDesc(JkCommissionRecord::getId);
        if (status != null && !status.trim().isEmpty()) q.eq(JkCommissionRecord::getStatus, status);
        if (sourceType != null && !sourceType.trim().isEmpty()) q.eq(JkCommissionRecord::getSourceType, sourceType);
        if (receiverRoleCode != null && !receiverRoleCode.trim().isEmpty()) q.eq(JkCommissionRecord::getReceiverRoleCode, receiverRoleCode);
        if (receiverUserId != null) q.eq(JkCommissionRecord::getReceiverUserId, receiverUserId);
        List<JkCommissionRecord> list = recordDao.selectList(q);
        displayEnrichmentSupport.enrichCommissionRecords(list);
        return CommonResult.success(list);
    }

    @GetMapping("/commission/account/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_ACCOUNT_LIST + "')")
    public CommonResult<List<JkCommissionAccount>> commissionAccounts(@RequestParam(required = false) String roleCode, @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<JkCommissionAccount> q = new LambdaQueryWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getIsDeleted, false).orderByDesc(JkCommissionAccount::getId);
        if (roleCode != null && !roleCode.trim().isEmpty()) q.eq(JkCommissionAccount::getRoleCode, roleCode);
        if (userId != null) q.eq(JkCommissionAccount::getUserId, userId);
        List<JkCommissionAccount> list = commissionAccountDao.selectList(q);
        displayEnrichmentSupport.enrichCommissionAccounts(list);
        return CommonResult.success(list);
    }

    @GetMapping("/fund/account/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_FUND_ACCOUNT_LIST + "')")
    public CommonResult<List<JkFundAccount>> fundAccounts(@RequestParam(required = false) String roleCode, @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<JkFundAccount> q = new LambdaQueryWrapper<JkFundAccount>().eq(JkFundAccount::getIsDeleted, false).orderByDesc(JkFundAccount::getId);
        if (roleCode != null && !roleCode.trim().isEmpty()) q.eq(JkFundAccount::getRoleCode, roleCode);
        if (userId != null) q.eq(JkFundAccount::getUserId, userId);
        List<JkFundAccount> list = fundAccountDao.selectList(q);
        displayEnrichmentSupport.enrichFundAccounts(list);
        return CommonResult.success(list);
    }

    @GetMapping("/fund/flow/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_FUND_FLOW_LIST + "')")
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
    public CommonResult<List<JkCommissionSettleTask>> settleTasks() {
        List<JkCommissionSettleTask> list = settleTaskDao.selectList(new LambdaQueryWrapper<JkCommissionSettleTask>().orderByDesc(JkCommissionSettleTask::getId));
        displayEnrichmentSupport.enrichCommissionSettleTasks(list);
        return CommonResult.success(list);
    }

    @PostMapping("/commission/settle/manual")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_SETTLE_MANUAL + "')")
    public CommonResult<JkCommissionSettleTask> settle(@RequestBody JkCommissionSettleRequest r) {
        return CommonResult.success(settleService.settleRecords(r.getCommissionRecordIds(), operator(), r.getRequestNo(), r.getRemark()));
    }

    @PostMapping("/commission/reverse/manual")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_REVERSE_MANUAL + "')")
    public CommonResult<JkCommissionReverse> reverse(@RequestBody JkCommissionReverseRequest r) {
        JkCommissionRecord record = recordDao.selectById(r.getCommissionRecordId());
        if (record == null) throw new IllegalArgumentException("佣金记录不存在");
        JkCommissionReverse reverse = reverseService.reverse(record.getId(), record.getSourceType(), record.getSourceId(), record.getSourceNo(), r.getReverseType(), r.getAmount(), r.getRequestNo(), operator(), r.getReason());
        displayEnrichmentSupport.enrichCommissionReverses(Collections.singletonList(reverse), Collections.singletonMap(record.getId(), record));
        return CommonResult.success(reverse);
    }

    @GetMapping("/commission/reverse/list")
    @PreAuthorize("hasAuthority('" + JkPermissionCodes.ADMIN_COMMISSION_REVERSE_LIST + "')")
    public CommonResult<List<JkCommissionReverse>> reverses() {
        List<JkCommissionReverse> list = reverseDao.selectList(new LambdaQueryWrapper<JkCommissionReverse>().orderByDesc(JkCommissionReverse::getId));
        Set<Long> recordIds = list.stream().map(JkCommissionReverse::getOriginalCommissionRecordId).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, JkCommissionRecord> recordMap = recordIds.isEmpty() ? Collections.emptyMap() : recordDao.selectBatchIds(recordIds).stream()
                .collect(java.util.stream.Collectors.toMap(JkCommissionRecord::getId, value -> value, (a, b) -> a));
        displayEnrichmentSupport.enrichCommissionReverses(list, recordMap);
        return CommonResult.success(list);
    }

    private Long operator() {
        Long id = adminActorService.getLinkedFrontUserId(adminActorService.getCurrentAdmin());
        if (id == null) throw new IllegalStateException("后台管理员未绑定业务用户");
        return id;
    }
}
