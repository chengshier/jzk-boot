package com.zbkj.service.service.jiuzhoukang.storage;

import com.zbkj.common.model.jiuzhoukang.JkFileObject;
import com.zbkj.common.response.jiuzhoukang.JkFileObjectResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface JkFileStorageService {
    JkFileObjectResponse store(MultipartFile file, String businessType, Long businessId, Long ownerUserId, String accessLevel);
    JkFileObjectResponse storeBytes(byte[] bytes, String originalName, String contentType, String businessType,
                                    Long businessId, Long ownerUserId, String accessLevel);
    byte[] read(Long fileId, Long viewerUserId, boolean admin);
    JkFileObject require(Long fileId);
    Map<String, Object> status();
}
