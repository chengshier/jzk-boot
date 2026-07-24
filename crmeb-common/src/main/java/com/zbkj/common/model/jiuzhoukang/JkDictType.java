package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jk_dict_type")
public class JkDictType implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String dictType;
    private String dictName;
    private String remark;
    private Boolean status;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
