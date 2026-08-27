package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 已通过归属校验后返回的三诺报告；不直接暴露第三方文件地址。 */
@Data
@Accessors(chain = true)
public class JkSinocareReportResponse {
    private Long id;
    private String deviceSn;
    private String reportType;
    private Date createTime;
    private String payload;
    /** 文件报告的文件名（仅 PDF 类型有值）；不直接暴露第三方 filePath。 */
    private String fileName;
    /** 文件报告的文件大小（字节，仅 PDF 类型有值）。 */
    private Long fileSize;
    /** 数字报告统计周期文本（beginDate 至 endDate，仅 DIGITAL 类型有值）。 */
    private String periodText;
}
