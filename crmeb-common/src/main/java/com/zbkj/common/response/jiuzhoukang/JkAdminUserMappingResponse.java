package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import java.util.Date;

@Data
public class JkAdminUserMappingResponse {
    private Long id;
    private Integer systemAdminId;
    private String adminRealName;
    private String adminAccount;
    private Long frontUserId;
    private String frontUserName;
    private String frontUserPhone;
    private String remark;
    private Boolean status;
    private Date createTime;
}
