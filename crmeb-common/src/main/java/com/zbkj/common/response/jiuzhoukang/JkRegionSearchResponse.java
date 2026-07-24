package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 区域搜索结果响应。
 */
@Data
@Accessors(chain = true)
public class JkRegionSearchResponse implements Serializable {
    /** 区域编码。 */
    private String regionCode;
    /** 区域名称。 */
    private String regionName;
    /** 区域层级。 */
    private Integer regionLevel;
    /** 上级区域编码。 */
    private String parentRegionCode;
    /** 完整路径名称。 */
    private String fullPathName;
    /** 完整路径编码。 */
    private List<String> fullPathCodes;
    /** 状态。 */
    private Boolean status;
    /** 是否已产生业务占用。 */
    private Boolean occupied;
}
