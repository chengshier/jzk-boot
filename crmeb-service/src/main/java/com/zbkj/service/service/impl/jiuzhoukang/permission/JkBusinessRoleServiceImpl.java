package com.zbkj.service.service.impl.jiuzhoukang.permission;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.exception.jiuzhoukang.JkBizException;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkBusinessPermission;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRolePermission;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRoleSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessRoleSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkBusinessRoleResponse;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessPermissionDao;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRolePermissionDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JkBusinessRoleServiceImpl extends ServiceImpl<JkBusinessRoleDao, JkBusinessRole> implements JkBusinessRoleService {

    @Autowired
    private JkBusinessRolePermissionDao rolePermissionDao;
    @Autowired
    private JkBusinessPermissionDao permissionDao;
    @Autowired
    private JkUserBusinessRoleDao userBusinessRoleDao;
    @Autowired
    private JkPermissionCacheVersionService cacheVersionService;

    @Override
    public List<JkBusinessRoleResponse> getList(JkBusinessRoleSearchRequest request) {
        LambdaQueryWrapper<JkBusinessRole> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkBusinessRole::getIsDeleted, false);
        if (request != null && ObjectUtil.isNotNull(request.getEnabled())) {
            lqw.eq(JkBusinessRole::getEnabled, request.getEnabled());
        }
        if (request != null && StrUtil.isNotBlank(request.getKeyword())) {
            lqw.and(i -> i.like(JkBusinessRole::getRoleName, request.getKeyword())
                    .or().like(JkBusinessRole::getRoleCode, request.getKeyword()));
        }
        lqw.orderByAsc(JkBusinessRole::getSort).orderByAsc(JkBusinessRole::getId);
        List<JkBusinessRole> roles = list(lqw);
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<String>> permissionMap = rolePermissionDao.selectList(new LambdaQueryWrapper<JkBusinessRolePermission>()
                        .eq(JkBusinessRolePermission::getIsDeleted, false))
                .stream()
                .collect(Collectors.groupingBy(JkBusinessRolePermission::getRoleId,
                        Collectors.mapping(JkBusinessRolePermission::getPermissionCode, Collectors.toList())));
        Map<String, String> permissionNameMap = permissionDao.selectList(new LambdaQueryWrapper<JkBusinessPermission>()
                        .eq(JkBusinessPermission::getIsDeleted, false)
                        .eq(JkBusinessPermission::getEnabled, true))
                .stream()
                .collect(Collectors.toMap(JkBusinessPermission::getPermissionCode,
                        item -> StrUtil.blankToDefault(item.getPermissionName(), item.getPermissionCode()),
                        (left, right) -> left));
        return roles.stream().map(role -> {
            JkBusinessRoleResponse response = new JkBusinessRoleResponse();
            BeanUtils.copyProperties(role, response);
            List<String> permissionCodes = permissionMap.getOrDefault(role.getId(), Collections.emptyList());
            response.setPermissionCodes(permissionCodes);
            response.setPermissionNames(permissionCodes.stream()
                    .map(code -> permissionNameMap.getOrDefault(code, code))
                    .collect(Collectors.toList()));
            response.setPermissionDisplayList(permissionCodes.stream()
                    .map(code -> {
                        String permissionName = permissionNameMap.get(code);
                        return StrUtil.isBlank(permissionName) || StrUtil.equals(permissionName, code)
                                ? code
                                : permissionName + "（" + code + "）";
                    })
                    .collect(Collectors.toList()));
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    public List<JkBusinessRole> getEnabledRoleList() {
        LambdaQueryWrapper<JkBusinessRole> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkBusinessRole::getIsDeleted, false);
        lqw.eq(JkBusinessRole::getEnabled, true);
        lqw.orderByAsc(JkBusinessRole::getSort).orderByAsc(JkBusinessRole::getId);
        return list(lqw);
    }

    @Override
    public List<String> getPermissionCodes(Long roleId) {
        LambdaQueryWrapper<JkBusinessRolePermission> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkBusinessRolePermission::getRoleId, roleId);
        lqw.eq(JkBusinessRolePermission::getIsDeleted, false);
        return rolePermissionDao.selectList(lqw).stream().map(JkBusinessRolePermission::getPermissionCode).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateEnabled(Long roleId, Boolean enabled) {
        JkBusinessRole role = getById(roleId);
        if (role == null || Boolean.TRUE.equals(role.getIsDeleted())) throw new JkBizException("业务角色不存在");
        role.setEnabled(enabled).setUpdateTime(new Date());
        boolean result = updateById(role);
        refreshRoleUsers(roleId, "角色启停变更", null);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkBusinessRole saveRole(JkBusinessRoleSaveRequest request, Long operatorId) {
        JkBusinessRole role = request.getId() == null ? new JkBusinessRole() : getById(request.getId());
        if (role == null) throw new JkBizException("业务角色不存在");
        JkBusinessRole duplicate = getOne(new LambdaQueryWrapper<JkBusinessRole>()
                .eq(JkBusinessRole::getRoleCode, request.getRoleCode().trim()).eq(JkBusinessRole::getIsDeleted, false)
                .ne(request.getId()!=null, JkBusinessRole::getId, request.getId()).last("limit 1"));
        if (duplicate != null) throw new JkBizException("角色编码已存在");
        if (Boolean.TRUE.equals(role.getIsSystem()) && request.getId()!=null && !role.getRoleCode().equals(request.getRoleCode().trim())) {
            throw new JkBizException("系统内置角色不允许修改编码");
        }
        Date now = new Date();
        role.setRoleCode(request.getRoleCode().trim()).setRoleName(request.getRoleName().trim())
                .setRoleType(StrUtil.blankToDefault(request.getRoleType(), "FRONT_BUSINESS"))
                .setRoleLevel(request.getRoleLevel()==null?0:request.getRoleLevel())
                .setNeedAudit(request.getNeedAudit()==null||request.getNeedAudit())
                .setAllowFrontApply(Boolean.TRUE.equals(request.getAllowFrontApply()))
                .setEnabled(request.getEnabled()==null||request.getEnabled()).setSort(request.getSort()==null?0:request.getSort())
                .setRemark(request.getRemark()).setStatus(true).setIsDeleted(false).setUpdateUserId(operatorId).setUpdateTime(now);
        if (role.getId()==null) { role.setIsSystem(false).setCreateUserId(operatorId).setCreateTime(now).setTenantId("000000"); save(role); }
        else updateById(role);
        refreshRoleUsers(role.getId(), "角色配置变更", operatorId);
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignPermissions(Long roleId, List<String> permissionCodes, Long operatorId) {
        JkBusinessRole role = getById(roleId);
        if (role == null || Boolean.TRUE.equals(role.getIsDeleted())) throw new JkBizException("业务角色不存在");
        List<String> codes = permissionCodes == null ? Collections.emptyList() : permissionCodes.stream()
                .filter(StrUtil::isNotBlank).map(String::trim).distinct().collect(Collectors.toList());
        if (!codes.isEmpty()) {
            long count = permissionDao.selectCount(new LambdaQueryWrapper<JkBusinessPermission>()
                    .in(JkBusinessPermission::getPermissionCode, codes).eq(JkBusinessPermission::getEnabled, true).eq(JkBusinessPermission::getIsDeleted, false));
            if (count != codes.size()) throw new JkBizException("包含不存在或已停用的权限点");
        }
        rolePermissionDao.delete(new LambdaQueryWrapper<JkBusinessRolePermission>().eq(JkBusinessRolePermission::getRoleId, roleId));
        Date now = new Date();
        for (String code : codes) rolePermissionDao.insert(new JkBusinessRolePermission().setRoleId(roleId).setPermissionCode(code)
                .setStatus(true).setIsDeleted(false).setCreateUserId(operatorId).setUpdateUserId(operatorId).setCreateTime(now).setUpdateTime(now).setTenantId("000000"));
        refreshRoleUsers(roleId, "角色权限重新授权", operatorId);
        return true;
    }

    private void refreshRoleUsers(Long roleId, String reason, Long operatorId) {
        userBusinessRoleDao.selectList(new LambdaQueryWrapper<com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole>()
                .eq(com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole::getRoleId, roleId).eq(com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole::getIsDeleted, false))
                .stream().map(com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole::getUserId).distinct()
                .forEach(userId -> cacheVersionService.refreshUserCacheVersion(userId, "ROLE_PERMISSION", reason, operatorId));
    }
}
