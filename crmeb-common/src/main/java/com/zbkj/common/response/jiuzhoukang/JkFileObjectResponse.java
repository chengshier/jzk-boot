package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class JkFileObjectResponse {
    private Long id;
    private String fileNo;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String businessType;
    private Long businessId;
    private String accessLevel;
    private String status;
    private Date createTime;
    private String downloadPath;
}
