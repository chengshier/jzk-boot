package com.zbkj.service.service.impl.jiuzhoukang.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkAdminUserMapping;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.utils.SecurityUtil;
import com.zbkj.service.dao.jiuzhoukang.JkAdminUserMappingDao;
import com.zbkj.service.service.jiuzhoukang.audit.JkAdminActorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JkAdminActorServiceImpl implements JkAdminActorService {

    @Autowired
    private JkAdminUserMappingDao adminUserMappingDao;

    @Override
    public SystemAdmin getCurrentAdmin() {
        return SecurityUtil.getLoginUserVo().getUser();
    }

    @Override
    public Long getLinkedFrontUserId(SystemAdmin admin) {
        if (admin == null || admin.getId() == null) {
            return null;
        }
        LambdaQueryWrapper<JkAdminUserMapping> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkAdminUserMapping::getSystemAdminId, admin.getId());
        lqw.eq(JkAdminUserMapping::getStatus, true);
        lqw.eq(JkAdminUserMapping::getIsDeleted, false);
        lqw.last(" limit 1");
        JkAdminUserMapping mapping = adminUserMappingDao.selectOne(lqw);
        if (mapping != null) {
            return mapping.getFrontUserId();
        }
        // 风险说明：阶段一不再默认通过手机号碰撞授予业务上下文，避免误绑前台用户。
        // 如需应急兼容手机号 fallback，必须在后续明确增加开关与审计说明后再启用。
        return null;
    }

    @Override
//    public boolean isPlatformSuperAdmin(SystemAdmin admin) {
//        if (admin == null) {
//            return false;
//        }
//        if (admin.getRoles() != null && admin.getRoles().contains("1")) {
//            return true;
//        }
//        return admin.getLevel() != null && admin.getLevel() == 1;
//    }
    public boolean isPlatformSuperAdmin(SystemAdmin admin) {
        if (admin == null) {
            return false;
        }

        // CRMEB 原系统中，角色 ID=1 为超级管理员。
        // 当前项目同时保留历史 level=0 超管账号兼容。
        boolean hasSuperAdminRole = false;
        if (admin.getRoles() != null && !admin.getRoles().trim().isEmpty()) {
            String[] roleIds = admin.getRoles().split(",");
            for (String roleId : roleIds) {
                if ("1".equals(roleId.trim())) {
                    hasSuperAdminRole = true;
                    break;
                }
            }
        }

        return hasSuperAdminRole || Integer.valueOf(0).equals(admin.getLevel());
    }
}
