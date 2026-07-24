package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 区域树节点响应。
 */
@Data
@Accessors(chain = true)
public class JkRegionTreeNodeResponse implements Serializable {
    /** 主键 ID。 */
    private Long id;
    /** 区域编码。 */
    private String regionCode;
    /** 区域名称。 */
    private String regionName;
    /** 上级区域编码。 */
    private String parentRegionCode;
    /** 区域层级。 */
    private Integer regionLevel;
    /** 是否已产生业务占用。 */
    private Boolean occupied;
    /** 状态。 */
    private Boolean status;
    /** 是否存在下级。 */
    private Boolean hasChildren;
    /** 直属下级数量。 */
    private Integer childCount;
}
