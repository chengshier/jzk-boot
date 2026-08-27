-- 九州康 JZK V3.1 第二批：佣金、业绩、经营收益与线下销售闭环（MySQL 5.7）
-- 依赖第一批关系额度脚本；本脚本不启用任何佣金模板，不补发历史金额。

SET @now = NOW();

CREATE TABLE IF NOT EXISTS `jk_performance_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `performance_no` varchar(64) NOT NULL,
  `source_type` varchar(48) NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `source_no` varchar(64) DEFAULT NULL,
  `source_item_id` bigint DEFAULT NULL,
  `performance_type` varchar(48) NOT NULL,
  `owner_user_id` bigint NOT NULL,
  `owner_role_code` varchar(48) DEFAULT NULL,
  `source_user_id` bigint DEFAULT NULL,
  `source_role_code` varchar(48) DEFAULT NULL,
  `direct_parent_user_id` bigint DEFAULT NULL,
  `county_agent_user_id` bigint DEFAULT NULL,
  `region_code` varchar(64) DEFAULT NULL,
  `product_id` int DEFAULT NULL,
  `sku_id` int DEFAULT NULL,
  `quantity` int NOT NULL DEFAULT 0,
  `base_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `performance_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `reversed_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `status` varchar(32) NOT NULL DEFAULT 'VALID',
  `occurred_at` datetime NOT NULL,
  `request_no` varchar(96) NOT NULL,
  `plan_id` bigint DEFAULT NULL,
  `rule_version_no` int DEFAULT NULL,
  `relation_snapshot_json` longtext,
  `source_snapshot_json` longtext,
  `action_key` varchar(191) NOT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_performance_action` (`action_key`),
  KEY `idx_jk_performance_owner_time` (`owner_user_id`,`occurred_at`),
  KEY `idx_jk_performance_source` (`source_type`,`source_id`,`source_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康独立业绩账本';

CREATE TABLE IF NOT EXISTS `jk_operation_profit_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `profit_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `role_code` varchar(48) DEFAULT NULL,
  `income_nature` varchar(32) NOT NULL DEFAULT 'OFFLINE_REALIZED',
  `source_type` varchar(48) NOT NULL,
  `source_id` bigint DEFAULT NULL,
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
  `request_no` varchar(96) NOT NULL,
  `action_key` varchar(191) NOT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_profit_action` (`action_key`),
  KEY `idx_jk_profit_user_time` (`user_id`,`create_time`),
  KEY `idx_jk_profit_source` (`source_type`,`source_id`,`source_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下经营收益账本，不进入平台提现账户';

CREATE TABLE IF NOT EXISTS `jk_offline_sale` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_no` varchar(64) NOT NULL,
  `request_no` varchar(96) NOT NULL,
  `seller_user_id` bigint NOT NULL,
  `seller_role_code` varchar(48) NOT NULL,
  `county_agent_user_id` bigint DEFAULT NULL,
  `direct_parent_user_id` bigint DEFAULT NULL,
  `region_code` varchar(64) DEFAULT NULL,
  `customer_type` varchar(32) NOT NULL,
  `customer_user_id` bigint DEFAULT NULL,
  `customer_name_masked` varchar(128) DEFAULT NULL,
  `customer_phone_masked` varchar(32) DEFAULT NULL,
  `registered_customer` tinyint(1) NOT NULL DEFAULT 0,
  `total_quantity` int NOT NULL DEFAULT 0,
  `total_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `pay_method` varchar(32) DEFAULT NULL,
  `sale_time` datetime NOT NULL,
  `voucher_urls` text,
  `promotion_source` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING_CONFIRM',
  `audit_required` tinyint(1) NOT NULL DEFAULT 0,
  `audit_user_id` bigint DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `audit_remark` varchar(500) DEFAULT NULL,
  `cancel_reason` varchar(500) DEFAULT NULL,
  `relation_snapshot_json` longtext,
  `risk_snapshot_json` longtext,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_user_id` bigint DEFAULT NULL,
  `update_user_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `version` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_offline_sale_no` (`sale_no`),
  UNIQUE KEY `uk_jk_offline_sale_request` (`request_no`),
  KEY `idx_jk_offline_sale_seller` (`seller_user_id`,`sale_time`),
  KEY `idx_jk_offline_sale_status` (`status`,`audit_required`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创客合伙人区县代理线下终端销售单';

CREATE TABLE IF NOT EXISTS `jk_offline_sale_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_id` bigint NOT NULL,
  `product_id` int NOT NULL,
  `sku_id` int NOT NULL,
  `product_name` varchar(255) DEFAULT NULL,
  `sku_name` varchar(255) DEFAULT NULL,
  `sku_code` varchar(128) DEFAULT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(18,2) NOT NULL,
  `total_amount` decimal(18,2) NOT NULL,
  `unit_cost` decimal(18,2) DEFAULT NULL,
  `cost_amount` decimal(18,2) DEFAULT NULL,
  `profit_amount` decimal(18,2) DEFAULT NULL,
  `stock_account_id` bigint DEFAULT NULL,
  `cost_snapshot_json` longtext,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_jk_offline_sale_item_sale` (`sale_id`),
  KEY `idx_jk_offline_sale_item_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下销售商品明细';

CREATE TABLE IF NOT EXISTS `jk_offline_sale_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_id` bigint NOT NULL,
  `action` varchar(48) NOT NULL,
  `before_status` varchar(32) DEFAULT NULL,
  `after_status` varchar(32) DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `operator_type` varchar(24) NOT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `request_no` varchar(96) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_jk_offline_sale_log_sale` (`sale_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下销售审核与状态日志';

CREATE TABLE IF NOT EXISTS `jk_commission_match_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_key` varchar(191) NOT NULL,
  `scenario` varchar(64) NOT NULL,
  `source_type` varchar(48) NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `source_item_id` bigint DEFAULT NULL,
  `receiver_user_id` bigint DEFAULT NULL,
  `reward_type` varchar(64) DEFAULT NULL,
  `rule_id` bigint DEFAULT NULL,
  `rule_version_no` int DEFAULT NULL,
  `match_status` varchar(32) NOT NULL,
  `reason_code` varchar(64) DEFAULT NULL,
  `calculation_json` longtext,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_commission_match_event` (`event_key`),
  KEY `idx_jk_commission_match_source` (`source_type`,`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金规则匹配与未命中审计';

DROP PROCEDURE IF EXISTS `jk_v31_add_column`;
DELIMITER $$
CREATE PROCEDURE `jk_v31_add_column`(IN p_table varchar(64), IN p_column varchar(64), IN p_definition text)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND COLUMN_NAME=p_column) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
END$$
DELIMITER ;

CALL jk_v31_add_column('jk_stock_transfer_item','received_qty','int NOT NULL DEFAULT 0 COMMENT ''实际收货数量''');
CALL jk_v31_add_column('jk_stock_transfer_item','source_unit_cost','decimal(18,2) DEFAULT NULL COMMENT ''实际批次加权单位成本''');
CALL jk_v31_add_column('jk_stock_transfer_item','cost_amount','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_stock_transfer_item','unit_spread','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_stock_transfer_item','spread_amount','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_stock_transfer_item','cost_method','varchar(32) DEFAULT NULL');
CALL jk_v31_add_column('jk_stock_transfer_item','cost_snapshot_json','longtext');
CALL jk_v31_add_column('jk_stock_transfer_item','profit_status','varchar(32) DEFAULT NULL');

CALL jk_v31_add_column('jk_commission_rule','plan_id','bigint DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','version_no','int DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','rule_code','varchar(96) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','reward_type','varchar(64) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','performance_type','varchar(64) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','beneficiary_type','varchar(64) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','base_type','varchar(64) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','calculation_type','varchar(32) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','rate','decimal(18,6) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','fixed_amount','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','unit_amount','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','trigger_timing','varchar(48) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','settle_delay_days','int NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','stack_group','varchar(64) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','stack_policy','varchar(32) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','priority','int NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','per_order_cap','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','per_user_period_cap','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','total_budget','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','requires_registered_customer','tinyint(1) NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','requires_voucher','tinyint(1) NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','requires_audit','tinyint(1) NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','effective_start_time','datetime DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','effective_end_time','datetime DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','publish_status','varchar(32) NOT NULL DEFAULT ''DRAFT''');
CALL jk_v31_add_column('jk_commission_rule','published_by','bigint DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','published_at','datetime DEFAULT NULL');

CALL jk_v31_add_column('jk_commission_record','source_item_id','bigint DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_record','reward_type','varchar(64) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_record','rule_version_no','int DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_record','income_nature','varchar(32) NOT NULL DEFAULT ''PLATFORM_PAYABLE''');
CALL jk_v31_add_column('jk_commission_record','reversed_amount','decimal(18,2) NOT NULL DEFAULT 0.00');
CALL jk_v31_add_column('jk_commission_record','beneficiary_snapshot_json','longtext');
CALL jk_v31_add_column('jk_commission_record','calculation_snapshot_json','longtext');
CALL jk_v31_add_column('jk_commission_record','commission_action_key','varchar(191) DEFAULT NULL');

DROP PROCEDURE IF EXISTS `jk_v31_add_column`;

-- 所有新模板保持关闭；历史启用规则不自动迁移为 PUBLISHED。
UPDATE jk_commission_rule
SET publish_status = CASE WHEN publish_status IS NULL OR publish_status='' THEN 'PENDING_CONFIRMATION' ELSE publish_status END,
    update_time = @now
WHERE is_deleted=0;

-- 执行后核对
SELECT COUNT(*) AS performance_count FROM jk_performance_record;
SELECT COUNT(*) AS profit_count FROM jk_operation_profit_record;
SELECT rule_no,rule_name,status,publish_status,effective_start_time,effective_end_time
FROM jk_commission_rule ORDER BY id DESC;
