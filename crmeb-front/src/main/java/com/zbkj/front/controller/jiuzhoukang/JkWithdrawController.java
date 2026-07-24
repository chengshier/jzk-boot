package com.zbkj.front.controller.jiuzhoukang;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkWithdrawApply;
import com.zbkj.common.request.jiuzhoukang.JkWithdrawApplyRequest;
import com.zbkj.common.request.jiuzhoukang.JkWithdrawPayeeAccountSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkWithdrawPayeeAccountResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.dao.jiuzhoukang.JkWithdrawApplyDao;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.jiuzhoukang.commission.JkWithdrawPayeeAccountService;
import com.zbkj.service.service.jiuzhoukang.commission.WithdrawService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/front/jk/withdraw")
@Api(tags = "九州康提现")
public class JkWithdrawController {
    private static final String CONFIG_KEY_MINIMUM = "jk_withdraw_minimum_amount";
    private static final String CONFIG_KEY_STEP = "jk_withdraw_step_amount";
    private static final String CONFIG_KEY_ARRIVAL = "jk_withdraw_arrival_time_text";

    @Autowired private WithdrawService withdrawService;
    @Autowired private JkWithdrawPayeeAccountService payeeAccountService;
    @Autowired private FrontTokenComponent frontTokenComponent;
    @Autowired private JkUserContextService userContextService;
    @Autowired private JkWithdrawApplyDao withdrawDao;
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @PostMapping("/apply")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<JkWithdrawApply> apply(@RequestBody JkWithdrawApplyRequest request) {
        if (request == null) throw new IllegalArgumentException("提现申请不能为空");
        Long userId = userId();
        String snapshot = resolveSnapshot(userId, request);
        JkWithdrawApply apply = withdrawService.apply(userId, role(userId), request.getAmount(),
                request.getRequestNo(), snapshot);
        enrich(Collections.singletonList(apply));
        return CommonResult.success(apply);
    }

    @GetMapping("/payee-account/list")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<List<JkWithdrawPayeeAccountResponse>> payeeAccountList() {
        return CommonResult.success(payeeAccountService.list(userId()));
    }

    @PostMapping("/payee-account/save")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<JkWithdrawPayeeAccountResponse> savePayeeAccount(@RequestBody JkWithdrawPayeeAccountSaveRequest request) {
        return CommonResult.success(payeeAccountService.save(userId(), request));
    }

    @PostMapping("/payee-account/{id}/default")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<JkWithdrawPayeeAccountResponse> defaultPayeeAccount(@PathVariable Long id) {
        return CommonResult.success(payeeAccountService.setDefault(userId(), id));
    }

    @PostMapping("/payee-account/{id}/delete")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<Boolean> deletePayeeAccount(@PathVariable Long id) {
        payeeAccountService.remove(userId(), id);
        return CommonResult.success(true);
    }

    @GetMapping("/config")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<Map<String, Object>> config() {
        BigDecimal minimumAmount = decimalConfig(CONFIG_KEY_MINIMUM);
        BigDecimal stepAmount = decimalConfig(CONFIG_KEY_STEP);
        if (stepAmount == null || stepAmount.signum() <= 0) stepAmount = BigDecimal.ONE;
        String arrivalTimeText = systemConfigService.getValueByKey(CONFIG_KEY_ARRIVAL);
        if (StrUtil.isBlank(arrivalTimeText)) arrivalTimeText = "1-2个工作日";

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("minimumAmount", minimumAmount);
        config.put("minAmount", minimumAmount);
        config.put("minWithdrawAmount", minimumAmount);
        config.put("stepAmount", stepAmount);
        config.put("arrivalTimeText", arrivalTimeText);
        config.put("minimumAmountSource", minimumAmount == null ? "待业务配置" : CONFIG_KEY_MINIMUM);
        config.put("accountRequired", true);
        config.put("accountManagedByServer", true);
        config.put("supportedAccountTypes", Collections.singletonList("BANK"));
        config.put("paymentMode", "OFFLINE_CONFIRM");
        config.put("autoPaymentEnabled", false);
        return CommonResult.success(config);
    }

    @GetMapping("/list")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_VIEW_SELF, checkDataScope = false)
    public CommonResult<List<JkWithdrawApply>> list() {
        List<JkWithdrawApply> list = withdrawDao.selectList(new LambdaQueryWrapper<JkWithdrawApply>()
                .eq(JkWithdrawApply::getUserId, userId())
                .eq(JkWithdrawApply::getIsDeleted, false)
                .orderByDesc(JkWithdrawApply::getId));
        enrich(list);
        return CommonResult.success(list);
    }

    @GetMapping("/detail/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_VIEW_SELF, checkDataScope = false)
    public CommonResult<JkWithdrawApply> detail(@PathVariable Long id) {
        JkWithdrawApply apply = withdrawDao.selectById(id);
        if (apply == null || !userId().equals(apply.getUserId())) throw new IllegalArgumentException("提现申请不存在");
        enrich(Collections.singletonList(apply));
        return CommonResult.success(apply);
    }

    private String resolveSnapshot(Long userId, JkWithdrawApplyRequest request) {
        if (request.getPayeeAccountId() != null) {
            return payeeAccountService.buildSnapshotJson(userId, request.getPayeeAccountId());
        }
        // 兼容旧 App：旧版结构化银行卡字段首次提交时自动保存为本人默认账户，再由服务端生成快照。
        if (StrUtil.isNotBlank(request.getAccountName()) || StrUtil.isNotBlank(request.getBankName())
                || StrUtil.isNotBlank(request.getBankAccount())) {
            JkWithdrawPayeeAccountSaveRequest save = new JkWithdrawPayeeAccountSaveRequest();
            save.setAccountType(request.getAccountType());
            save.setAccountName(request.getAccountName());
            save.setBankName(request.getBankName());
            save.setBankAccount(request.getBankAccount());
            save.setSetDefault(true);
            JkWithdrawPayeeAccountResponse account = payeeAccountService.save(userId, save);
            return payeeAccountService.buildSnapshotJson(userId, account.getId());
        }
        throw new IllegalArgumentException("请选择提现收款账户");
    }

    private void enrich(List<JkWithdrawApply> rows) {
        displayEnrichmentSupport.enrichWithdrawApplies(rows);
        for (JkWithdrawApply row : rows) {
            Map<String, Object> masked = payeeAccountService.maskedSnapshot(row.getPayeeSnapshotJson());
            row.setPayeeAccountId((Long) masked.get("payeeAccountId"));
            row.setPayeeAccountType((String) masked.get("accountType"));
            row.setPayeeAccountName((String) masked.get("accountName"));
            row.setPayeeBankName((String) masked.get("bankName"));
            row.setPayeeBankAccountMask((String) masked.get("bankAccountMask"));
        }
    }

    private BigDecimal decimalConfig(String key) {
        String value = systemConfigService.getValueByKey(key);
        if (StrUtil.isBlank(value)) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("九州康提现配置非法：" + key);
        }
    }

    private Long userId() {
        Integer value = frontTokenComponent.getUserId();
        if (value == null || value <= 0) throw new IllegalArgumentException("请先登录");
        return Long.valueOf(value);
    }

    private String role(Long userId) {
        return userContextService.getFrontContext(userId).getPrimaryRoleCode();
    }
}
