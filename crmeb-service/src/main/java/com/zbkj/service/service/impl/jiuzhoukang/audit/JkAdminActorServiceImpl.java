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
    public boolean isPlatformSuperAdmin(SystemAdmin admin) {
        if (admin == null) {
            return false;
        }
        if (admin.getRoles() != null && admin.getRoles().contains("1")) {
            return true;
        }
        return admin.getLevel() != null && admin.getLevel() == 1;
    }
}
