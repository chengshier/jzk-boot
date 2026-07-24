package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class JkOptionResponse implements Serializable {
    private String value;
    private String label;
    private String extra;
    private String phone;
    private String roleCode;
    private String roleName;
    private String regionCode;
    private Boolean disabled;
}
