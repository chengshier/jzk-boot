package com.zbkj.service.service.jiuzhoukang.audit;

import com.zbkj.common.model.system.SystemAdmin;

public interface JkAdminActorService {
    SystemAdmin getCurrentAdmin();
    Long getLinkedFrontUserId(SystemAdmin admin);
    boolean isPlatformSuperAdmin(SystemAdmin admin);
}
