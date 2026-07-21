package com.zbkj.service.service.impl.jiuzhoukang.commission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRuleItem;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkCommissionRuleItemSaveRequest;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleItemDao;
import com.zbkj.service.service.jiuzhoukang.commission.CommissionRuleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommissionRuleServiceImpl extends ServiceImpl<JkCommissionRuleDao, JkCommissionRule> implements CommissionRuleService {
    @org.springframework.beans.factory.annotation.Autowired private JkCommissionRuleItemDao itemDao;
    @org.springframework.beans.factory.annotation.Autowired private JkBusinessRoleService businessRoleService;
    @Override
    public JkCommissionRule saveRule(JkCommissionRuleSaveRequest request) {
        JkCommissionRule rule = request.getId() == null ? new JkCommissionRule() : getById(request.getId());
        if (rule == null) rule = new JkCommissionRule();
        BeanUtils.copyProperties(request, rule);
        Date now = new Date();
        if (rule.getId() == null) {
            rule.setRuleNo("CR" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            rule.setRuleVersion(rule.getRuleVersion() == null ? 1 : rule.getRuleVersion());
            rule.setStatus(true).setIsDeleted(false).setVersion(0).setCreateTime(now).setUpdateTime(now);
            save(rule);
        } else { rule.setUpdateTime(now); updateById(rule); }
        JkCommissionRule saved = getById(rule.getId());
        enrichRuleDisplays(Collections.singletonList(saved));
        return saved;
    }
    @Override public JkCommissionRuleItem saveItem(JkCommissionRuleItemSaveRequest request) {
        if (request.getRuleId() == null || getById(request.getRuleId()) == null) throw new IllegalArgumentException("佣金规则不存在");
        if (!"PERCENT".equals(request.getCalculationType()) && !"FIXED".equals(request.getCalculationType())) throw new IllegalArgumentException("佣金计算类型非法");
        if ("PERCENT".equals(request.getCalculationType()) && (request.getCommissionRate() == null || request.getCommissionRate().signum() < 0)) throw new IllegalArgumentException("佣金比例非法");
        if ("FIXED".equals(request.getCalculationType()) && (request.getFixedAmount() == null || request.getFixedAmount().signum() < 0)) throw new IllegalArgumentException("固定佣金非法");
        JkCommissionRuleItem item=request.getId()==null?new JkCommissionRuleItem():itemDao.selectById(request.getId()); if(item==null)item=new JkCommissionRuleItem();
        org.springframework.beans.BeanUtils.copyProperties(request,item); Date now=new Date(); if(item.getId()==null){item.setItemNo("CI"+UUID.randomUUID().toString().replace("-","").substring(0,16)).setPriority(item.getPriority()==null?0:item.getPriority()).setStatus(item.getStatus()==null?true:item.getStatus()).setIsDeleted(false).setCreateTime(now).setUpdateTime(now);itemDao.insert(item);}else{item.setUpdateTime(now);itemDao.updateById(item);}JkCommissionRuleItem saved = itemDao.selectById(item.getId()); enrichRuleItemDisplays(Collections.singletonList(saved)); return saved;
    }
    @Override public List<JkCommissionRuleItem> listItems(Long ruleId){List<JkCommissionRuleItem> items = itemDao.selectList(new LambdaQueryWrapper<JkCommissionRuleItem>().eq(JkCommissionRuleItem::getRuleId,ruleId).eq(JkCommissionRuleItem::getIsDeleted,false).orderByDesc(JkCommissionRuleItem::getPriority)); enrichRuleItemDisplays(items); return items;}
    @Override public boolean updateItemStatus(Long id,boolean status){JkCommissionRuleItem item=itemDao.selectById(id);if(item==null)return false;item.setStatus(status).setUpdateTime(new Date());return itemDao.updateById(item)>0;}    @Override public boolean updateStatus(Long id, boolean status) { JkCommissionRule rule=getById(id); if(rule==null)return false; rule.setStatus(status).setUpdateTime(new Date()); return updateById(rule); }
    @Override public List<JkCommissionRule> listActiveRules(String sourceType,String receiverRoleCode) { return listRules(sourceType, receiverRoleCode, true); }
    @Override public List<JkCommissionRule> listRules(String sourceType,String receiverRoleCode,Boolean status) { LambdaQueryWrapper<JkCommissionRule> q=new LambdaQueryWrapper<>(); q.eq(JkCommissionRule::getIsDeleted,false); if(sourceType!=null&&!sourceType.trim().isEmpty())q.eq(JkCommissionRule::getSourceType,sourceType); if(receiverRoleCode!=null&&!receiverRoleCode.trim().isEmpty())q.eq(JkCommissionRule::getReceiverRoleCode,receiverRoleCode); if(status!=null)q.eq(JkCommissionRule::getStatus,status); q.orderByDesc(JkCommissionRule::getRuleVersion).orderByDesc(JkCommissionRule::getId); List<JkCommissionRule> rules = list(q); enrichRuleDisplays(rules); return rules; }

    void enrichRuleDisplays(List<JkCommissionRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        Map<String, String> roleNameMap = roleNameMap();
        for (JkCommissionRule rule : rules) {
            rule.setSourceTypeText(labelSourceType(rule.getSourceType()));
            rule.setReceiverRoleName(resolveRoleName(roleNameMap, rule.getReceiverRoleCode()));
            rule.setStatusText(Boolean.TRUE.equals(rule.getStatus()) ? "启用" : "禁用");
            rule.setStatusTag(Boolean.TRUE.equals(rule.getStatus()) ? "success" : "info");
        }
    }

    void enrichRuleItemDisplays(List<JkCommissionRuleItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<String, String> roleNameMap = roleNameMap();
        for (JkCommissionRuleItem item : items) {
            item.setReceiverRoleName(resolveRoleName(roleNameMap, item.getReceiverRoleCode()));
            item.setCalculationTypeText(labelCalculationType(item.getCalculationType()));
            item.setStatusText(Boolean.TRUE.equals(item.getStatus()) ? "启用" : "禁用");
            item.setStatusTag(Boolean.TRUE.equals(item.getStatus()) ? "success" : "info");
        }
    }

    private Map<String, String> roleNameMap() {
        List<JkBusinessRole> roles = businessRoleService.getEnabledRoleList();
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyMap();
        }
        return roles.stream().collect(Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
    }

    private String resolveRoleName(Map<String, String> roleNameMap, String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            return "--";
        }
        return roleNameMap.getOrDefault(roleCode, roleCode);
    }

    private String labelSourceType(String sourceType) {
        if (StrUtil.isBlank(sourceType)) {
            return "--";
        }
        if ("RETAIL_ORDER".equals(sourceType)) {
            return "零售订单";
        }
        if ("PLATFORM_ORDER".equals(sourceType)) {
            return "平台订货";
        }
        if ("STOCK_TRANSFER".equals(sourceType)) {
            return "库存调拨";
        }
        return sourceType;
    }

    private String labelCalculationType(String calculationType) {
        if (StrUtil.isBlank(calculationType)) {
            return "--";
        }
        if ("PERCENT".equals(calculationType)) {
            return "比例";
        }
        if ("FIXED".equals(calculationType)) {
            return "固定金额";
        }
        return calculationType;
    }
}
