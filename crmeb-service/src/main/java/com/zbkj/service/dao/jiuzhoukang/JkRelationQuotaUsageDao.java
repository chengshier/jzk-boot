package com.zbkj.service.dao.jiuzhoukang;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.jiuzhoukang.JkRelationQuotaUsage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface JkRelationQuotaUsageDao extends BaseMapper<JkRelationQuotaUsage> {

    @Insert("INSERT IGNORE INTO jk_relation_quota_usage " +
            "(parent_user_id, used_count, reserved_count, version, is_deleted, create_time, update_time, tenant_id) " +
            "VALUES (#{parentUserId}, 0, 0, 0, 0, NOW(), NOW(), '000000')")
    int insertIgnore(@Param("parentUserId") Long parentUserId);

    @Select("SELECT * FROM jk_relation_quota_usage " +
            "WHERE parent_user_id = #{parentUserId} AND is_deleted = 0 LIMIT 1 FOR UPDATE")
    JkRelationQuotaUsage selectForUpdate(@Param("parentUserId") Long parentUserId);
}
