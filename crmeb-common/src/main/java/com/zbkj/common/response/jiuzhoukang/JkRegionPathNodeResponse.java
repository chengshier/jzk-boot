package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 区域路径节点响应。
 */
@Data
@Accessors(chain = true)
public class JkRegionPathNodeResponse implements Serializable {
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
    /** 状态。 */
    private Boolean status;
}
