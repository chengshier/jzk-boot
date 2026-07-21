package com.zbkj.service.service.jiuzhoukang.context;

public interface JkUserContextService {
    JkUserContext getFrontContext(Long userId);
    JkUserContext getAnonymousContext();
    JkUserContext getAdminContext();
    void assertHasPermission(String permissionCode, boolean checkDataScope);
}
