-- 九州康 JZK V3.1 第三批：运营和非厂商健康能力（MySQL 5.7）
SET @now = NOW();

CREATE TABLE IF NOT EXISTS `jk_stock_check` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_no` varchar(64) NOT NULL,
  `request_no` varchar(80) NOT NULL,
  `stock_account_id` bigint NOT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `scope_type` varchar(32) NOT NULL DEFAULT 'ACCOUNT',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT',
  `freeze_status` varchar(32) NOT NULL DEFAULT 'NOT_FROZEN',
  `book_total_qty` int NOT NULL DEFAULT 0,
  `actual_total_qty` int NOT NULL DEFAULT 0,
  `profit_qty` int NOT NULL DEFAULT 0,
  `loss_qty` int NOT NULL DEFAULT 0,
  `audit_user_id` bigint DEFAULT NULL,
  `audit_time` datetime DEFAULT NULL,
  `audit_remark` varchar(500) DEFAULT NULL,
  `adjust_action_key` varchar(255) DEFAULT NULL,
  `completed_time` datetime DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_user_id` bigint DEFAULT NULL,
  `update_user_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_stock_check_no` (`check_no`),
  UNIQUE KEY `uk_jk_stock_check_request` (`request_no`),
  UNIQUE KEY `uk_jk_stock_check_adjust` (`adjust_action_key`),
  KEY `idx_jk_stock_check_account_status` (`stock_account_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康库存盘点单';

CREATE TABLE IF NOT EXISTS `jk_stock_check_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL,
  `stock_item_id` bigint NOT NULL,
  `product_id` int NOT NULL,
  `sku_id` int DEFAULT NULL,
  `sku_code` varchar(100) DEFAULT NULL,
  `book_available_qty` int NOT NULL DEFAULT 0,
  `book_frozen_qty` int NOT NULL DEFAULT 0,
  `actual_available_qty` int DEFAULT NULL,
  `difference_qty` int NOT NULL DEFAULT 0,
  `difference_type` varchar(20) NOT NULL DEFAULT 'NONE',
  `remark` varchar(500) DEFAULT NULL,
  `version_snapshot` int DEFAULT NULL,
  `adjusted` tinyint(1) NOT NULL DEFAULT 0,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_stock_check_item` (`check_id`,`stock_item_id`),
  KEY `idx_jk_stock_check_item_product` (`product_id`,`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康库存盘点明细';

CREATE TABLE IF NOT EXISTS `jk_stock_check_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_id` bigint NOT NULL,
  `action` varchar(40) NOT NULL,
  `before_status` varchar(32) DEFAULT NULL,
  `after_status` varchar(32) DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `snapshot_json` longtext,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_jk_stock_check_audit` (`check_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康库存盘点审核日志';

CREATE TABLE IF NOT EXISTS `jk_report_export_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) NOT NULL,
  `report_type` varchar(60) NOT NULL,
  `request_no` varchar(80) NOT NULL,
  `request_json` longtext,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `storage_provider` varchar(32) NOT NULL DEFAULT 'MINIO',
  `object_key` varchar(500) DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `content_type` varchar(100) DEFAULT NULL,
  `download_count` int NOT NULL DEFAULT 0,
  `expire_time` datetime DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `completed_time` datetime DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_export_task_no` (`task_no`),
  UNIQUE KEY `uk_jk_export_request` (`request_no`),
  KEY `idx_jk_export_status` (`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康异步报表导出任务';

CREATE TABLE IF NOT EXISTS `jk_promotion_scene` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene_code` varchar(64) NOT NULL,
  `promoter_user_id` bigint NOT NULL,
  `promoter_role_code` varchar(40) DEFAULT NULL,
  `region_code` varchar(40) DEFAULT NULL,
  `page_path` varchar(255) NOT NULL,
  `scene_value` varchar(64) NOT NULL,
  `object_key` varchar(500) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `disabled_reason` varchar(500) DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_promotion_scene_code` (`scene_code`),
  UNIQUE KEY `uk_jk_promotion_scene_value` (`scene_value`),
  KEY `idx_jk_promotion_promoter` (`promoter_user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康微信小程序码场景映射';

CREATE TABLE IF NOT EXISTS `jk_promotion_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene_id` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `scan_count` int NOT NULL DEFAULT 0,
  `new_user_count` int NOT NULL DEFAULT 0,
  `initial_bind_count` int NOT NULL DEFAULT 0,
  `effective_bind_count` int NOT NULL DEFAULT 0,
  `buyer_count` int NOT NULL DEFAULT 0,
  `sale_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_promotion_stat_day` (`scene_id`,`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康推广效果日统计';

CREATE TABLE IF NOT EXISTS `jk_subscription_message_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) NOT NULL,
  `event_type` varchar(60) NOT NULL,
  `event_key` varchar(160) NOT NULL,
  `receiver_user_id` bigint NOT NULL,
  `openid` varchar(100) DEFAULT NULL,
  `template_code` varchar(80) DEFAULT NULL,
  `template_id` varchar(120) DEFAULT NULL,
  `page_path` varchar(255) DEFAULT NULL,
  `payload_json` longtext,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `retry_count` int NOT NULL DEFAULT 0,
  `max_retry_count` int NOT NULL DEFAULT 5,
  `next_retry_time` datetime DEFAULT NULL,
  `last_error` varchar(1000) DEFAULT NULL,
  `enabled_snapshot` tinyint(1) NOT NULL DEFAULT 0,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_subscription_event_receiver` (`event_key`,`receiver_user_id`),
  KEY `idx_jk_subscription_status` (`status`,`next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康微信订阅消息任务';

CREATE TABLE IF NOT EXISTS `jk_subscription_message_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `attempt_no` int NOT NULL,
  `request_json` longtext,
  `response_json` longtext,
  `status` varchar(32) NOT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_jk_subscription_log_task` (`task_id`,`attempt_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康微信订阅消息发送日志';

-- 异常收货 V2 扩展；处理结果决定最终入库数量、退款和赔付，不直接静默修改原单。
ALTER TABLE `jk_trade_receive_exception`
  ADD COLUMN `resolution_type` varchar(40) DEFAULT NULL COMMENT '补发/退款/赔付/差额调整/部分收货/拒收',
  ADD COLUMN `normal_received_qty` int NOT NULL DEFAULT 0,
  ADD COLUMN `exception_qty` int NOT NULL DEFAULT 0,
  ADD COLUMN `refund_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN `compensation_amount` decimal(18,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN `receiver_confirm_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  ADD COLUMN `sender_confirm_status` varchar(32) NOT NULL DEFAULT 'PENDING',
  ADD COLUMN `resolution_snapshot_json` longtext;

-- 默认关闭外部依赖。
INSERT INTO `eb_system_config` (`name`,`title`,`value`,`status`,`form_type`,`create_time`,`update_time`)
SELECT 'jk_subscription_message_enabled','九州康订阅消息总开关','false',1,'text',@now,@now
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name`='jk_subscription_message_enabled');

INSERT INTO `eb_system_config` (`name`,`title`,`value`,`status`,`form_type`,`create_time`,`update_time`)
SELECT 'jk_wechat_miniprogram_code_enabled','九州康微信小程序码总开关','false',1,'text',@now,@now
WHERE NOT EXISTS (SELECT 1 FROM `eb_system_config` WHERE `name`='jk_wechat_miniprogram_code_enabled');

SELECT 'V3.1 phase3 schema ready; external message and mini-program-code switches remain disabled' AS result;
