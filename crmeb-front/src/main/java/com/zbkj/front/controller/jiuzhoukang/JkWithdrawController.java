package com.zbkj.front.controller.jiuzhoukang;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkWithdrawApply;
import com.zbkj.common.request.jiuzhoukang.JkWithdrawApplyRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.dao.jiuzhoukang.JkWithdrawApplyDao;
import com.zbkj.service.service.SystemConfigService;
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
    @Autowired private FrontTokenComponent frontTokenComponent;
    @Autowired private JkUserContextService userContextService;
    @Autowired private JkWithdrawApplyDao withdrawDao;
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @PostMapping("/apply")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<JkWithdrawApply> apply(@RequestBody JkWithdrawApplyRequest request) {
        validatePayee(request);
        Long userId = userId();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("accountType", "BANK");
        snapshot.put("accountName", request.getAccountName().trim());
        snapshot.put("bankName", request.getBankName().trim());
        snapshot.put("bankAccount", normalizeBankAccount(request.getBankAccount()));
        snapshot.put("bankAccountMask", maskBankAccount(request.getBankAccount()));
        JkWithdrawApply apply = withdrawService.apply(userId, role(userId), request.getAmount(),
                request.getRequestNo(), JSONUtil.toJsonStr(snapshot));
        displayEnrichmentSupport.enrichWithdrawApplies(Collections.singletonList(apply));
        return CommonResult.success(apply);
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
        // 保留 App 历史字段别名，避免旧版本最低金额回退成 1 元。
        config.put("minAmount", minimumAmount);
        config.put("minWithdrawAmount", minimumAmount);
        config.put("stepAmount", stepAmount);
        config.put("arrivalTimeText", arrivalTimeText);
        config.put("minimumAmountSource", minimumAmount == null ? "待业务配置" : CONFIG_KEY_MINIMUM);
        config.put("accountRequired", true);
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
        displayEnrichmentSupport.enrichWithdrawApplies(list);
        return CommonResult.success(list);
    }

    @GetMapping("/detail/{id}")
    @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_VIEW_SELF, checkDataScope = false)
    public CommonResult<JkWithdrawApply> detail(@PathVariable Long id) {
        JkWithdrawApply apply = withdrawDao.selectById(id);
        if (apply == null || !userId().equals(apply.getUserId())) throw new IllegalArgumentException("提现申请不存在");
        displayEnrichmentSupport.enrichWithdrawApplies(Collections.singletonList(apply));
        return CommonResult.success(apply);
    }

    private void validatePayee(JkWithdrawApplyRequest request) {
        if (request == null) throw new IllegalArgumentException("提现申请不能为空");
        String accountType = StrUtil.blankToDefault(request.getAccountType(), "BANK").trim().toUpperCase();
        if (!"BANK".equals(accountType)) throw new IllegalArgumentException("当前仅支持银行卡提现");
        if (StrUtil.isBlank(request.getAccountName())) throw new IllegalArgumentException("请填写收款人姓名");
        if (StrUtil.isBlank(request.getBankName())) throw new IllegalArgumentException("请填写开户银行");
        String bankAccount = normalizeBankAccount(request.getBankAccount());
        if (!bankAccount.matches("\\d{8,30}")) throw new IllegalArgumentException("请填写有效银行卡号");
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

    private String normalizeBankAccount(String value) {
        return value == null ? "" : value.replaceAll("[\\s-]", "");
    }

    private String maskBankAccount(String value) {
        String account = normalizeBankAccount(value);
        if (account.length() <= 8) return account;
        return account.substring(0, 4) + " **** **** " + account.substring(account.length() - 4);
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
