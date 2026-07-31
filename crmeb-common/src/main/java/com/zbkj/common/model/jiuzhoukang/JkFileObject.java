package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_file_object")
public class JkFileObject implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String fileNo;
    private String storageProvider;
    private String bucketName;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String fileHash;
    private String businessType;
    private Long businessId;
    private Long ownerUserId;
    private String accessLevel;
    private String status;
    private Date expireTime;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
