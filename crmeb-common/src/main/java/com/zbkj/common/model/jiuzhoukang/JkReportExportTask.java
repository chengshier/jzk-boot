package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 大数据量报表异步导出任务。下载接口仍会再次校验权限。 */
@Data
@Accessors(chain = true)
@TableName("jk_report_export_task")
public class JkReportExportTask implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String taskNo;
    private String reportType;
    private String requestNo;
    private String requestJson;
    private String status;
    private Integer progress;
    private String storageProvider;
    private String objectKey;
    /** 历史兼容字段，V3.1 不再写本地绝对路径。 */
    private String filePath;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Integer downloadCount;
    private String errorMessage;
    /** 历史字段。 */
    private Long requestUserId;
    private Long createdBy;
    private Date completedTime;
    private Date expireTime;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false) private String downloadUrl;
}
