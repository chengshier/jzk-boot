package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.request.jiuzhoukang.JkWithdrawPayeeAccountSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkWithdrawPayeeAccountResponse;

import java.util.List;
import java.util.Map;

public interface JkWithdrawPayeeAccountService {
    List<JkWithdrawPayeeAccountResponse> list(Long userId);
    JkWithdrawPayeeAccountResponse save(Long userId, JkWithdrawPayeeAccountSaveRequest request);
    JkWithdrawPayeeAccountResponse setDefault(Long userId, Long id);
    void remove(Long userId, Long id);
    String buildSnapshotJson(Long userId, Long id);
    Map<String, Object> maskedSnapshot(String snapshotJson);
    Map<String, Object> revealSnapshot(String snapshotJson);
}
