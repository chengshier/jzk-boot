package com.zbkj.service.service.impl.jiuzhoukang.permission;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.jiuzhoukang.JkPermissionCacheVersion;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.service.dao.jiuzhoukang.JkPermissionCacheVersionDao;
import com.zbkj.service.service.jiuzhoukang.permission.JkPermissionCacheVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JkPermissionCacheVersionServiceImpl implements JkPermissionCacheVersionService {

    @Autowired
    private JkPermissionCacheVersionDao cacheVersionDao;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Long getUserCacheVersion(Long userId) {
        JkPermissionCacheVersion version = getOrCreate(userId);
        return version.getCacheVersion();
    }

    @Override
    public Long refreshUserCacheVersion(Long userId, String changeType, String reason, Long operatorId) {
        JkPermissionCacheVersion version = getOrCreate(userId);
        version.setCacheVersion(version.getCacheVersion() + 1);
        version.setChangeType(changeType);
        version.setChangeReason(reason);
        version.setUpdateUserId(operatorId);
        version.setUpdateTime(DateUtil.date());
        cacheVersionDao.updateById(version);
        clearUserContextCache(userId);
        return version.getCacheVersion();
    }

    @Override
    public void clearUserContextCache(Long userId) {
        redisUtil.delete(JkBizConstants.REDIS_CONTEXT_KEY_PREFIX + userId);
    }

    private JkPermissionCacheVersion getOrCreate(Long userId) {
        LambdaQueryWrapper<JkPermissionCacheVersion> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkPermissionCacheVersion::getUserId, userId);
        lqw.last(" limit 1");
        JkPermissionCacheVersion version = cacheVersionDao.selectOne(lqw);
        if (version != null) {
            return version;
        }
        version = new JkPermissionCacheVersion();
        version.setUserId(userId);
        version.setCacheVersion(1L);
        version.setStatus(true);
        version.setIsDeleted(false);
        version.setCreateTime(DateUtil.date());
        version.setUpdateTime(DateUtil.date());
        cacheVersionDao.insert(version);
        return version;
    }
}
