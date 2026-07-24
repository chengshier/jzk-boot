package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.model.jiuzhoukang.JkRegionAgent;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.jiuzhoukang.JkRegionAgentBindRequest;
import com.zbkj.common.response.jiuzhoukang.JkRegionAgentResponse;
import com.zbkj.service.dao.jiuzhoukang.JkRegionAgentDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContextService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.region.JkRegionAgentService;
import com.zbkj.service.service.jiuzhoukang.scope.JkUserDataScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JkRegionAgentServiceImpl implements JkRegionAgentService {
    @Autowired private JkRegionAgentDao regionAgentDao;
    @Autowired private JkRegionDao regionDao;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private UserService userService;
    @Autowired private JkUserContextService contextService;
    @Autowired private JkUserDataScopeService dataScopeService;
    @Autowired private JkPermissionCacheVersionService cacheVersionService;

    @Override public List<JkRegionAgentResponse> list(String regionCode, Long countyAgentUserId, Boolean activeOnly) {
        LambdaQueryWrapper<JkRegionAgent> q = new LambdaQueryWrapper<JkRegionAgent>().eq(JkRegionAgent::getIsDeleted, false).orderByDesc(JkRegionAgent::getId);
        if (StrUtil.isNotBlank(regionCode)) q.eq(JkRegionAgent::getRegionCode, regionCode.trim());
        if (countyAgentUserId != null) q.eq(JkRegionAgent::getCountyAgentUserId, countyAgentUserId);
        if (Boolean.TRUE.equals(activeOnly)) q.eq(JkRegionAgent::getStatus, true).eq(JkRegionAgent::getBindStatus, "EFFECTIVE");
        List<JkRegionAgent> rows = regionAgentDao.selectList(q);
        Set<String> regionCodes = rows.stream().map(JkRegionAgent::getRegionCode).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String,String> regionNames = regionCodes.isEmpty()?Collections.emptyMap():regionDao.selectList(new LambdaQueryWrapper<JkRegion>().in(JkRegion::getRegionCode,regionCodes))
                .stream().collect(Collectors.toMap(JkRegion::getRegionCode,JkRegion::getRegionName,(a,b)->a));
        Set<Integer> userIds = rows.stream().map(JkRegionAgent::getCountyAgentUserId).filter(Objects::nonNull).map(Long::intValue).collect(Collectors.toSet());
        Map<Integer,User> users = userIds.isEmpty()?Collections.emptyMap():userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getUid,u->u,(a,b)->a));
        List<JkRegionAgentResponse> result=new ArrayList<>();
        for(JkRegionAgent row:rows){
            JkRegionAgentResponse r=new JkRegionAgentResponse();
            r.setId(row.getId());r.setRegionCode(row.getRegionCode());r.setRegionName(regionNames.get(row.getRegionCode()));r.setCountyAgentUserId(row.getCountyAgentUserId());
            User u=row.getCountyAgentUserId()==null?null:users.get(row.getCountyAgentUserId().intValue());
            if(u!=null){r.setCountyAgentName(StrUtil.blankToDefault(u.getRealName(),u.getNickname()));r.setCountyAgentPhone(u.getPhone());}
            r.setBindStatus(row.getBindStatus());r.setBindStatusText(bindStatusText(row.getBindStatus()));r.setEffectiveTime(row.getEffectiveTime());r.setExpireTime(row.getExpireTime());
            r.setChangeReason(row.getChangeReason());r.setRemark(row.getRemark());r.setStatus(row.getStatus());r.setCreateTime(row.getCreateTime());result.add(r);
        }
        return result;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public JkRegionAgentResponse bind(JkRegionAgentBindRequest request, Long operatorId) {
        JkRegion region=regionDao.selectOne(new LambdaQueryWrapper<JkRegion>().eq(JkRegion::getRegionCode,request.getRegionCode().trim()).eq(JkRegion::getStatus,true).eq(JkRegion::getIsDeleted,false).last("limit 1"));
        if(region==null) throw new IllegalArgumentException("区域不存在或已停用");
        JkUserContext agentContext=contextService.getFrontContext(request.getCountyAgentUserId());
        if(agentContext==null || Boolean.TRUE.equals(agentContext.getFreezeStatus()) || !JkBizConstants.ROLE_COUNTY_AGENT.equals(agentContext.getPrimaryRoleCode())){
            throw new IllegalArgumentException("所选用户不是有效区县代");
        }
        JkRegionAgent current=regionAgentDao.selectOne(new LambdaQueryWrapper<JkRegionAgent>().eq(JkRegionAgent::getRegionCode,request.getRegionCode().trim())
                .eq(JkRegionAgent::getStatus,true).eq(JkRegionAgent::getIsDeleted,false).eq(JkRegionAgent::getBindStatus,"EFFECTIVE").last("limit 1"));
        if(current!=null && request.getCountyAgentUserId().equals(current.getCountyAgentUserId())) return list(request.getRegionCode(),request.getCountyAgentUserId(),true).get(0);
        Date now=new Date();
        if(current!=null){current.setStatus(false).setBindStatus("INVALID").setExpireTime(now).setChangeReason(StrUtil.blankToDefault(request.getChangeReason(),"区域代理换绑"))
                .setUpdateUserId(operatorId).setUpdateTime(now);regionAgentDao.updateById(current);}
        JkRegionAgent entity=new JkRegionAgent().setRegionCode(request.getRegionCode().trim()).setCountyAgentUserId(request.getCountyAgentUserId())
                .setBindStatus("EFFECTIVE").setEffectiveTime(now).setChangeReason(request.getChangeReason()).setRemark(request.getRemark())
                .setStatus(true).setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId).setCreateTime(now).setUpdateTime(now).setTenantId("000000");
        regionAgentDao.insert(entity);
        region.setOccupied(true).setUpdateUserId(operatorId).setUpdateTime(now);regionDao.updateById(region);
        JkUserBusinessRole role=userRoleDao.selectOne(new LambdaQueryWrapper<JkUserBusinessRole>().eq(JkUserBusinessRole::getUserId,request.getCountyAgentUserId())
                .eq(JkUserBusinessRole::getRoleCode,JkBizConstants.ROLE_COUNTY_AGENT).eq(JkUserBusinessRole::getIsDeleted,false).last("limit 1"));
        if(role!=null){role.setRegionCode(request.getRegionCode().trim()).setBelongCountyAgentId(request.getCountyAgentUserId()).setUpdateUserId(operatorId).setUpdateTime(now);userRoleDao.updateById(role);}
        dataScopeService.rebuildUserScopes(request.getCountyAgentUserId(),request.getRegionCode().trim(),request.getCountyAgentUserId(),agentContext.getPermissions(),operatorId);
        cacheVersionService.refreshUserCacheVersion(request.getCountyAgentUserId(),"REGION_AGENT","区域代理配置变更",operatorId);
        return list(request.getRegionCode(),request.getCountyAgentUserId(),true).get(0);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public boolean invalidate(Long id,String reason,Long operatorId){
        JkRegionAgent entity=regionAgentDao.selectById(id);if(entity==null||Boolean.TRUE.equals(entity.getIsDeleted()))return false;
        Date now=new Date();entity.setStatus(false).setBindStatus("INVALID").setExpireTime(now).setChangeReason(reason).setUpdateUserId(operatorId).setUpdateTime(now);
        int updated=regionAgentDao.updateById(entity);
        if(entity.getCountyAgentUserId()!=null)cacheVersionService.refreshUserCacheVersion(entity.getCountyAgentUserId(),"REGION_AGENT","区域代理失效",operatorId);
        return updated==1;
    }

    private String bindStatusText(String s){if("EFFECTIVE".equals(s))return"生效中";if("INVALID".equals(s))return"已失效";if("EXPIRED".equals(s))return"已到期";return"未绑定";}
}
