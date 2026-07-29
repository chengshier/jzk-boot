-- 九州康 JZK V3.1 补漏结构升级（MySQL 5.7）
-- 执行前必须先运行 00-jk-v31-schema-precheck.sql。

DELIMITER $$
DROP PROCEDURE IF EXISTS jk_v31_add_column_if_missing$$
CREATE PROCEDURE jk_v31_add_column_if_missing(
    IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL jk_v31_add_column_if_missing('eb_user','jk_region_code',"varchar(64) DEFAULT NULL COMMENT '九州康用户标准所属区域编码'");
CALL jk_v31_add_column_if_missing('eb_user','jk_region_source',"varchar(32) DEFAULT NULL COMMENT 'USER_PROFILE/ADMIN/ORDER_ADDRESS_INITIALIZED/MIGRATION'");
CALL jk_v31_add_column_if_missing('eb_user','jk_region_update_time',"datetime DEFAULT NULL COMMENT '九州康所属区域更新时间'");

CALL jk_v31_add_column_if_missing('eb_store_order','jk_shipping_address_id',"int DEFAULT NULL COMMENT '本单收货地址ID快照'");
CALL jk_v31_add_column_if_missing('eb_store_order','jk_shipping_region_code',"varchar(64) DEFAULT NULL COMMENT '本单标准收货区域编码快照，仅用于当前订单归属兜底'");

CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','attribution_no',"varchar(64) DEFAULT NULL COMMENT '归属快照编号'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','product_id',"bigint DEFAULT NULL COMMENT '商品ID快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','sku_id',"bigint DEFAULT NULL COMMENT 'SKU/规格值ID快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','quantity',"int DEFAULT NULL COMMENT '购买数量快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','direct_parent_role_code',"varchar(64) DEFAULT NULL COMMENT '直属上级角色快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','profile_region_code',"varchar(64) DEFAULT NULL COMMENT '下单时个人资料区域'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','shipping_region_code',"varchar(64) DEFAULT NULL COMMENT '本单标准收货区域'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','final_region_code',"varchar(64) DEFAULT NULL COMMENT '本单最终归属区域'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','final_region_name_snapshot',"varchar(255) DEFAULT NULL COMMENT '最终区域名称快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','region_source_type',"varchar(32) DEFAULT NULL COMMENT 'RELATION/USER_PROFILE/ORDER_ADDRESS_FALLBACK/PLATFORM_DEFAULT/MANUAL_RESOLVED'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','shipping_address_id',"bigint DEFAULT NULL COMMENT '本单收货地址ID快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','freight_allocated_amount',"decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '运费分摊，仅展示且不参与佣金'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','refund_amount',"decimal(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计退款金额兼容列'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','relation_snapshot_json',"longtext COMMENT '关系快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','profile_snapshot_json',"longtext COMMENT '个人资料区域快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','shipping_address_snapshot_json',"longtext COMMENT '本单收货地址快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','region_resolution_snapshot_json',"longtext COMMENT '区域候选与采用原因快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','price_snapshot_json',"longtext COMMENT '价格和实付分摊快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','rule_context_snapshot_json',"longtext COMMENT '下单时规则上下文快照'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','attribution_status',"varchar(32) NOT NULL DEFAULT 'RESOLVED' COMMENT 'RESOLVED/PENDING_MANUAL/CONFLICT/LOCKED/REVERSED'");
CALL jk_v31_add_column_if_missing('jk_retail_order_attribution','lock_status',"varchar(32) NOT NULL DEFAULT 'UNLOCKED' COMMENT 'UNLOCKED/LOCKED'");

CREATE TABLE IF NOT EXISTS jk_retail_order_attribution_adjustment (
    id bigint NOT NULL AUTO_INCREMENT,
    attribution_id bigint NOT NULL,
    before_snapshot_json longtext,
    after_snapshot_json longtext,
    adjust_reason varchar(500) NOT NULL,
    adjust_type varchar(64) NOT NULL,
    operator_user_id bigint NOT NULL,
    audit_user_id bigint DEFAULT NULL,
    status varchar(32) NOT NULL COMMENT 'APPLIED/PENDING_AUDIT/PENDING_COMPENSATION/REJECTED',
    request_no varchar(64) NOT NULL,
    create_time datetime NOT NULL,
    update_time datetime DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jk_retail_attr_adjust_request (request_no),
    KEY idx_jk_retail_attr_adjust_attr (attribution_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='零售订单归属调整记录；锁定后只允许补偿和冲正，不覆盖原快照';

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='jk_retail_order_attribution' AND INDEX_NAME='uk_jk_retail_attr_order_info');
SET @idx_sql = IF(@idx_exists=0, 'ALTER TABLE jk_retail_order_attribution ADD UNIQUE KEY uk_jk_retail_attr_order_info(order_info_id)', 'SELECT 1');
PREPARE idx_stmt FROM @idx_sql; EXECUTE idx_stmt; DEALLOCATE PREPARE idx_stmt;

DROP PROCEDURE IF EXISTS jk_v31_add_column_if_missing;

-- 兼容旧数据：只补兼容列，不重算当前关系，不锁定历史空数据。
UPDATE jk_retail_order_attribution
SET final_region_code = COALESCE(final_region_code, region_code),
    region_source_type = COALESCE(region_source_type,
        CASE attribution_type WHEN 'DIRECT_PARENT' THEN 'RELATION' WHEN 'REGION_AGENT' THEN 'USER_PROFILE' ELSE 'PLATFORM_DEFAULT' END),
    refund_amount = COALESCE(refund_amount, refunded_amount, 0),
    attribution_status = COALESCE(NULLIF(attribution_status,''), 'RESOLVED'),
    lock_status = COALESCE(NULLIF(lock_status,''), 'UNLOCKED')
WHERE is_deleted = 0;
