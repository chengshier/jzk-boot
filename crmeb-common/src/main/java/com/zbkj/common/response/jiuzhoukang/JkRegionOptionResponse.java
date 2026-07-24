package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 区域选择器节点响应。
 */
@Data
@Accessors(chain = true)
public class JkRegionOptionResponse implements Serializable {
    /** 展示名称。 */
    private String label;
    /** 选项值。 */
    private String value;
    /** 区域层级。 */
    private Integer regionLevel;
    /** 是否为叶子节点。 */
    private Boolean leaf;
    /** 是否禁用。 */
    private Boolean disabled;
}
