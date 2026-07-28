-- 九州康 JZK V3.1 第二批：佣金、业绩、团队和线下经营闭环（MySQL 5.7）
-- 执行前必须备份相关业务表。本脚本只建立底座和默认关闭模板，不自动补发历史佣金。
SET @now = NOW();

CREATE TABLE IF NOT EXISTS `jk_performance_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `performance_no` varchar(64) NOT NULL,
  `source_type` varchar(40) NOT NULL,
  `source_id` bigint NOT NULL,
  `source_no` varchar(64) DEFAULT NULL,
  `source_item_id` bigint DEFAULT NULL,
  `performance_type` varchar(40) NOT NULL,
  `owner_user_id` bigint NOT NULL,
  `owner_role_code` varchar(40) DEFAULT NULL,
  `source_user_id` bigint DEFAULT NULL,
  `source_role_code` varchar(40) DEFAULT NULL,
  `direct_parent_user_id` bigint DEFAULT NULL,
  `county_agent_user_id` bigint DEFAULT NULL,
  `region_code` varchar(40) DEFAULT NULL,
  `product_id` int DEFAULT NULL,
  `sku_id` int DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT 0,
  `base_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `performance_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `reversed_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `status` varchar(32) NOT NULL DEFAULT 'VALID',
  `occurred_at` datetime NOT NULL,
  `request_no` varchar(80) NOT NULL,
  `plan_id` bigint DEFAULT NULL,
  `rule_version_no` int DEFAULT NULL,
  `relation_snapshot_json` longtext,
  `source_snapshot_json` longtext,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_performance_request` (`request_no`),
  KEY `idx_jk_performance_owner_time` (`owner_user_id`,`occurred_at`),
  KEY `idx_jk_performance_source` (`source_type`,`source_id`,`source_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康独立业绩账本';

CREATE TABLE IF NOT EXISTS `jk_operation_profit_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `profit_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `role_code` varchar(40) DEFAULT NULL,
  `income_nature` varchar(32) NOT NULL DEFAULT 'OFFLINE_REALIZED',
  `source_type` varchar(40) NOT NULL,
  `source_id` bigint NOT NULL,
  `source_no` varchar(64) DEFAULT NULL,
  `source_item_id` bigint DEFAULT NULL,
  `product_id` int DEFAULT NULL,
  `sku_id` int DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT 0,
  `revenue_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `cost_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `profit_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `reversed_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `status` varchar(32) NOT NULL DEFAULT 'CONFIRMED',
  `cost_snapshot_json` longtext,
  `relation_snapshot_json` longtext,
  `request_no` varchar(80) NOT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_profit_request` (`request_no`),
  KEY `idx_jk_profit_user_time` (`user_id`,`create_time`),
  KEY `idx_jk_profit_source` (`source_type`,`source_id`,`source_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康线下经营收益账本';

CREATE TABLE IF NOT EXISTS `jk_offline_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_no` varchar(64) NOT NULL,
  `request_no` varchar(80) NOT NULL,
  `seller_user_id` bigint NOT NULL,
  `seller_role_code` varchar(40) NOT NULL,
  `county_agent_user_id` bigint DEFAULT NULL,
  `region_code` varchar(40) DEFAULT NULL,
  `customer_type` varchar(32) NOT NULL,
  `customer_user_id` bigint DEFAULT NULL,
  `customer_name_masked` varchar(80) DEFAULT NULL,
  `customer_phone_masked` varchar(32) DEFAULT NULL,
  `registered_customer` tinyint(1) NOT NULL DEFAULT 0,
  `payment_method` varchar(32) DEFAULT NULL,
  `voucher_url` varchar(500) DEFAULT NULL,
  `promotion_source` varchar(100) DEFAULT NULL,
  `sale_time` datetime NOT NULL,
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `total_cost_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `total_profit_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `audit_required` tinyint(1) NOT NULL DEFAULT 0,
  `audit_status` varchar(32) NOT NULL DEFAULT 'NOT_REQUIRED',
  `status` varchar(32) NOT NULL DEFAULT 'CONFIRMED',
  `relation_snapshot_json` longtext,
  `source_snapshot_json` longtext,
  `cancel_reason` varchar(500) DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_user_id` bigint DEFAULT NULL,
  `update_user_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_offline_sale_no` (`sale_no`),
  UNIQUE KEY `uk_jk_offline_sale_request` (`request_no`),
  KEY `idx_jk_offline_sale_seller_time` (`seller_user_id`,`sale_time`),
  KEY `idx_jk_offline_sale_status` (`status`,`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康线下终端销售单';

CREATE TABLE IF NOT EXISTS `jk_offline_sale_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_id` bigint NOT NULL,
  `product_id` int NOT NULL,
  `sku_id` int DEFAULT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  `sku_name` varchar(255) DEFAULT NULL,
  `sku_code` varchar(100) DEFAULT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(18,2) NOT NULL,
  `total_amount` decimal(18,2) NOT NULL,
  `unit_cost` decimal(18,2) DEFAULT NULL,
  `cost_amount` decimal(18,2) DEFAULT NULL,
  `profit_amount` decimal(18,2) DEFAULT NULL,
  `cost_method` varchar(32) DEFAULT 'FIFO_BATCH',
  `cost_snapshot_json` longtext,
  `returned_qty` int NOT NULL DEFAULT 0,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_jk_offline_sale_item_sale` (`sale_id`),
  KEY `idx_jk_offline_sale_item_product` (`product_id`,`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康线下销售明细';

CREATE TABLE IF NOT EXISTS `jk_offline_sale_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_id` bigint NOT NULL,
  `action` varchar(40) NOT NULL,
  `before_status` varchar(32) DEFAULT NULL,
  `after_status` varchar(32) DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `operator_type` varchar(20) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `snapshot_json` longtext,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_jk_offline_sale_audit` (`sale_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康线下销售审核日志';

-- 调拨实际收货、批次成本和价差快照。
ALTER TABLE `jk_stock_transfer_item`
  ADD COLUMN `received_qty` int NOT NULL DEFAULT 0 COMMENT '实际收货数量',
  ADD COLUMN `source_unit_cost` decimal(18,2) DEFAULT NULL COMMENT '来源批次加权单位成本',
  ADD COLUMN `cost_amount` decimal(18,2) DEFAULT NULL COMMENT '实际成本',
  ADD COLUMN `unit_spread` decimal(18,2) DEFAULT NULL COMMENT '单位价差',
  ADD COLUMN `spread_amount` decimal(18,2) DEFAULT NULL COMMENT '价差金额',
  ADD COLUMN `cost_method` varchar(32) DEFAULT NULL COMMENT '成本方法',
  ADD COLUMN `cost_snapshot_json` longtext COMMENT '批次成本快照',
  ADD COLUMN `profit_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '经营收益状态';

-- 佣金规则 V3.1 扩展。status 继续作为是否有效的布尔字段，publish_status 记录草稿/审核/发布状态。
ALTER TABLE `jk_commission_rule`
  ADD COLUMN `plan_id` bigint DEFAULT NULL,
  ADD COLUMN `rule_code` varchar(80) DEFAULT NULL,
  ADD COLUMN `reward_type` varchar(60) DEFAULT NULL,
  ADD COLUMN `performance_type` varchar(60) DEFAULT NULL,
  ADD COLUMN `beneficiary_type` varchar(60) DEFAULT NULL,
  ADD COLUMN `base_type` varchar(60) DEFAULT NULL,
  ADD COLUMN `calculation_type` varchar(40) DEFAULT NULL,
  ADD COLUMN `rate` decimal(18,6) DEFAULT NULL,
  ADD COLUMN `fixed_amount` decimal(18,2) DEFAULT NULL,
  ADD COLUMN `unit_amount` decimal(18,2) DEFAULT NULL,
  ADD COLUMN `trigger_timing` varchar(40) DEFAULT NULL,
  ADD COLUMN `settle_delay_days` int NOT NULL DEFAULT 0,
  ADD COLUMN `stack_group` varchar(80) DEFAULT NULL,
  ADD COLUMN `stack_policy` varchar(40) NOT NULL DEFAULT 'MAX_ONE',
  ADD COLUMN `priority` int NOT NULL DEFAULT 0,
  ADD COLUMN `per_order_cap` decimal(18,2) DEFAULT NULL,
  ADD COLUMN `per_user_period_cap` decimal(18,2) DEFAULT NULL,
  ADD COLUMN `total_budget` decimal(18,2) DEFAULT NULL,
  ADD COLUMN `requires_registered_customer` tinyint(1) NOT NULL DEFAULT 0,
  ADD COLUMN `requires_voucher` tinyint(1) NOT NULL DEFAULT 0,
  ADD COLUMN `requires_audit` tinyint(1) NOT NULL DEFAULT 0,
  ADD COLUMN `publish_status` varchar(32) NOT NULL DEFAULT 'DRAFT',
  ADD COLUMN `published_by` bigint DEFAULT NULL,
  ADD COLUMN `published_time` datetime DEFAULT NULL;

ALTER TABLE `jk_commission_record`
  ADD COLUMN `source_item_id` bigint DEFAULT NULL,
  ADD COLUMN `reward_type` varchar(60) DEFAULT NULL,
  ADD COLUMN `rule_version_no` int DEFAULT NULL,
  ADD COLUMN `commission_action_key` varchar(255) DEFAULT NULL,
  ADD COLUMN `income_nature` varchar(32) NOT NULL DEFAULT 'PLATFORM_PAYABLE',
  ADD COLUMN `relation_snapshot_json` longtext,
  ADD COLUMN `source_snapshot_json` longtext,
  ADD COLUMN `calculation_snapshot_json` longtext,
  ADD COLUMN `reversed_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN `negative_offset_amount` decimal(18,2) NOT NULL DEFAULT 0.00;

-- 对新环境提供唯一键；旧环境执行前先审计重复值。
SET @has_commission_action_key = (
 SELECT COUNT(1) FROM information_schema.statistics
 WHERE table_schema=DATABASE() AND table_name='jk_commission_record' AND index_name='uk_jk_commission_action_key'
);
SET @sql = IF(@has_commission_action_key=0,
 'ALTER TABLE jk_commission_record ADD UNIQUE KEY uk_jk_commission_action_key (commission_action_key)',
 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 默认关闭模板补全。没有金额、比例和生效时间，不能产生佣金。
UPDATE `jk_commission_rule`
SET `status`=0,
    `publish_status`='DRAFT',
    `effective_time`=NULL,
    `expire_time`=NULL,
    `rate`=NULL,
    `fixed_amount`=NULL,
    `unit_amount`=NULL,
    `update_time`=@now
WHERE `rule_no` LIKE 'V31-%';

SELECT 'V3.1 phase2 schema ready; commission templates remain disabled' AS result;
