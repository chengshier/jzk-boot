package com.zbkj.front.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount;
import com.zbkj.common.model.jiuzhoukang.JkFundFlow;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRecordDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundFlowDao;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("api/front/jk")
@Api(tags = "九州康收益中心")
public class JkCommissionController {
    @Autowired private FrontTokenComponent frontTokenComponent;
    @Autowired private JkUserContextService contextService;
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkCommissionRecordDao commissionRecordDao;
    @Autowired private JkFundAccountDao fundAccountDao;
    @Autowired private JkFundFlowDao fundFlowDao;
    @Autowired private JkDisplayEnrichmentSupport displayEnrichmentSupport;

    @GetMapping("/commission/summary") @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_VIEW_SELF, checkDataScope = false)
    public CommonResult<Map<String,Object>> summary() {
        Long userId=userId(); String role=role(userId); JkCommissionAccount commission=commissionAccountDao.selectOne(new LambdaQueryWrapper<JkCommissionAccount>().eq(JkCommissionAccount::getUserId,userId).eq(JkCommissionAccount::getRoleCode,role).eq(JkCommissionAccount::getIsDeleted,false));
        JkFundAccount fund=fundAccountDao.selectOne(new LambdaQueryWrapper<JkFundAccount>().eq(JkFundAccount::getUserId,userId).eq(JkFundAccount::getRoleCode,role).eq(JkFundAccount::getIsDeleted,false));
        if (commission != null) {
            displayEnrichmentSupport.enrichCommissionAccounts(Collections.singletonList(commission));
        }
        if (fund != null) {
            displayEnrichmentSupport.enrichFundAccounts(Collections.singletonList(fund));
        }
        Map<String,Object> result=new LinkedHashMap<>(); result.put("commissionAccount",commission); result.put("fundAccount",fund); result.put("availableAmount",fund==null?BigDecimal.ZERO:fund.getAvailableAmount()); return CommonResult.success(result);
    }
    @GetMapping("/commission/list") @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_VIEW_SELF, checkDataScope = false)
    public CommonResult<List<JkCommissionRecord>> commissionList(@RequestParam(required=false) String status) { LambdaQueryWrapper<JkCommissionRecord> q=new LambdaQueryWrapper<JkCommissionRecord>().eq(JkCommissionRecord::getReceiverUserId,userId()).eq(JkCommissionRecord::getIsDeleted,false).orderByDesc(JkCommissionRecord::getId); if(status!=null&&!status.trim().isEmpty())q.eq(JkCommissionRecord::getStatus,status); List<JkCommissionRecord> list = commissionRecordDao.selectList(q); displayEnrichmentSupport.enrichCommissionRecords(list); return CommonResult.success(list); }
    @GetMapping("/commission/detail/{id}") @JkBizPermission(value = JkBizPermissionCodes.COMMISSION_VIEW_SELF, checkDataScope = false)
    public CommonResult<JkCommissionRecord> commissionDetail(@PathVariable Long id) { JkCommissionRecord record=commissionRecordDao.selectById(id); if(record==null||!userId().equals(record.getReceiverUserId()))throw new IllegalArgumentException("佣金记录不存在"); displayEnrichmentSupport.enrichCommissionRecords(Collections.singletonList(record)); return CommonResult.success(record); }
    @GetMapping("/fund/account") @JkBizPermission(value = JkBizPermissionCodes.FUND_ACCOUNT_VIEW, checkDataScope = false)
    public CommonResult<JkFundAccount> fundAccount(){Long userId=userId();JkFundAccount account = fundAccountDao.selectOne(new LambdaQueryWrapper<JkFundAccount>().eq(JkFundAccount::getUserId,userId).eq(JkFundAccount::getRoleCode,role(userId)).eq(JkFundAccount::getIsDeleted,false)); if (account != null) { displayEnrichmentSupport.enrichFundAccounts(Collections.singletonList(account)); } return CommonResult.success(account);}
    @GetMapping("/fund/flow/list") @JkBizPermission(value = JkBizPermissionCodes.FUND_FLOW_VIEW, checkDataScope = false)
    public CommonResult<List<JkFundFlow>> fundFlows(){Long userId=userId();JkFundAccount a=fundAccountDao.selectOne(new LambdaQueryWrapper<JkFundAccount>().eq(JkFundAccount::getUserId,userId).eq(JkFundAccount::getRoleCode,role(userId)).eq(JkFundAccount::getIsDeleted,false));if(a==null)return CommonResult.success(Collections.<JkFundFlow>emptyList());List<JkFundFlow> list = fundFlowDao.selectList(new LambdaQueryWrapper<JkFundFlow>().eq(JkFundFlow::getAccountId,a.getId()).orderByDesc(JkFundFlow::getId)); displayEnrichmentSupport.enrichFundFlows(list, Collections.singletonMap(a.getId(), a)); return CommonResult.success(list);}
    private Long userId(){return Long.valueOf(frontTokenComponent.getUserId());}
    private String role(Long userId){return contextService.getFrontContext(userId).getPrimaryRoleCode();}
}
