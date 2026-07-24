package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkAgentRelation;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.jiuzhoukang.JkAgentRelationBindRequest;
import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAgentRelationDao;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import com.zbkj.service.service.jiuzhoukang.region.JkAgentRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JkAgentRelationServiceImpl implements JkAgentRelationService {
    @Autowired private JkAgentRelationDao relationDao;
    @Autowired private UserService userService;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private JkBusinessRoleDao roleDao;
    @Autowired private JkRegionDao regionDao;
    @Autowired private JkPermissionCacheVersionService cacheVersionService;

    @Override
    public List<JkAgentRelationResponse> list(Long userId, Long parentUserId, Boolean activeOnly) {
        LambdaQueryWrapper<JkAgentRelation> query = new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getIsDeleted, false).orderByDesc(JkAgentRelation::getId);
        if (userId != null) query.eq(JkAgentRelation::getUserId, userId);
        if (parentUserId != null) query.eq(JkAgentRelation::getParentUserId, parentUserId);
        if (Boolean.TRUE.equals(activeOnly)) query.eq(JkAgentRelation::getStatus, true);
        List<JkAgentRelation> rows = relationDao.selectList(query);

        Set<Long> userIds = new HashSet<>();
        for (JkAgentRelation relation : rows) {
            if (relation.getUserId() != null) userIds.add(relation.getUserId());
            if (relation.getParentUserId() != null) userIds.add(relation.getParentUserId());
        }
        if (userIds.isEmpty()) return Collections.emptyList();

        List<Integer> integerIds = userIds.stream().map(Long::intValue).collect(Collectors.toList());
        Map<Integer, User> users = userService.listByIds(integerIds).stream()
                .collect(Collectors.toMap(User::getUid, value -> value, (a, b) -> a));
        Map<Long, JkUserBusinessRole> identityMap = loadPrimaryIdentities(userIds);
        Map<String, String> roleNames = roleDao.selectList(new LambdaQueryWrapper<JkBusinessRole>()
                        .eq(JkBusinessRole::getIsDeleted, false)).stream()
                .collect(Collectors.toMap(JkBusinessRole::getRoleCode, JkBusinessRole::getRoleName, (a, b) -> a));
        Set<String> regionCodes = identityMap.values().stream().map(JkUserBusinessRole::getRegionCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Map<String, String> regionNames = regionCodes.isEmpty() ? Collections.emptyMap()
                : regionDao.selectList(new LambdaQueryWrapper<JkRegion>()
                .in(JkRegion::getRegionCode, regionCodes).eq(JkRegion::getIsDeleted, false)).stream()
                .collect(Collectors.toMap(JkRegion::getRegionCode, JkRegion::getRegionName, (a, b) -> a));

        List<JkAgentRelationResponse> result = new ArrayList<>();
        for (JkAgentRelation row : rows) {
            JkAgentRelationResponse response = new JkAgentRelationResponse();
            response.setId(row.getId());
            response.setUserId(row.getUserId());
            response.setParentUserId(row.getParentUserId());
            response.setRootUserId(row.getRootUserId());
            response.setRelationType(row.getRelationType());
            response.setBindSource(row.getBindSource());
            response.setSourceCode(row.getSourceCode());
            response.setEffectiveTime(row.getEffectiveTime());
            response.setExpireTime(row.getExpireTime());
            response.setChangeReason(row.getChangeReason());
            response.setRemark(row.getRemark());
            response.setStatus(row.getStatus());
            response.setCreateTime(row.getCreateTime());

            User user = row.getUserId() == null ? null : users.get(row.getUserId().intValue());
            User parent = row.getParentUserId() == null ? null : users.get(row.getParentUserId().intValue());
            if (user != null) {
                response.setUserName(StrUtil.blankToDefault(user.getRealName(), user.getNickname()));
                response.setUserPhone(user.getPhone());
                response.setUserAvatar(user.getAvatar());
            }
            if (parent != null) {
                response.setParentName(StrUtil.blankToDefault(parent.getRealName(), parent.getNickname()));
                response.setParentPhone(parent.getPhone());
            }
            JkUserBusinessRole identity = identityMap.get(row.getUserId());
            if (identity != null) {
                response.setRoleCode(identity.getRoleCode());
                response.setRoleName(roleNames.getOrDefault(identity.getRoleCode(), identity.getRoleCode()));
                response.setRegionCode(identity.getRegionCode());
                response.setRegionName(StrUtil.isBlank(identity.getRegionCode()) ? null : regionNames.get(identity.getRegionCode()));
                response.setFreezeStatus(identity.getFreezeStatus());
                response.setIdentityStatusText(Boolean.TRUE.equals(identity.getFreezeStatus()) ? "身份冻结" : "身份正常");
            }
            result.add(response);
        }
        return result;
    }

    private Map<Long, JkUserBusinessRole> loadPrimaryIdentities(Set<Long> userIds) {
        List<JkUserBusinessRole> roles = userRoleDao.selectList(new LambdaQueryWrapper<JkUserBusinessRole>()
                .in(JkUserBusinessRole::getUserId, userIds)
                .eq(JkUserBusinessRole::getAuditStatus, JkBizConstants.AUDIT_STATUS_EFFECTIVE)
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .orderByDesc(JkUserBusinessRole::getIsPrimary)
                .orderByDesc(JkUserBusinessRole::getId));
        Map<Long, JkUserBusinessRole> result = new LinkedHashMap<>();
        for (JkUserBusinessRole role : roles) result.putIfAbsent(role.getUserId(), role);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkAgentRelationResponse bind(JkAgentRelationBindRequest request, Long operatorId) {
        if (request.getParentUserId() != null && request.getUserId().equals(request.getParentUserId())) throw new IllegalArgumentException("不能绑定自己为上级");
        User user = userService.getById(request.getUserId().intValue());
        if (user == null) throw new IllegalArgumentException("下级用户不存在");
        User parent = request.getParentUserId() == null ? null : userService.getById(request.getParentUserId().intValue());
        if (request.getParentUserId() != null && parent == null) throw new IllegalArgumentException("上级用户不存在");
        assertNoCycle(request.getUserId(), request.getParentUserId());
        JkAgentRelation current = relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                .eq(JkAgentRelation::getUserId, request.getUserId()).eq(JkAgentRelation::getStatus, true)
                .eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
        if (current != null && Objects.equals(current.getParentUserId(), request.getParentUserId())) return list(request.getUserId(), null, true).get(0);
        Date now = new Date();
        if (current != null) {
            current.setStatus(false).setExpireTime(now).setChangeReason(StrUtil.blankToDefault(request.getChangeReason(), "关系换绑"))
                    .setUpdateUserId(operatorId).setUpdateTime(now);
            relationDao.updateById(current);
        }
        Long root = resolveRoot(request.getParentUserId());
        JkAgentRelation entity = new JkAgentRelation().setUserId(request.getUserId()).setParentUserId(request.getParentUserId()).setRootUserId(root)
                .setRelationType(StrUtil.blankToDefault(request.getRelationType(), "DIRECT"))
                .setBindSource(StrUtil.blankToDefault(request.getBindSource(), "ADMIN"))
                .setSourceCode(request.getSourceCode()).setEffectiveTime(now)
                .setReplacedRelationId(current == null ? null : current.getId()).setChangeReason(request.getChangeReason()).setRemark(request.getRemark())
                .setStatus(true).setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId)
                .setCreateTime(now).setUpdateTime(now).setTenantId("000000");
        relationDao.insert(entity);
        user.setSpreadUid(request.getParentUserId() == null ? 0 : Math.toIntExact(request.getParentUserId()));
        userService.updateById(user);
        cacheVersionService.refreshUserCacheVersion(request.getUserId(), "AGENT_RELATION", "上下级关系变更", operatorId);
        return list(request.getUserId(), null, true).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean invalidate(Long id, String reason, Long operatorId) {
        JkAgentRelation entity = relationDao.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) return false;
        Date now = new Date();
        entity.setStatus(false).setExpireTime(now).setChangeReason(reason).setUpdateUserId(operatorId).setUpdateTime(now);
        int result = relationDao.updateById(entity);
        User user = userService.getById(entity.getUserId().intValue());
        if (user != null) {
            user.setSpreadUid(0);
            userService.updateById(user);
        }
        cacheVersionService.refreshUserCacheVersion(entity.getUserId(), "AGENT_RELATION", "上下级关系失效", operatorId);
        return result == 1;
    }

    private void assertNoCycle(Long userId, Long parentUserId) {
        Long cursor = parentUserId;
        Set<Long> seen = new HashSet<>();
        for (int i = 0; cursor != null && i < 100; i++) {
            if (userId.equals(cursor)) throw new IllegalArgumentException("绑定会形成循环关系");
            if (!seen.add(cursor)) throw new IllegalArgumentException("现有关系链存在循环，请先修复");
            JkAgentRelation relation = relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                    .eq(JkAgentRelation::getUserId, cursor).eq(JkAgentRelation::getStatus, true)
                    .eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
            cursor = relation == null ? null : relation.getParentUserId();
        }
        if (cursor != null) throw new IllegalArgumentException("关系层级超过系统安全上限");
    }

    private Long resolveRoot(Long parentUserId) {
        if (parentUserId == null) return null;
        Long cursor = parentUserId;
        Long root = parentUserId;
        for (int i = 0; i < 100; i++) {
            JkAgentRelation relation = relationDao.selectOne(new LambdaQueryWrapper<JkAgentRelation>()
                    .eq(JkAgentRelation::getUserId, cursor).eq(JkAgentRelation::getStatus, true)
                    .eq(JkAgentRelation::getIsDeleted, false).last("limit 1"));
            if (relation == null || relation.getParentUserId() == null) return relation != null && relation.getRootUserId() != null ? relation.getRootUserId() : root;
            root = relation.getParentUserId();
            cursor = relation.getParentUserId();
        }
        return root;
    }
}
