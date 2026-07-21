package com.zbkj.front.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkWithdrawApply;
import com.zbkj.common.request.jiuzhoukang.JkWithdrawApplyRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.dao.jiuzhoukang.JkWithdrawApplyDao;
import com.zbkj.service.service.jiuzhoukang.commission.WithdrawService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("api/front/jk/withdraw")
@Api(tags = "九州康提现")
public class JkWithdrawController {
    private static final String CONFIG_KEY_JK_WITHDRAW_MINIMUM_AMOUNT = "jk_withdraw_minimum_amount";
    @Autowired private WithdrawService withdrawService;
    @Autowired private FrontTokenComponent frontTokenComponent;
    @Autowired private JkUserContextService userContextService;
    @Autowired private JkWithdrawApplyDao withdrawDao;
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @PostMapping("/apply") @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<JkWithdrawApply> apply(@RequestBody JkWithdrawApplyRequest request) { Long userId=userId(); JkWithdrawApply apply = withdrawService.apply(userId, role(userId), request.getAmount(), request.getRequestNo(), request.getPayeeSnapshotJson()); displayEnrichmentSupport.enrichWithdrawApplies(Collections.singletonList(apply)); return CommonResult.success(apply); }
    @GetMapping("/config") @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_APPLY, checkDataScope = false)
    public CommonResult<Map<String,Object>> config(){Map<String,Object> config=new LinkedHashMap<>();String value=systemConfigService.getValueByKey(CONFIG_KEY_JK_WITHDRAW_MINIMUM_AMOUNT);BigDecimal minimumAmount=value==null||value.trim().isEmpty()?null:new BigDecimal(value.trim());config.put("minimumAmount",minimumAmount);config.put("minimumAmountSource",minimumAmount==null?"待业务配置":"jk_withdraw_minimum_amount");config.put("paymentMode","OFFLINE_CONFIRM");config.put("autoPaymentEnabled",false);return CommonResult.success(config);}
    @GetMapping("/list") @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_VIEW_SELF, checkDataScope = false)
    public CommonResult<List<JkWithdrawApply>> list(){List<JkWithdrawApply> list = withdrawDao.selectList(new LambdaQueryWrapper<JkWithdrawApply>().eq(JkWithdrawApply::getUserId,userId()).eq(JkWithdrawApply::getIsDeleted,false).orderByDesc(JkWithdrawApply::getId)); displayEnrichmentSupport.enrichWithdrawApplies(list); return CommonResult.success(list);}
    @GetMapping("/detail/{id}") @JkBizPermission(value = JkBizPermissionCodes.WITHDRAW_VIEW_SELF, checkDataScope = false)
    public CommonResult<JkWithdrawApply> detail(@PathVariable Long id){JkWithdrawApply apply=withdrawDao.selectById(id);if(apply==null||!userId().equals(apply.getUserId()))throw new IllegalArgumentException("提现申请不存在");displayEnrichmentSupport.enrichWithdrawApplies(Collections.singletonList(apply));return CommonResult.success(apply);}
    private Long userId(){return Long.valueOf(frontTokenComponent.getUserId());}
    private String role(Long userId){return userContextService.getFrontContext(userId).getPrimaryRoleCode();}
}
