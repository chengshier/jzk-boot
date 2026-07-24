package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkAdminUserMapping;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.jiuzhoukang.JkAdminUserMappingSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkAdminUserMappingResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAdminUserMappingDao;
import com.zbkj.service.service.SystemAdminService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.region.JkAdminUserMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JkAdminUserMappingServiceImpl implements JkAdminUserMappingService {
    @Autowired private JkAdminUserMappingDao mappingDao;
    @Autowired private SystemAdminService systemAdminService;
    @Autowired private UserService userService;
    @Autowired private JkPermissionCacheVersionService cacheVersionService;

    @Override public List<JkAdminUserMappingResponse> list(Integer systemAdminId,Long frontUserId){
        LambdaQueryWrapper<JkAdminUserMapping> q=new LambdaQueryWrapper<JkAdminUserMapping>().eq(JkAdminUserMapping::getIsDeleted,false).orderByDesc(JkAdminUserMapping::getId);
        if(systemAdminId!=null)q.eq(JkAdminUserMapping::getSystemAdminId,systemAdminId);if(frontUserId!=null)q.eq(JkAdminUserMapping::getFrontUserId,frontUserId);
        List<JkAdminUserMapping> rows=mappingDao.selectList(q);Set<Integer> admins=rows.stream().map(JkAdminUserMapping::getSystemAdminId).filter(Objects::nonNull).collect(Collectors.toSet());Set<Integer> users=rows.stream().map(JkAdminUserMapping::getFrontUserId).filter(Objects::nonNull).map(Long::intValue).collect(Collectors.toSet());
        Map<Integer,SystemAdmin> adminMap=admins.isEmpty()?Collections.emptyMap():systemAdminService.listByIds(admins).stream().collect(Collectors.toMap(SystemAdmin::getId,a->a,(a,b)->a));
        Map<Integer,User> userMap=users.isEmpty()?Collections.emptyMap():userService.listByIds(users).stream().collect(Collectors.toMap(User::getUid,u->u,(a,b)->a));
        List<JkAdminUserMappingResponse> result=new ArrayList<>();for(JkAdminUserMapping row:rows){JkAdminUserMappingResponse r=new JkAdminUserMappingResponse();r.setId(row.getId());r.setSystemAdminId(row.getSystemAdminId());r.setFrontUserId(row.getFrontUserId());r.setRemark(row.getRemark());r.setStatus(row.getStatus());r.setCreateTime(row.getCreateTime());SystemAdmin a=adminMap.get(row.getSystemAdminId());User u=row.getFrontUserId()==null?null:userMap.get(row.getFrontUserId().intValue());if(a!=null){r.setAdminRealName(a.getRealName());r.setAdminAccount(a.getAccount());}if(u!=null){r.setFrontUserName(StrUtil.blankToDefault(u.getRealName(),u.getNickname()));r.setFrontUserPhone(u.getPhone());}result.add(r);}return result;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public JkAdminUserMappingResponse save(JkAdminUserMappingSaveRequest request,Long operatorId){
        if(systemAdminService.getById(request.getSystemAdminId())==null)throw new IllegalArgumentException("后台管理员不存在");if(userService.getById(request.getFrontUserId().intValue())==null)throw new IllegalArgumentException("前台业务用户不存在");
        JkAdminUserMapping conflict=mappingDao.selectOne(new LambdaQueryWrapper<JkAdminUserMapping>().eq(JkAdminUserMapping::getIsDeleted,false)
                .and(w->w.eq(JkAdminUserMapping::getSystemAdminId,request.getSystemAdminId()).or().eq(JkAdminUserMapping::getFrontUserId,request.getFrontUserId()))
                .ne(request.getId()!=null,JkAdminUserMapping::getId,request.getId()).last("limit 1"));if(conflict!=null)throw new IllegalArgumentException("后台管理员或前台用户已存在有效映射");
        Date now=new Date();JkAdminUserMapping entity=request.getId()==null?new JkAdminUserMapping():mappingDao.selectById(request.getId());if(entity==null)throw new IllegalArgumentException("映射不存在");entity.setSystemAdminId(request.getSystemAdminId()).setFrontUserId(request.getFrontUserId()).setRemark(request.getRemark()).setStatus(request.getStatus()==null||request.getStatus()).setIsDeleted(false).setUpdateUserId(operatorId).setUpdateTime(now);if(entity.getId()==null){entity.setCreateUserId(operatorId).setCreateTime(now).setTenantId("000000");mappingDao.insert(entity);}else mappingDao.updateById(entity);cacheVersionService.clearUserContextCache(request.getFrontUserId());return list(request.getSystemAdminId(),request.getFrontUserId()).get(0);
    }
    @Override public boolean updateStatus(Long id,boolean status,Long operatorId){JkAdminUserMapping entity=mappingDao.selectById(id);if(entity==null)return false;entity.setStatus(status).setUpdateUserId(operatorId).setUpdateTime(new Date());int n=mappingDao.updateById(entity);cacheVersionService.clearUserContextCache(entity.getFrontUserId());return n==1;}
}
