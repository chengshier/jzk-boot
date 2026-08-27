package com.zbkj.service.service.jiuzhoukang.wechat;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkSubscriptionTask;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkSubscriptionTaskCreateRequest;

import java.util.Map;

public interface JkSubscriptionTaskService {
    JkSubscriptionTask enqueue(JkSubscriptionTaskCreateRequest request);
    int processDue(int limit);
    JkSubscriptionTask retry(Long taskId, String reason);
    PageInfo<JkSubscriptionTask> list(String status, String templateCode, Long receiverUserId, PageParamRequest page);
    Map<String, Object> status();
}
