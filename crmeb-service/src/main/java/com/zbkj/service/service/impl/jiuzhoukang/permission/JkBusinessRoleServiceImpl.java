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
import com.zbkj.common.response.jiuzhoukang.JkBusinessRoleResponse;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessPermissionDao;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRolePermissionDao;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JkBusinessRoleServiceImpl extends ServiceImpl<JkBusinessRoleDao, JkBusinessRole> implements JkBusinessRoleService {

    @Autowired
    private JkBusinessRolePermissionDao rolePermissionDao;
    @Autowired
    private JkBusinessPermissionDao permissionDao;

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
    public Boolean updateEnabled(Long roleId, Boolean enabled) {
        JkBusinessRole role = getById(roleId);
        if (role == null || Boolean.TRUE.equals(role.getIsDeleted())) {
            throw new JkBizException("业务角色不存在");
        }
        role.setEnabled(enabled);
        return updateById(role);
    }
}
