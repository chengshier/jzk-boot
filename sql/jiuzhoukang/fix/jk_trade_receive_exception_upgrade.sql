-- 九州康订货/调拨异常收货升级（MySQL 5.7）
--
-- V1 处理原则：
-- 1. 收货人上报数量短缺、破损或其他异常后，原业务单进入 RECEIVE_EXCEPTION；
-- 2. 异常期间不执行任何库存入库、佣金触发或金额调整；
-- 3. 后台完成补发、核对或线下处理后，将业务单恢复为 SHIPPED / TRANSFERRED；
-- 4. 收货人重新核对并执行正常全量收货。
--
-- 这样可以在尚未建立索赔、补发、差额结算模型前，避免错误库存和错误业绩。
-- 执行前请先备份数据库。

CREATE TABLE IF NOT EXISTS `jk_trade_receive_exception` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exception_no` varchar(64) NOT NULL COMMENT '异常单号',
  `request_no` varchar(64) NOT NULL COMMENT '客户端幂等号',
  `business_type` varchar(32) NOT NULL COMMENT '业务类型：PLATFORM_ORDER/STOCK_TRANSFER',
  `business_id` bigint(20) NOT NULL COMMENT '业务单ID',
  `business_no` varchar(64) NOT NULL COMMENT '业务单号快照',
  `receiver_user_id` bigint(20) NOT NULL COMMENT '收货人用户ID',
  `status` varchar(32) NOT NULL COMMENT '状态：PENDING/PROCESSING/RESOLVED/REJECTED',
  `exception_type` varchar(32) NOT NULL COMMENT '异常类型：SHORTAGE/DAMAGED/MIXED/OTHER',
  `expected_total_qty` int(11) NOT NULL DEFAULT '0' COMMENT '应收总数量',
  `received_total_qty` int(11) NOT NULL DEFAULT '0' COMMENT '实收总数量',
  `shortage_total_qty` int(11) NOT NULL DEFAULT '0' COMMENT '短缺总数量',
  `damaged_total_qty` int(11) NOT NULL DEFAULT '0' COMMENT '破损总数量',
  `exception_reason` varchar(1000) NOT NULL COMMENT '异常原因',
  `evidence_json` text COMMENT '异常凭证URL数组JSON',
  `handle_action` varchar(32) DEFAULT NULL COMMENT '最后处理动作',
  `handle_remark` varchar(1000) DEFAULT NULL COMMENT '处理说明',
  `handle_user_id` bigint(20) DEFAULT NULL COMMENT '处理人业务用户ID',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '软删除',
  `create_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
  `update_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_receive_exception_no` (`exception_no`),
  UNIQUE KEY `uk_jk_receive_request_no` (`request_no`),
  KEY `idx_jk_receive_business` (`business_type`,`business_id`,`status`,`is_deleted`),
  KEY `idx_jk_receive_user_status` (`receiver_user_id`,`status`,`is_deleted`),
  KEY `idx_jk_receive_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康订货调拨异常收货';

CREATE TABLE IF NOT EXISTS `jk_trade_receive_exception_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `exception_id` bigint(20) NOT NULL COMMENT '异常收货主记录ID',
  `business_item_id` bigint(20) NOT NULL COMMENT '原业务商品明细ID',
  `product_id` int(11) DEFAULT NULL COMMENT '商品ID快照',
  `sku_id` int(11) DEFAULT NULL COMMENT 'SKU ID快照',
  `product_name` varchar(255) NOT NULL COMMENT '商品名称快照',
  `sku_name` varchar(255) DEFAULT NULL COMMENT 'SKU名称快照',
  `sku_code` varchar(128) DEFAULT NULL COMMENT 'SKU编码快照',
  `expected_qty` int(11) NOT NULL DEFAULT '0' COMMENT '应收数量',
  `received_qty` int(11) NOT NULL DEFAULT '0' COMMENT '实收数量',
  `damaged_qty` int(11) NOT NULL DEFAULT '0' COMMENT '破损数量',
  `shortage_qty` int(11) NOT NULL DEFAULT '0' COMMENT '短缺数量',
  `item_remark` varchar(500) DEFAULT NULL COMMENT '商品差异说明',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '软删除',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_receive_exception_item` (`exception_id`,`business_item_id`,`is_deleted`),
  KEY `idx_jk_receive_exception_id` (`exception_id`),
  KEY `idx_jk_receive_product_sku` (`product_id`,`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康异常收货商品差异明细';

-- 一致性审计一：业务单进入异常状态，但不存在待处理/处理中异常记录。
SELECT 'PLATFORM_ORDER' AS business_type, o.id AS business_id, o.platform_order_no AS business_no
FROM jk_platform_order o
LEFT JOIN jk_trade_receive_exception e
  ON e.business_type = 'PLATFORM_ORDER'
 AND e.business_id = o.id
 AND e.status IN ('PENDING','PROCESSING')
 AND e.is_deleted = 0
WHERE o.status = 'RECEIVE_EXCEPTION'
  AND o.is_deleted = 0
  AND e.id IS NULL
UNION ALL
SELECT 'STOCK_TRANSFER', t.id, t.transfer_no
FROM jk_stock_transfer t
LEFT JOIN jk_trade_receive_exception e
  ON e.business_type = 'STOCK_TRANSFER'
 AND e.business_id = t.id
 AND e.status IN ('PENDING','PROCESSING')
 AND e.is_deleted = 0
WHERE t.status = 'RECEIVE_EXCEPTION'
  AND t.is_deleted = 0
  AND e.id IS NULL;

-- 一致性审计二：异常记录仍在处理中，但原业务单已不处于异常状态。
SELECT e.id, e.exception_no, e.business_type, e.business_id, e.status
FROM jk_trade_receive_exception e
LEFT JOIN jk_platform_order o
  ON e.business_type = 'PLATFORM_ORDER' AND o.id = e.business_id AND o.is_deleted = 0
LEFT JOIN jk_stock_transfer t
  ON e.business_type = 'STOCK_TRANSFER' AND t.id = e.business_id AND t.is_deleted = 0
WHERE e.status IN ('PENDING','PROCESSING')
  AND e.is_deleted = 0
  AND ((e.business_type = 'PLATFORM_ORDER' AND (o.id IS NULL OR o.status <> 'RECEIVE_EXCEPTION'))
    OR (e.business_type = 'STOCK_TRANSFER' AND (t.id IS NULL OR t.status <> 'RECEIVE_EXCEPTION')));

-- 一致性审计三：异常明细数量不合法。
SELECT i.*
FROM jk_trade_receive_exception_item i
WHERE i.is_deleted = 0
  AND (i.expected_qty < 0
    OR i.received_qty < 0
    OR i.damaged_qty < 0
    OR i.shortage_qty < 0
    OR i.received_qty > i.expected_qty
    OR i.damaged_qty > i.received_qty
    OR i.shortage_qty <> i.expected_qty - i.received_qty);
