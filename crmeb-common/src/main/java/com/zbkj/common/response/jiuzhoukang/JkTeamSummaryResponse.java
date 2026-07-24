package com.zbkj.common.response.jiuzhoukang;

import com.zbkj.common.response.jiuzhoukang.JkAgentRelationResponse;
import lombok.Data;
import java.util.List;

@Data
public class JkTeamSummaryResponse {
    private Long userId;
    private JkAgentRelationResponse currentRelation;
    private Integer directTeamCount;
    private List<JkAgentRelationResponse> directTeam;
    private List<JkAgentRelationResponse> relationHistory;
}
