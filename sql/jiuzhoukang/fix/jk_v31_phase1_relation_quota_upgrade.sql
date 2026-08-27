-- 九州康 V3.1 第一批：上下级人数规则、额度预占和后台权限升级（MySQL 5.7）
--
-- 目标：
-- 1. 直属下级人数不再写死，默认规则为 50 人；
-- 2. 换绑申请预占目标上级名额，避免审核期间并发超额；
-- 3. 普通绑定、审核换绑、管理员强制调整使用独立入口；
-- 4. 新增关系人数规则页面和强制调整按钮 authority。
--
-- 本脚本可重复执行。执行前请备份 jk_agent_relation、eb_system_menu。
-- 本脚本不自动给普通后台角色授权，执行后需在角色管理中分配新页面和按钮权限。

SET @now = NOW();

CREATE TABLE IF NOT EXISTS `jk_relation_limit_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_code` varchar(64) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(128) NOT NULL COMMENT '规则名称',
  `plan_id` bigint DEFAULT NULL COMMENT '商业方案ID，预留',
  `version_no` varchar(32) NOT NULL DEFAULT 'V1' COMMENT '规则版本',
  `parent_role_code` varchar(64) DEFAULT NULL COMMENT '上级角色，空表示通配',
  `child_role_code` varchar(64) DEFAULT NULL COMMENT '下级角色，空表示通配',
  `region_code` varchar(64) DEFAULT NULL COMMENT '区域编码，空表示通配',
  `max_direct_children` int NOT NULL DEFAULT 50 COMMENT '直属有效下级上限',
  `warning_threshold` int NOT NULL DEFAULT 80 COMMENT '使用率预警百分比',
  `overflow_policy` varchar(32) NOT NULL DEFAULT 'REJECT' COMMENT '超额策略：REJECT/APPROVAL',
  `priority` int NOT NULL DEFAULT 0 COMMENT '优先级，越大越优先',
  `effective_start_time` datetime DEFAULT NULL COMMENT '生效时间',
  `effective_end_time` datetime DEFAULT NULL COMMENT '失效时间',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '软删除',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(32) DEFAULT '000000' COMMENT '租户',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relation_limit_rule_version` (`rule_code`,`version_no`),
  KEY `idx_relation_limit_match` (`status`,`parent_role_code`,`child_role_code`,`region_code`,`priority`),
  KEY `idx_relation_limit_effective` (`effective_start_time`,`effective_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康直属下级人数限制规则';

CREATE TABLE IF NOT EXISTS `jk_relation_quota_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_user_id` bigint NOT NULL COMMENT '上级用户ID',
  `used_count` int NOT NULL DEFAULT 0 COMMENT '有效直属关系数量快照',
  `reserved_count` int NOT NULL DEFAULT 0 COMMENT '换绑预占数量快照',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观版本',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '软删除',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(32) DEFAULT '000000' COMMENT '租户',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relation_quota_parent` (`parent_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康直属人数额度使用快照';

CREATE TABLE IF NOT EXISTS `jk_relation_quota_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `reservation_no` varchar(64) NOT NULL COMMENT '预占单号',
  `request_no` varchar(64) NOT NULL COMMENT '业务幂等号',
  `scene` varchar(32) NOT NULL COMMENT '预占场景',
  `parent_user_id` bigint NOT NULL COMMENT '目标上级',
  `child_user_id` bigint NOT NULL COMMENT '申请换绑用户',
  `rule_id` bigint DEFAULT NULL COMMENT '命中规则ID',
  `status` varchar(32) NOT NULL COMMENT 'RESERVED/CONSUMED/REJECTED/CANCELLED/EXPIRED',
  `expire_time` datetime DEFAULT NULL COMMENT '预占到期时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '软删除',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建人',
  `update_user_id` bigint DEFAULT NULL COMMENT '更新人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(32) DEFAULT '000000' COMMENT '租户',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_relation_quota_request` (`request_no`),
  UNIQUE KEY `uk_relation_quota_no` (`reservation_no`),
  KEY `idx_relation_quota_parent_status` (`parent_user_id`,`status`,`expire_time`),
  KEY `idx_relation_quota_child` (`child_user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康换绑目标额度预占';

-- 默认 50 人是可配置种子，不是 Java 硬编码的唯一规则。
INSERT INTO `jk_relation_limit_rule`
(`rule_code`,`rule_name`,`version_no`,`max_direct_children`,`warning_threshold`,`overflow_policy`,`priority`,
 `status`,`remark`,`is_deleted`,`create_time`,`update_time`,`tenant_id`)
SELECT 'DEFAULT_DIRECT_LIMIT','默认直属人数限制','V1',50,80,'REJECT',0,1,
       'V3.1 默认规则；后续可按角色、区域和商业方案新增更高优先级规则',0,@now,@now,'000000'
WHERE NOT EXISTS (
  SELECT 1 FROM `jk_relation_limit_rule`
  WHERE `rule_code`='DEFAULT_DIRECT_LIMIT' AND `version_no`='V1'
);

UPDATE `jk_relation_limit_rule`
SET `rule_name`='默认直属人数限制',
    `max_direct_children`=50,
    `warning_threshold`=80,
    `overflow_policy`='REJECT',
    `status`=1,
    `is_deleted`=0,
    `update_time`=@now
WHERE `rule_code`='DEFAULT_DIRECT_LIMIT' AND `version_no`='V1';

-- 按现有有效关系回填额度快照；真实校验仍会在事务内重新统计有效关系。
INSERT INTO `jk_relation_quota_usage`
(`parent_user_id`,`used_count`,`reserved_count`,`version`,`is_deleted`,`create_time`,`update_time`,`tenant_id`)
SELECT r.`parent_user_id`, COUNT(1), 0, 0, 0, @now, @now, '000000'
FROM `jk_agent_relation` r
WHERE r.`parent_user_id` IS NOT NULL
  AND r.`status`=1
  AND r.`is_deleted`=0
GROUP BY r.`parent_user_id`
ON DUPLICATE KEY UPDATE
  `used_count`=VALUES(`used_count`),
  `update_time`=VALUES(`update_time`),
  `is_deleted`=0;

-- ============================================================================
-- 后台菜单与 authority
-- ============================================================================
SET @operation_root_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `path`='/operation' AND `is_del`=0
  ORDER BY `id` ASC LIMIT 1
);
SET @operation_root_id = IFNULL(@operation_root_id, 0);

SET @jk_root_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk' OR `name` IN ('九州康管理','九州康'))
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);
INSERT INTO `eb_system_menu`
(`pid`,`name`,`icon`,`perms`,`path`,`menu_type`,`sort`,`is_show`,`is_del`,`create_time`,`update_time`)
SELECT @operation_root_id,'九州康管理','s-data',NULL,'/operation/jzk','M',50,1,0,@now,@now
WHERE @jk_root_id IS NULL;
SET @jk_root_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk' OR `name` IN ('九州康管理','九州康'))
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);

SET @relation_limit_menu_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk/relationLimit'
      OR `perms`='admin:jk:relation:limit:rule:list')
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);
INSERT INTO `eb_system_menu`
(`pid`,`name`,`icon`,`perms`,`path`,`menu_type`,`sort`,`is_show`,`is_del`,`create_time`,`update_time`)
SELECT @jk_root_id,'绑定人数规则',NULL,'admin:jk:relation:limit:rule:list',
       '/operation/jzk/relationLimit','C',94,1,0,@now,@now
WHERE @relation_limit_menu_id IS NULL;
SET @relation_limit_menu_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk/relationLimit'
      OR `perms`='admin:jk:relation:limit:rule:list')
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);
UPDATE `eb_system_menu`
SET `pid`=@jk_root_id,`name`='绑定人数规则',`perms`='admin:jk:relation:limit:rule:list',
    `path`='/operation/jzk/relationLimit',`menu_type`='C',`sort`=94,`is_show`=1,`is_del`=0,`update_time`=@now
WHERE `id`=@relation_limit_menu_id;

-- 保存、启停按钮。
SET @relation_limit_save_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `perms`='admin:jk:relation:limit:rule:save'
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);
INSERT INTO `eb_system_menu`
(`pid`,`name`,`icon`,`perms`,`path`,`menu_type`,`sort`,`is_show`,`is_del`,`create_time`,`update_time`)
SELECT @relation_limit_menu_id,'保存绑定人数规则',NULL,'admin:jk:relation:limit:rule:save','',
       'A',1,1,0,@now,@now
WHERE @relation_limit_save_id IS NULL;

SET @relation_limit_status_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `perms`='admin:jk:relation:limit:rule:status'
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);
INSERT INTO `eb_system_menu`
(`pid`,`name`,`icon`,`perms`,`path`,`menu_type`,`sort`,`is_show`,`is_del`,`create_time`,`update_time`)
SELECT @relation_limit_menu_id,'启停绑定人数规则',NULL,'admin:jk:relation:limit:rule:status','',
       'A',2,1,0,@now,@now
WHERE @relation_limit_status_id IS NULL;

-- 强制调整挂在“上下级关系”页面下。
SET @agent_relation_menu_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk/agentRelation'
      OR `perms`='admin:jk:agent-relation:manage')
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);
SET @force_adjust_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `perms`='admin:jk:agent-relation:force-adjust'
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);
INSERT INTO `eb_system_menu`
(`pid`,`name`,`icon`,`perms`,`path`,`menu_type`,`sort`,`is_show`,`is_del`,`create_time`,`update_time`)
SELECT @agent_relation_menu_id,'管理员强制调整关系',NULL,
       'admin:jk:agent-relation:force-adjust','','A',9,1,0,@now,@now
WHERE @agent_relation_menu_id IS NOT NULL AND @force_adjust_id IS NULL;

-- 恢复历史软删除按钮并统一父菜单。
UPDATE `eb_system_menu`
SET `pid`=@relation_limit_menu_id,`name`='保存绑定人数规则',`path`='',`menu_type`='A',
    `sort`=1,`is_show`=1,`is_del`=0,`update_time`=@now
WHERE `perms`='admin:jk:relation:limit:rule:save';
UPDATE `eb_system_menu`
SET `pid`=@relation_limit_menu_id,`name`='启停绑定人数规则',`path`='',`menu_type`='A',
    `sort`=2,`is_show`=1,`is_del`=0,`update_time`=@now
WHERE `perms`='admin:jk:relation:limit:rule:status';
UPDATE `eb_system_menu`
SET `pid`=@agent_relation_menu_id,`name`='管理员强制调整关系',`path`='',`menu_type`='A',
    `sort`=9,`is_show`=1,`is_del`=0,`update_time`=@now
WHERE `perms`='admin:jk:agent-relation:force-adjust' AND @agent_relation_menu_id IS NOT NULL;

-- ============================================================================
-- 执行后核对
-- ============================================================================
SELECT `rule_code`,`version_no`,`parent_role_code`,`child_role_code`,`region_code`,
       `max_direct_children`,`overflow_policy`,`priority`,`status`,`is_deleted`
FROM `jk_relation_limit_rule`
ORDER BY `priority` DESC,`id` DESC;

SELECT `parent_user_id`,`used_count`,`reserved_count`,`version`
FROM `jk_relation_quota_usage`
ORDER BY `used_count` DESC,`parent_user_id` ASC;

SELECT `id`,`pid`,`name`,`perms`,`path`,`menu_type`,`is_show`,`is_del`
FROM `eb_system_menu`
WHERE `perms` IN (
  'admin:jk:relation:limit:rule:list',
  'admin:jk:relation:limit:rule:save',
  'admin:jk:relation:limit:rule:status',
  'admin:jk:agent-relation:force-adjust'
)
ORDER BY `pid`,`id`;
