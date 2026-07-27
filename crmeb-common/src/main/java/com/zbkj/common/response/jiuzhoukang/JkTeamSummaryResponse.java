package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.util.List;

@Data
public class JkTeamSummaryResponse {
    private Long userId;
    private JkAgentRelationResponse currentRelation;
    private Integer directTeamCount;
    /** 包含全部层级且去重后的团队人数。 */
    private Integer totalTeamCount;
    /** 旧 App 字段兼容别名。 */
    private Integer teamCount;
    private Integer todayNewCount;
    private Integer monthNewCount;
    private List<JkAgentRelationResponse> directTeam;
    private List<JkAgentRelationResponse> relationHistory;
}
