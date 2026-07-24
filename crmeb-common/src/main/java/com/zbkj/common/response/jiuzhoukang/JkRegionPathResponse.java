package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 区域完整路径响应。
 */
@Data
@Accessors(chain = true)
public class JkRegionPathResponse implements Serializable {
    /** 当前区域节点。 */
    private JkRegionPathNodeResponse current;
    /** 从省到当前区域的路径节点。 */
    private List<JkRegionPathNodeResponse> nodes;
    /** 完整路径名称。 */
    private String fullPathName;
    /** 完整路径编码。 */
    private List<String> fullPathCodes;
}
