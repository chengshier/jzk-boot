package com.zbkj.service.service.impl.jiuzhoukang.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.jiuzhoukang.JkBusinessPermission;
import com.zbkj.common.response.jiuzhoukang.JkBusinessPermissionResponse;
import com.zbkj.common.request.jiuzhoukang.JkBusinessPermissionSaveRequest;
import com.zbkj.common.model.jiuzhoukang.JkBusinessRolePermission;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessPermissionDao;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessRolePermissionDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessPermissionService;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JkBusinessPermissionServiceImpl extends ServiceImpl<JkBusinessPermissionDao, JkBusinessPermission> implements JkBusinessPermissionService {
    @Autowired private JkBusinessRolePermissionDao rolePermissionDao;
    @Autowired private JkUserBusinessRoleDao userRoleDao;
    @Autowired private JkPermissionCacheVersionService cacheVersionService;

    @Override
    public List<JkBusinessPermissionResponse> getList() {
        LambdaQueryWrapper<JkBusinessPermission> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkBusinessPermission::getIsDeleted, false);
        lqw.orderByAsc(JkBusinessPermission::getModuleCode).orderByAsc(JkBusinessPermission::getId);
        return list(lqw).stream().map(item -> {
            JkBusinessPermissionResponse response = new JkBusinessPermissionResponse();
            BeanUtils.copyProperties(item, response);
            response.setModuleName(labelModule(item.getModuleCode()));
            response.setPermissionTypeText(labelPermissionType(item.getPermissionType()));
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkBusinessPermission savePermission(JkBusinessPermissionSaveRequest request, Long operatorId) {
        JkBusinessPermission entity = request.getId()==null ? new JkBusinessPermission() : getById(request.getId());
        if (entity==null) throw new IllegalArgumentException("权限点不存在");
        JkBusinessPermission duplicate=getOne(new LambdaQueryWrapper<JkBusinessPermission>().eq(JkBusinessPermission::getPermissionCode,request.getPermissionCode().trim())
                .eq(JkBusinessPermission::getIsDeleted,false).ne(request.getId()!=null,JkBusinessPermission::getId,request.getId()).last("limit 1"));
        if(duplicate!=null)throw new IllegalArgumentException("权限编码已存在");
        Date now=new Date();entity.setPermissionCode(request.getPermissionCode().trim()).setPermissionName(request.getPermissionName().trim())
                .setModuleCode(request.getModuleCode().trim()).setPermissionType(request.getPermissionType()==null?"API":request.getPermissionType())
                .setEnabled(request.getEnabled()==null||request.getEnabled()).setRemark(request.getRemark()).setStatus(true).setIsDeleted(false).setUpdateUserId(operatorId).setUpdateTime(now);
        if(entity.getId()==null){entity.setCreateUserId(operatorId).setCreateTime(now).setTenantId("000000");save(entity);}else updateById(entity);
        refreshPermissionUsers(entity.getPermissionCode(),operatorId);return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateEnabled(Long permissionId, Boolean enabled, Long operatorId) {
        JkBusinessPermission entity=getById(permissionId);if(entity==null)throw new IllegalArgumentException("权限点不存在");
        entity.setEnabled(enabled).setUpdateUserId(operatorId).setUpdateTime(new Date());boolean result=updateById(entity);refreshPermissionUsers(entity.getPermissionCode(),operatorId);return result;
    }

    private void refreshPermissionUsers(String permissionCode,Long operatorId){
        Set<Long> roleIds=rolePermissionDao.selectList(new LambdaQueryWrapper<JkBusinessRolePermission>().eq(JkBusinessRolePermission::getPermissionCode,permissionCode).eq(JkBusinessRolePermission::getIsDeleted,false))
                .stream().map(JkBusinessRolePermission::getRoleId).collect(Collectors.toSet());if(roleIds.isEmpty())return;
        userRoleDao.selectList(new LambdaQueryWrapper<JkUserBusinessRole>().in(JkUserBusinessRole::getRoleId,roleIds).eq(JkUserBusinessRole::getIsDeleted,false)).stream().map(JkUserBusinessRole::getUserId).distinct()
                .forEach(userId->cacheVersionService.refreshUserCacheVersion(userId,"ROLE_PERMISSION","权限点配置变更",operatorId));
    }

    private String labelModule(String moduleCode) {
        if (moduleCode == null || moduleCode.trim().isEmpty()) {
            return "--";
        }
        if ("identity".equalsIgnoreCase(moduleCode)) {
            return "身份权限";
        }
        if ("trade".equalsIgnoreCase(moduleCode)) {
            return "交易视图";
        }
        if ("price".equalsIgnoreCase(moduleCode)) {
            return "价格规则";
        }
        if ("stock".equalsIgnoreCase(moduleCode)) {
            return "库存管理";
        }
        if ("audit".equalsIgnoreCase(moduleCode)) {
            return "审核日志";
        }
        if ("commission".equalsIgnoreCase(moduleCode)) {
            return "佣金管理";
        }
        if ("fund".equalsIgnoreCase(moduleCode)) {
            return "资金管理";
        }
        if ("withdraw".equalsIgnoreCase(moduleCode)) {
            return "提现管理";
        }
        return moduleCode;
    }

    private String labelPermissionType(String permissionType) {
        if (permissionType == null || permissionType.trim().isEmpty()) {
            return "--";
        }
        if ("VIEW".equalsIgnoreCase(permissionType)) {
            return "查看";
        }
        if ("ACTION".equalsIgnoreCase(permissionType)) {
            return "操作";
        }
        if ("CONFIG".equalsIgnoreCase(permissionType)) {
            return "配置";
        }
        if ("AUDIT".equalsIgnoreCase(permissionType)) {
            return "审核";
        }
        if ("SETTLE".equalsIgnoreCase(permissionType)) {
            return "结算";
        }
        return permissionType;
    }
}
