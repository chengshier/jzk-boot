package com.zbkj.common.response.jiuzhoukang;

import com.zbkj.common.model.jiuzhoukang.*;
import lombok.Data;
import java.util.List;

@Data
public class JkHealthDashboardResponse {
    private JkHealthProfile profile;
    private JkHealthData latestGlucose;
    private Integer todayRecordCount;
    private Integer activeAlertCount;
    private Integer boundDeviceCount;
    private List<JkHealthData> recentRecords;
}
