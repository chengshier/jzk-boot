package com.zbkj.front.controller.jiuzhoukang;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.constants.jiuzhoukang.JkBizPermissionCodes;
import com.zbkj.common.annotation.jiuzhoukang.JkBizPermission;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockFlow;
import com.zbkj.common.model.jiuzhoukang.JkStockItem;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockFlowResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.dao.jiuzhoukang.JkStockFlowDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockItemDao;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.stock.StockAccountService;
import com.zbkj.service.service.jiuzhoukang.support.JkDisplayEnrichmentSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/front/jk/stock")
public class JkStockController {
 @Autowired private FrontTokenComponent token;
 @Autowired private JkUserContextService contextService;
 @Autowired private StockAccountService accountService;
 @Autowired private JkStockItemDao itemDao;
 @Autowired private JkStockFlowDao flowDao;
 @Autowired private JkDisplayEnrichmentSupport displayEnrichmentSupport;
 @GetMapping("/my") @JkBizPermission(value = JkBizPermissionCodes.STOCK_VIEW_SELF) public CommonResult<Map<String,Object>> my() {
  JkUserContext c=context(); List<JkStockAccount> accounts=accounts(c.getUserId()); List<Long> ids=ids(accounts);
  List<JkStockItem> items=ids.isEmpty()?Collections.emptyList():itemDao.selectList(new LambdaQueryWrapper<JkStockItem>().in(JkStockItem::getStockAccountId,ids).eq(JkStockItem::getIsDeleted,false));
  List<JkStockItemResponse> rows = items.stream().map(this::toItemResponse).collect(Collectors.toList());
  displayEnrichmentSupport.enrichStockItems(rows);
  Map<String,Object> data=new HashMap<>();data.put("identity",c.getPrimaryRoleName());data.put("freezeReason",c.getFreezeReason());data.put("accounts",accounts);data.put("items",rows);return CommonResult.success(data);
 }
 @GetMapping("/flow/list") @JkBizPermission(value = JkBizPermissionCodes.STOCK_FLOW_VIEW) public CommonResult<CommonPage<JkStockFlowResponse>> flow(PageParamRequest page){JkUserContext c=context();List<Long> ids=ids(accounts(c.getUserId()));List<JkStockFlow> list=ids.isEmpty()?Collections.emptyList():flowDao.selectList(new LambdaQueryWrapper<JkStockFlow>().in(JkStockFlow::getStockAccountId,ids).eq(JkStockFlow::getIsDeleted,false).orderByDesc(JkStockFlow::getId));List<JkStockFlowResponse> rows=list.stream().map(this::toFlowResponse).collect(Collectors.toList());displayEnrichmentSupport.enrichStockFlows(rows);return CommonResult.success(CommonPage.restPage(new com.github.pagehelper.PageInfo<>(rows)));}
 private JkUserContext context(){Integer uid=token.getUserId();if(uid==null)throw new CrmebException("请先登录");JkUserContext c=contextService.getFrontContext(Long.valueOf(uid));if(Boolean.TRUE.equals(c.getFreezeStatus()))throw new CrmebException(c.getFreezeReason());if(!(JkBizConstants.ROLE_COUNTY_AGENT.equals(c.getPrimaryRoleCode())||JkBizConstants.ROLE_MAKER.equals(c.getPrimaryRoleCode())||JkBizConstants.ROLE_PARTNER.equals(c.getPrimaryRoleCode())))throw new CrmebException("当前身份不支持库存中心");return c;}
 private List<JkStockAccount> accounts(Long uid){return accountService.list(new LambdaQueryWrapper<JkStockAccount>().eq(JkStockAccount::getOwnerUserId,uid).eq(JkStockAccount::getIsDeleted,false).eq(JkStockAccount::getStatus,true));}
 private List<Long> ids(List<JkStockAccount> a){List<Long> r=new ArrayList<>();for(JkStockAccount x:a)r.add(x.getId());return r;}
 private JkStockItemResponse toItemResponse(JkStockItem item){JkStockItemResponse response=new JkStockItemResponse();BeanUtils.copyProperties(item,response);return response;}
 private JkStockFlowResponse toFlowResponse(JkStockFlow item){JkStockFlowResponse response=new JkStockFlowResponse();BeanUtils.copyProperties(item,response);return response;}
}
