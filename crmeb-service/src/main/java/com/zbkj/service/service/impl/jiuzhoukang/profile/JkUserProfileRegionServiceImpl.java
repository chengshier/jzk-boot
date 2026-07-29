package com.zbkj.service.service.impl.jiuzhoukang.profile;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.model.user.User;
import com.zbkj.common.request.jiuzhoukang.JkUserProfileRegionSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkRegionPathResponse;
import com.zbkj.common.response.jiuzhoukang.JkUserProfileRegionResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAuditLogDao;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.jiuzhoukang.profile.JkUserProfileRegionService;
import com.zbkj.service.service.jiuzhoukang.region.JkRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 复用 eb_user 保存用户稳定区域。不会创建 jk_user_business_role，也不会读取或覆盖默认收货地址。
 */
@Service
public class JkUserProfileRegionServiceImpl implements JkUserProfileRegionService {
    private static final String NOTICE = "所在地区用于后续九州康业务归属判断；实际配送仍以每笔订单选择的收货地址为准。修改后不影响历史订单。";

    @Autowired private UserService userService;
    @Autowired private JkRegionService regionService;
    @Autowired private JkAuditLogDao auditLogDao;

    @Override
    public JkUserProfileRegionResponse get(Long userId) {
        User user = requireUser(userId);
        return build(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkUserProfileRegionResponse saveByUser(Long userId, JkUserProfileRegionSaveRequest request) {
        return save(userId, "USER_PROFILE", null, null, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkUserProfileRegionResponse saveByAdmin(Long userId, Long adminId, String adminName,
                                                   JkUserProfileRegionSaveRequest request) {
        return save(userId, "ADMIN", adminId, adminName, request);
    }

    private JkUserProfileRegionResponse save(Long userId, String source, Long adminId, String adminName,
                                             JkUserProfileRegionSaveRequest request) {
        if (request == null || request.getRegionCode() == null || request.getRegionCode().trim().isEmpty()) {
            throw new CrmebException("请选择所在地区");
        }
        String regionCode = request.getRegionCode().trim();
        JkRegionPathResponse path = regionService.getRegionPath(regionCode);
        JkRegion current = path == null || path.getCurrent() == null ? null : new JkRegion()
                .setRegionCode(path.getCurrent().getRegionCode())
                .setRegionName(path.getCurrent().getRegionName());
        if (current == null || current.getRegionCode() == null) {
            throw new CrmebException("所在地区不是有效的九州康区域节点");
        }

        User user = requireUser(userId);
        String before = user.getJkRegionCode();
        Date now = new Date();
        user.setJkRegionCode(regionCode);
        user.setJkRegionSource(source);
        user.setJkRegionUpdateTime(now);
        user.setUpdateTime(now);
        if (!userService.updateById(user)) {
            throw new CrmebException("保存所在地区失败");
        }

        if (adminId != null) {
            JkAuditLog log = new JkAuditLog();
            log.setBusinessType("USER_PROFILE_REGION");
            log.setBusinessId(userId);
            log.setBusinessNo(String.valueOf(userId));
            log.setRequestNo(request.getRequestNo() == null || request.getRequestNo().trim().isEmpty()
                    ? "UPR" + IdWorker.getIdStr() : request.getRequestNo().trim());
            log.setAuditUserId(adminId);
            log.setAuditUserName(adminName);
            log.setAuditUserType("ADMIN");
            log.setAuditAction("UPDATE_REGION");
            log.setBeforeStatus(before);
            log.setAfterStatus(regionCode);
            log.setAuditRemark(request.getReason());
            log.setOperateSource("ADMIN_USER_PROFILE");
            log.setStatus(true);
            log.setIsDeleted(false);
            log.setCreateUserId(adminId);
            log.setUpdateUserId(adminId);
            log.setCreateTime(now);
            log.setUpdateTime(now);
            log.setTenantId("000000");
            log.setCreateDept(0L);
            auditLogDao.insert(log);
        }
        return build(user);
    }

    private User requireUser(Long userId) {
        if (userId == null) throw new CrmebException("用户不能为空");
        User user = userService.getById(userId.intValue());
        if (user == null) throw new CrmebException("用户不存在");
        return user;
    }

    private JkUserProfileRegionResponse build(User user) {
        JkUserProfileRegionResponse response = new JkUserProfileRegionResponse();
        response.setUserId(user.getUid().longValue());
        response.setRegionCode(user.getJkRegionCode());
        response.setRegionSource(user.getJkRegionSource());
        response.setRegionUpdateTime(user.getJkRegionUpdateTime());
        response.setDetailAddress(user.getAddres());
        response.setNotice(NOTICE);
        if (user.getJkRegionCode() != null && !user.getJkRegionCode().trim().isEmpty()) {
            JkRegionPathResponse path = regionService.getRegionPath(user.getJkRegionCode());
            if (path != null) {
                response.setRegionPathName(path.getFullPathName());
                if (path.getCurrent() != null) response.setRegionName(path.getCurrent().getRegionName());
            }
        }
        return response;
    }
}
