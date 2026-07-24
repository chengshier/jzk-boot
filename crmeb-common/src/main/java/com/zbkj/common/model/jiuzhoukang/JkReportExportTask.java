package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/** 大数据量报表异步导出任务。下载接口仍会再次校验权限。 */
@Data @Accessors(chain = true) @TableName("jk_report_export_task")
public class JkReportExportTask implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO) private Long id;
    private String taskNo;
    private String reportType;
    private String requestJson;
    private String status;
    private Integer progress;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String errorMessage;
    private Long requestUserId;
    private Date expireTime;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
