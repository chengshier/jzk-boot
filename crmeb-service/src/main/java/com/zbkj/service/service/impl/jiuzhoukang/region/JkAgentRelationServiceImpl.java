package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationBindRequest;
import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JkAgentRelationServiceImpl implements JkAgentRelationService {
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private UserService userService;
    @Autowired private JkPermissionCacheVersionService cacheVersionService;

    @Override public List<JkAgentRelationResponse> list(Long userId,Long parentUserId,Boolean activeOnly){
        LambdaQueryWrapper<JkAgentRelation> q=new LambdaQueryWrapper<JkAgentRelation>().eq(JkAgentRelation::getIsDeleted,false).orderByDesc(JkAgentRelation::getId);
        if(userId!=null)q.eq(JkAgentRelation::getUserId,userId);if(parentUserId!=null)q.eq(JkAgentRelation::getParentUserId,parentUserId);if(Boolean.TRUE.equals(activeOnly))q.eq(JkAgentRelation::getStatus,true);
        List<JkAgentRelation> rows=relationDao.selectList(q);Set<Integer> ids=new HashSet<>();for(JkAgentRelation r:rows){if(r.getUserId()!=null)ids.add(r.getUserId().intValue());if(r.getParentUserId()!=null)ids.add(r.getParentUserId().intValue());}
        Map<Integer,User> users=ids.isEmpty()?Collections.emptyMap():userService.listByIds(ids).stream().collect(Collectors.toMap(User::getUid,u->u,(a,b)->a));
        List<JkAgentRelationResponse> result=new ArrayList<>();for(JkAgentRelation row:rows){JkAgentRelationResponse r=new JkAgentRelationResponse();
            r.setId(row.getId());r.setUserId(row.getUserId());r.setParentUserId(row.getParentUserId());r.setRootUserId(row.getRootUserId());r.setRelationType(row.getRelationType());r.setBindSource(row.getBindSource());r.setSourceCode(row.getSourceCode());
            r.setEffectiveTime(row.getEffectiveTime());r.setExpireTime(row.getExpireTime());r.setChangeReason(row.getChangeReason());r.setRemark(row.getRemark());r.setStatus(row.getStatus());r.setCreateTime(row.getCreateTime());
            User u=row.getUserId()==null?null:users.get(row.getUserId().intValue());User p=row.getParentUserId()==null?null:users.get(row.getParentUserId().intValue());
            if(u!=null){r.setUserName(StrUtil.blankToDefault(u.getRealName(),u.getNickname()));r.setUserPhone(u.getPhone());}if(p!=null){r.setParentName(StrUtil.blankToDefault(p.getRealName(),p.getNickname()));r.setParentPhone(p.getPhone());}result.add(r);}return result;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public JkAgentRelationResponse bind(JkAgentRelationBindRequest request,Long operatorId){
        if(request.getParentUserId()!=null && request.getUserId().equals(request.getParentUserId()))throw new IllegalArgumentException("不能绑定自己为上级");
        User user=userService.getById(request.getUserId().intValue());if(user==null)throw new IllegalArgumentException("下级用户不存在");
        User parent=request.getParentUserId()==null?null:userService.getById(request.getParentUserId().intValue());if(request.getParentUserId()!=null&&parent==null)throw new IllegalArgumentException("上级用户不存在");
        assertNoCycle(request.getUserId(),request.getParentUserId());
        JkAgentRelation current=relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>().eq(JkAgentRelation::getUserId,request.getUserId()).eq(JkAgentRelation::getStatus,true).eq(JkAgentRelation::getIsDeleted,false).last("limit 1"));
        if(current!=null&&Objects.equals(current.getParentUserId(),request.getParentUserId()))return list(request.getUserId(),null,true).get(0);
        Date now=new Date();if(current!=null){current.setStatus(false).setExpireTime(now).setChangeReason(StrUtil.blankToDefault(request.getChangeReason(),"关系换绑")).setUpdateUserId(operatorId).setUpdateTime(now);relationDao.updateById(current);}
        Long root=resolveRoot(request.getParentUserId());
        JkAgentRelation entity=new JkAgentRelation().setUserId(request.getUserId()).setParentUserId(request.getParentUserId()).setRootUserId(root)
                .setRelationType(StrUtil.blankToDefault(request.getRelationType(),"DIRECT")).setBindSource(StrUtil.blankToDefault(request.getBindSource(),"ADMIN"))
                .setSourceCode(request.getSourceCode()).setEffectiveTime(now).setReplacedRelationId(current==null?null:current.getId()).setChangeReason(request.getChangeReason()).setRemark(request.getRemark())
                .setStatus(true).setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId).setCreateTime(now).setUpdateTime(now).setTenantId("000000");relationDao.insert(entity);
        user.setSpreadUid(request.getParentUserId()==null?0:Math.toIntExact(request.getParentUserId()));userService.updateById(user);
        cacheVersionService.refreshUserCacheVersion(request.getUserId(),"AGENT_RELATION","上下级关系变更",operatorId);
        return list(request.getUserId(),null,true).get(0);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public boolean invalidate(Long id,String reason,Long operatorId){JkAgentRelation entity=relationDao.selectById(id);if(entity==null||Boolean.TRUE.equals(entity.getIsDeleted()))return false;Date now=new Date();entity.setStatus(false).setExpireTime(now).setChangeReason(reason).setUpdateUserId(operatorId).setUpdateTime(now);int n=relationDao.updateById(entity);User user=userService.getById(entity.getUserId().intValue());if(user!=null){user.setSpreadUid(0);userService.updateById(user);}cacheVersionService.refreshUserCacheVersion(entity.getUserId(),"AGENT_RELATION","上下级关系失效",operatorId);return n==1;}

    private void assertNoCycle(Long userId,Long parentUserId){Long cursor=parentUserId;Set<Long> seen=new HashSet<>();for(int i=0;cursor!=null&&i<100;i++){if(userId.equals(cursor))throw new IllegalArgumentException("绑定会形成循环关系");if(!seen.add(cursor))throw new IllegalArgumentException("现有关系链存在循环，请先修复");JkAgentRelation r=relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>().eq(JkAgentRelation::getUserId,cursor).eq(JkAgentRelation::getStatus,true).eq(JkAgentRelation::getIsDeleted,false).last("limit 1"));cursor=r==null?null:r.getParentUserId();}if(cursor!=null)throw new IllegalArgumentException("关系层级超过系统安全上限");}
    private Long resolveRoot(Long parentUserId){if(parentUserId==null)return null;Long cursor=parentUserId,root=parentUserId;for(int i=0;i<100;i++){JkAgentRelation r=relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>().eq(JkAgentRelation::getUserId,cursor).eq(JkAgentRelation::getStatus,true).eq(JkAgentRelation::getIsDeleted,false).last("limit 1"));if(r==null||r.getParentUserId()==null)return r!=null&&r.getRootUserId()!=null?r.getRootUserId():root;root=r.getParentUserId();cursor=r.getParentUserId();}return root;}
}
