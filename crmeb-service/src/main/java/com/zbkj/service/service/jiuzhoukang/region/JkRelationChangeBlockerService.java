package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.model.jiuzhoukang.JkAgentRelationChangeApply;
import com.zbkj.common.response.jiuzhoukang.JkRelationChangeBlockerResponse;

import java.util.List;

/** 换绑审核实时阻断检查。 */
public interface JkRelationChangeBlockerService {
    List<JkRelationChangeBlockerResponse> check(JkAgentRelationChangeApply apply);
    JkAgentRelationChangeApply fill(JkAgentRelationChangeApply apply);
    void assertNoBlockers(JkAgentRelationChangeApply apply);
}
