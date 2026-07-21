package com.zbkj.service.service.jiuzhoukang.permission;

public interface JkPermissionCacheVersionService {
    Long getUserCacheVersion(Long userId);
    Long refreshUserCacheVersion(Long userId, String changeType, String reason, Long operatorId);
    void clearUserContextCache(Long userId);
}
