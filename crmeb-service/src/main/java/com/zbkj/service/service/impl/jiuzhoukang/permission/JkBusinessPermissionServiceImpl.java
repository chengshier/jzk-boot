package com.zbkj.service.service.impl.jiuzhoukang.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.jiuzhoukang.JkBusinessPermission;
import com.zbkj.common.response.jiuzhoukang.JkBusinessPermissionResponse;
import com.zbkj.service.dao.jiuzhoukang.JkBusinessPermissionDao;
import com.zbkj.service.service.jiuzhoukang.permission.JkBusinessPermissionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JkBusinessPermissionServiceImpl extends ServiceImpl<JkBusinessPermissionDao, JkBusinessPermission> implements JkBusinessPermissionService {

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
