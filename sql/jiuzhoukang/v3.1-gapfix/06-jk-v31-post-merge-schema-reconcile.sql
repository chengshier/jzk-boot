-- JZK V3.1 Post-Merge Hardening：修复 Phase2/Phase3 合并后模型与真实表结构漂移（MySQL 5.7）
-- 仅补齐最终业务模型已使用的字段；不启用佣金模板，不重算历史佣金，不修改归属快照。

DROP PROCEDURE IF EXISTS jk_v31_reconcile_add_column;
DELIMITER $$
CREATE PROCEDURE jk_v31_reconcile_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=p_table)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=p_table AND column_name=p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 库存盘点：以 jk_v31_phase3_operations_health.sql 的冻结快照模型为最终结构。
CALL jk_v31_reconcile_add_column('jk_stock_check','scope_type',"varchar(32) NOT NULL DEFAULT 'ACCOUNT'");
CALL jk_v31_reconcile_add_column('jk_stock_check','freeze_status',"varchar(32) NOT NULL DEFAULT 'NOT_FROZEN'");
CALL jk_v31_reconcile_add_column('jk_stock_check','book_total_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_reconcile_add_column('jk_stock_check','actual_total_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_reconcile_add_column('jk_stock_check','profit_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_reconcile_add_column('jk_stock_check','loss_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_reconcile_add_column('jk_stock_check','adjust_action_key','varchar(255) DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_stock_check','completed_time','datetime DEFAULT NULL');

CALL jk_v31_reconcile_add_column('jk_stock_check_item','stock_item_id','bigint DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_stock_check_item','book_available_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_reconcile_add_column('jk_stock_check_item','book_frozen_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_reconcile_add_column('jk_stock_check_item','actual_available_qty','int DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_stock_check_item','difference_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_reconcile_add_column('jk_stock_check_item','difference_type',"varchar(20) NOT NULL DEFAULT 'NONE'");
CALL jk_v31_reconcile_add_column('jk_stock_check_item','remark','varchar(500) DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_stock_check_item','version_snapshot','int DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_stock_check_item','adjusted','tinyint(1) NOT NULL DEFAULT 0');

-- 推广码：随机 scene 映射到推广人快照，禁止把 userId 直接放进二维码 scene。
CALL jk_v31_reconcile_add_column('jk_promotion_scene','promoter_user_id','bigint DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_promotion_scene','promoter_role_code','varchar(40) DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_promotion_scene','region_code','varchar(40) DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_promotion_scene','scene_value','varchar(64) DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_promotion_scene','object_key','varchar(500) DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_promotion_scene','disabled_reason','varchar(500) DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_promotion_scene','expire_time','datetime DEFAULT NULL');
-- 历史版本可能把 status 建成 tinyint；最终 V3.1 使用 ACTIVE/DISABLED 字符串状态。
SET @promotion_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='jk_promotion_scene');
SET @ddl = IF(@promotion_exists>0,
  "ALTER TABLE jk_promotion_scene MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'ACTIVE'",
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE jk_promotion_scene
SET status = CASE
    WHEN status IN ('1','true','TRUE') THEN 'ACTIVE'
    WHEN status IN ('0','false','FALSE') THEN 'DISABLED'
    ELSE status
END
WHERE @promotion_exists>0;

-- 佣金：只补不可变快照、负向抵扣和正式 published_at；所有模板继续保持原开关状态。
CALL jk_v31_reconcile_add_column('jk_commission_rule','published_at','datetime DEFAULT NULL');
CALL jk_v31_reconcile_add_column('jk_commission_record','negative_offset_amount','decimal(18,2) NOT NULL DEFAULT 0.00');
CALL jk_v31_reconcile_add_column('jk_commission_record','relation_snapshot_json','longtext');
CALL jk_v31_reconcile_add_column('jk_commission_record','source_snapshot_json','longtext');

DROP PROCEDURE IF EXISTS jk_v31_reconcile_add_column;

SELECT 'JZK V3.1 post-merge schema reconcile ready; commission templates were not enabled' AS result;
