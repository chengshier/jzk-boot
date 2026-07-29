-- 九州康 JZK V3.1 第三批补丁：异常收货 V2 分 SKU 处理明细（MySQL 5.7）
CREATE TABLE IF NOT EXISTS `jk_receive_exception_resolution_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `resolution_id` bigint NOT NULL,
  `exception_item_id` bigint NOT NULL,
  `business_item_id` bigint DEFAULT NULL,
  `product_id` int NOT NULL,
  `sku_id` int DEFAULT NULL,
  `accepted_qty` int NOT NULL DEFAULT 0,
  `reship_qty` int NOT NULL DEFAULT 0,
  `refund_qty` int NOT NULL DEFAULT 0,
  `return_qty` int NOT NULL DEFAULT 0,
  `logistics_company` varchar(100) DEFAULT NULL,
  `logistics_no` varchar(100) DEFAULT NULL,
  `item_remark` varchar(500) DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_jk_receive_resolution_item` (`resolution_id`,`exception_item_id`),
  KEY `idx_jk_receive_resolution_item_exception` (`exception_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常收货V2分SKU处理明细';
