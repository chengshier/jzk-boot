-- 九州康 JZK V3.1 第二、第三批可重复执行收口脚本（MySQL 5.7）
-- 前提：已执行本分支 phase2 / phase3 建表脚本，或当前库已包含前六阶段基础表。
-- 本脚本负责：字段兼容、旧表升级、佣金模板修正、业务权限、后台菜单和 authority。
SET @now = NOW();

DROP PROCEDURE IF EXISTS `jk_v31_add_column`;
DELIMITER $$
CREATE PROCEDURE `jk_v31_add_column`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- 第二批字段
CALL jk_v31_add_column('jk_stock_transfer_item','received_qty','int NOT NULL DEFAULT 0 COMMENT ''实际收货数量''');
CALL jk_v31_add_column('jk_stock_transfer_item','source_unit_cost','decimal(18,2) DEFAULT NULL COMMENT ''来源批次加权单位成本''');
CALL jk_v31_add_column('jk_stock_transfer_item','cost_amount','decimal(18,2) DEFAULT NULL COMMENT ''实际成本''');
CALL jk_v31_add_column('jk_stock_transfer_item','unit_spread','decimal(18,2) DEFAULT NULL COMMENT ''单位价差''');
CALL jk_v31_add_column('jk_stock_transfer_item','spread_amount','decimal(18,2) DEFAULT NULL COMMENT ''价差金额''');
CALL jk_v31_add_column('jk_stock_transfer_item','cost_method','varchar(32) DEFAULT NULL COMMENT ''成本方法''');
CALL jk_v31_add_column('jk_stock_transfer_item','cost_snapshot_json','longtext COMMENT ''批次成本快照''');
CALL jk_v31_add_column('jk_stock_transfer_item','profit_status','varchar(32) NOT NULL DEFAULT ''PENDING'' COMMENT ''经营收益状态''');

CALL jk_v31_add_column('jk_commission_rule','plan_id','bigint DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','rule_code','varchar(80) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','reward_type','varchar(60) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','performance_type','varchar(60) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','beneficiary_type','varchar(60) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','base_type','varchar(60) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','calculation_type','varchar(40) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','rate','decimal(18,6) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','fixed_amount','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','unit_amount','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','trigger_timing','varchar(40) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','settle_delay_days','int NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','stack_group','varchar(80) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','stack_policy','varchar(40) NOT NULL DEFAULT ''MAX_ONE''');
CALL jk_v31_add_column('jk_commission_rule','priority','int NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','per_order_cap','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','per_user_period_cap','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','total_budget','decimal(18,2) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','requires_registered_customer','tinyint(1) NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','requires_voucher','tinyint(1) NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','requires_audit','tinyint(1) NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_commission_rule','publish_status','varchar(32) NOT NULL DEFAULT ''DRAFT''');
CALL jk_v31_add_column('jk_commission_rule','published_by','bigint DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_rule','published_time','datetime DEFAULT NULL');

CALL jk_v31_add_column('jk_commission_record','source_item_id','bigint DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_record','reward_type','varchar(60) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_record','rule_version_no','int DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_record','commission_action_key','varchar(255) DEFAULT NULL');
CALL jk_v31_add_column('jk_commission_record','income_nature','varchar(32) NOT NULL DEFAULT ''PLATFORM_PAYABLE''');
CALL jk_v31_add_column('jk_commission_record','relation_snapshot_json','longtext');
CALL jk_v31_add_column('jk_commission_record','source_snapshot_json','longtext');
CALL jk_v31_add_column('jk_commission_record','calculation_snapshot_json','longtext');
CALL jk_v31_add_column('jk_commission_record','reversed_amount','decimal(18,2) NOT NULL DEFAULT 0.00');
CALL jk_v31_add_column('jk_commission_record','negative_offset_amount','decimal(18,2) NOT NULL DEFAULT 0.00');

-- 第三批字段与旧 report_export_task 兼容
CALL jk_v31_add_column('jk_trade_receive_exception','resolution_type','varchar(40) DEFAULT NULL');
CALL jk_v31_add_column('jk_trade_receive_exception','normal_received_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_trade_receive_exception','exception_qty','int NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_trade_receive_exception','refund_amount','decimal(18,2) NOT NULL DEFAULT 0.00');
CALL jk_v31_add_column('jk_trade_receive_exception','compensation_amount','decimal(18,2) NOT NULL DEFAULT 0.00');
CALL jk_v31_add_column('jk_trade_receive_exception','receiver_confirm_status','varchar(32) NOT NULL DEFAULT ''PENDING''');
CALL jk_v31_add_column('jk_trade_receive_exception','sender_confirm_status','varchar(32) NOT NULL DEFAULT ''PENDING''');
CALL jk_v31_add_column('jk_trade_receive_exception','resolution_snapshot_json','longtext');

CALL jk_v31_add_column('jk_report_export_task','request_no','varchar(80) DEFAULT NULL');
CALL jk_v31_add_column('jk_report_export_task','storage_provider','varchar(32) NOT NULL DEFAULT ''MINIO''');
CALL jk_v31_add_column('jk_report_export_task','object_key','varchar(500) DEFAULT NULL');
CALL jk_v31_add_column('jk_report_export_task','content_type','varchar(100) DEFAULT NULL');
CALL jk_v31_add_column('jk_report_export_task','download_count','int NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_report_export_task','created_by','bigint DEFAULT NULL');
CALL jk_v31_add_column('jk_report_export_task','completed_time','datetime DEFAULT NULL');
CALL jk_v31_add_column('jk_report_export_task','progress','int NOT NULL DEFAULT 0');
CALL jk_v31_add_column('jk_report_export_task','file_path','varchar(500) DEFAULT NULL');
CALL jk_v31_add_column('jk_report_export_task','request_user_id','bigint DEFAULT NULL');

DROP PROCEDURE IF EXISTS `jk_v31_add_column`;

-- 唯一索引：存在历史重复时不自动破坏数据，请先执行重复审计。
SET @dup_action = (SELECT COUNT(1) FROM jk_commission_record WHERE commission_action_key IS NOT NULL GROUP BY commission_action_key HAVING COUNT(1)>1 LIMIT 1);
SET @has_action_index = (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='jk_commission_record' AND index_name='uk_jk_commission_action_key');
SET @ddl = IF(IFNULL(@dup_action,0)=0 AND @has_action_index=0,
  'ALTER TABLE jk_commission_record ADD UNIQUE KEY uk_jk_commission_action_key (commission_action_key)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @dup_export_request = (SELECT COUNT(1) FROM jk_report_export_task WHERE request_no IS NOT NULL GROUP BY request_no HAVING COUNT(1)>1 LIMIT 1);
SET @has_export_request_index = (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='jk_report_export_task' AND index_name='uk_jk_export_request');
SET @ddl = IF(IFNULL(@dup_export_request,0)=0 AND @has_export_request_index=0,
  'ALTER TABLE jk_report_export_task ADD UNIQUE KEY uk_jk_export_request (request_no)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 默认佣金模板映射到 V3.1 字段，全部继续关闭，不填写正式比例。
UPDATE jk_commission_rule SET
  rule_code='MAKER_DIRECT_REFERRAL', reward_type='DIRECT_REFERRAL', source_type='RETAIL_SALE', performance_type='RETAIL_ONLINE',
  beneficiary_type='DIRECT_PARENT_SNAPSHOT', base_type='ITEM_PAID_AMOUNT', calculation_type='PERCENT', trigger_timing='ORDER_COMPLETED',
  stack_group='MAKER_REFERRAL', stack_policy='MAX_ONE', priority=100, publish_status='DRAFT', status=0,
  effective_time=NULL, expire_time=NULL, rate=NULL, fixed_amount=NULL, unit_amount=NULL, update_time=@now
WHERE rule_no='TPL-MAKER-DIRECT';
UPDATE jk_commission_rule SET
  rule_code='MAKER_SELF_RETAIL', reward_type='SELF_RETAIL', source_type='RETAIL_SALE', performance_type='RETAIL_OFFLINE',
  beneficiary_type='PERFORMANCE_OWNER', base_type='REAL_GROSS_PROFIT', calculation_type='PERCENT', trigger_timing='SALE_CONFIRMED',
  stack_group='MAKER_SELF', stack_policy='MAX_ONE', priority=100, publish_status='DRAFT', status=0,
  effective_time=NULL, expire_time=NULL, rate=NULL, fixed_amount=NULL, unit_amount=NULL, update_time=@now
WHERE rule_no='TPL-MAKER-SELF';
UPDATE jk_commission_rule SET
  rule_code='PARTNER_DIRECT_REFERRAL', reward_type='DIRECT_REFERRAL', source_type='RETAIL_SALE', performance_type='RETAIL_ONLINE',
  beneficiary_type='DIRECT_PARENT_SNAPSHOT', base_type='ITEM_PAID_AMOUNT', calculation_type='PERCENT', trigger_timing='ORDER_COMPLETED',
  stack_group='PARTNER_REFERRAL', stack_policy='MAX_ONE', priority=110, publish_status='DRAFT', status=0,
  effective_time=NULL, expire_time=NULL, rate=NULL, fixed_amount=NULL, unit_amount=NULL, update_time=@now
WHERE rule_no='TPL-PARTNER-DIRECT';
UPDATE jk_commission_rule SET
  rule_code='PARTNER_SELF_RETAIL', reward_type='SELF_RETAIL', source_type='RETAIL_SALE', performance_type='RETAIL_OFFLINE',
  beneficiary_type='PERFORMANCE_OWNER', base_type='REAL_GROSS_PROFIT', calculation_type='PERCENT', trigger_timing='SALE_CONFIRMED',
  stack_group='PARTNER_SELF', stack_policy='MAX_ONE', priority=110, publish_status='DRAFT', status=0,
  effective_time=NULL, expire_time=NULL, rate=NULL, fixed_amount=NULL, unit_amount=NULL, update_time=@now
WHERE rule_no='TPL-PARTNER-SELF';
UPDATE jk_commission_rule SET
  rule_code='COUNTY_REGION_RETAIL', reward_type='REGION_MANAGEMENT', source_type='RETAIL_SALE', performance_type='RETAIL_OFFLINE',
  beneficiary_type='COUNTY_AGENT_SNAPSHOT', base_type='ITEM_PAID_AMOUNT', calculation_type='PERCENT', trigger_timing='SALE_CONFIRMED',
  stack_group='COUNTY_REGION', stack_policy='MAX_ONE', priority=120, publish_status='DRAFT', status=0,
  effective_time=NULL, expire_time=NULL, rate=NULL, fixed_amount=NULL, unit_amount=NULL, update_time=@now
WHERE rule_no='TPL-COUNTY-REGION';
UPDATE jk_commission_rule SET
  rule_code='COUNTY_PLATFORM_ORDER_SUBSIDY', reward_type='PLATFORM_ORDER_SUBSIDY', source_type='PLATFORM_ORDER', performance_type='PLATFORM_PURCHASE',
  beneficiary_type='PURCHASER_SNAPSHOT', base_type='PLATFORM_ORDER_AMOUNT', calculation_type='PERCENT', trigger_timing='STOCK_IN',
  stack_group='COUNTY_ORDER_SUBSIDY', stack_policy='MAX_ONE', priority=80, publish_status='DRAFT', status=0,
  effective_time=NULL, expire_time=NULL, rate=NULL, fixed_amount=NULL, unit_amount=NULL, update_time=@now
WHERE rule_no='TPL-COUNTY-ORDER-SUBSIDY';
UPDATE jk_commission_rule SET
  rule_code='COUNTY_TRANSFER_SUBSIDY', reward_type='TRANSFER_PLATFORM_SUBSIDY', source_type='STOCK_TRANSFER', performance_type='STOCK_TRANSFER',
  beneficiary_type='TRANSFER_SENDER_SNAPSHOT', base_type='TRANSFER_AMOUNT', calculation_type='PERCENT', trigger_timing='STOCK_IN',
  stack_group='COUNTY_TRANSFER_SUBSIDY', stack_policy='MAX_ONE', priority=80, publish_status='DRAFT', status=0,
  effective_time=NULL, expire_time=NULL, rate=NULL, fixed_amount=NULL, unit_amount=NULL, update_time=@now
WHERE rule_no='TPL-COUNTY-TRANSFER-SUBSIDY';
UPDATE jk_commission_rule SET status=0,publish_status='DRAFT',effective_time=NULL,expire_time=NULL,
  rate=NULL,fixed_amount=NULL,unit_amount=NULL,update_time=@now
WHERE rule_no LIKE 'TPL-%' OR rule_no LIKE 'V31-%';

-- 前台业务权限点
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_time,update_time,tenant_id)
SELECT code,name,'V31','ACTION',1,remark,1,0,@now,@now,'000000'
FROM (
 SELECT 'performance.view.self' code,'查看本人业绩' name,'查看本人独立业绩账本' remark UNION ALL
 SELECT 'operation.profit.view.self','查看本人经营收益','查看 OFFLINE_REALIZED 经营收益' UNION ALL
 SELECT 'offline.sale.manage.self','管理本人线下销售','登记、查看、取消和退货本人线下终端销售' UNION ALL
 SELECT 'stock.check.self','本人库存盘点','区县代理发起和提交本人库存盘点' UNION ALL
 SELECT 'health.report.view.self','查看本人健康报告','查看非厂商健康周报和月报'
) p WHERE NOT EXISTS (SELECT 1 FROM jk_business_permission x WHERE x.permission_code=p.code);
UPDATE jk_business_permission SET enabled=1,status=1,is_deleted=0,update_time=@now
WHERE permission_code IN ('performance.view.self','operation.profit.view.self','offline.sale.manage.self','stock.check.self','health.report.view.self');

-- 三类代理业务权限；健康报告同时授予普通用户。只新增缺失关系。
INSERT INTO jk_business_role_permission(role_id,permission_code,status,is_deleted,create_time,update_time,tenant_id)
SELECT r.id,p.permission_code,1,0,@now,@now,'000000'
FROM jk_business_role r
JOIN jk_business_permission p ON p.permission_code IN ('performance.view.self','operation.profit.view.self','offline.sale.manage.self','health.report.view.self')
WHERE r.role_code IN ('maker','partner','county_agent') AND r.is_deleted=0
AND NOT EXISTS (SELECT 1 FROM jk_business_role_permission rp WHERE rp.role_id=r.id AND rp.permission_code=p.permission_code);
INSERT INTO jk_business_role_permission(role_id,permission_code,status,is_deleted,create_time,update_time,tenant_id)
SELECT r.id,'stock.check.self',1,0,@now,@now,'000000' FROM jk_business_role r
WHERE r.role_code='county_agent' AND r.is_deleted=0
AND NOT EXISTS (SELECT 1 FROM jk_business_role_permission rp WHERE rp.role_id=r.id AND rp.permission_code='stock.check.self');
INSERT INTO jk_business_role_permission(role_id,permission_code,status,is_deleted,create_time,update_time,tenant_id)
SELECT r.id,'health.report.view.self',1,0,@now,@now,'000000' FROM jk_business_role r
WHERE r.role_code='normal_user' AND r.is_deleted=0
AND NOT EXISTS (SELECT 1 FROM jk_business_role_permission rp WHERE rp.role_id=r.id AND rp.permission_code='health.report.view.self');

-- 后台菜单与 authority
SET @jk_root_id=(SELECT id FROM eb_system_menu WHERE (path='/operation/jzk' OR name='九州康管理') ORDER BY is_del,id LIMIT 1);
SET @jk_root_id=IFNULL(@jk_root_id,0);

DROP PROCEDURE IF EXISTS `jk_v31_seed_menu`;
DELIMITER $$
CREATE PROCEDURE `jk_v31_seed_menu`(IN p_name VARCHAR(100),IN p_path VARCHAR(255),IN p_perm VARCHAR(255),IN p_sort INT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE path=p_path OR (p_perm<>'' AND perms=p_perm)) THEN
    INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
    VALUES(@jk_root_id,p_name,NULL,NULLIF(p_perm,''),p_path,'C',p_sort,1,0,@now,@now);
  ELSE
    UPDATE eb_system_menu SET pid=@jk_root_id,name=p_name,perms=NULLIF(p_perm,''),path=p_path,menu_type='C',sort=p_sort,is_show=1,is_del=0,update_time=@now
    WHERE path=p_path OR (p_perm<>'' AND perms=p_perm);
  END IF;
END$$
DELIMITER ;
CALL jk_v31_seed_menu('角色佣金规则 V3.1','/operation/jzk/commissionV31','admin:jk:commission:v31:list',84);
CALL jk_v31_seed_menu('业绩与线下销售','/operation/jzk/businessLedger','admin:jk:offline:sale:list',73);
CALL jk_v31_seed_menu('库存盘点','/operation/jzk/stockCheck','admin:jk:stock:check:list',64);
CALL jk_v31_seed_menu('V3.1 运营中心','/operation/jzk/operationsV31','admin:jk:promotion:stat:list',45);
DROP PROCEDURE IF EXISTS `jk_v31_seed_menu`;

SET @commission_menu=(SELECT id FROM eb_system_menu WHERE path='/operation/jzk/commissionV31' ORDER BY is_del,id LIMIT 1);
SET @ledger_menu=(SELECT id FROM eb_system_menu WHERE path='/operation/jzk/businessLedger' ORDER BY is_del,id LIMIT 1);
SET @check_menu=(SELECT id FROM eb_system_menu WHERE path='/operation/jzk/stockCheck' ORDER BY is_del,id LIMIT 1);
SET @ops_menu=(SELECT id FROM eb_system_menu WHERE path='/operation/jzk/operationsV31' ORDER BY is_del,id LIMIT 1);

DROP PROCEDURE IF EXISTS `jk_v31_seed_action`;
DELIMITER $$
CREATE PROCEDURE `jk_v31_seed_action`(IN p_pid BIGINT,IN p_name VARCHAR(100),IN p_perm VARCHAR(255),IN p_sort INT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM eb_system_menu WHERE perms=p_perm) THEN
    INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
    VALUES(p_pid,p_name,NULL,p_perm,'','A',p_sort,1,0,@now,@now);
  ELSE
    UPDATE eb_system_menu SET pid=p_pid,name=p_name,path='',menu_type='A',sort=p_sort,is_show=1,is_del=0,update_time=@now WHERE perms=p_perm;
  END IF;
END$$
DELIMITER ;
CALL jk_v31_seed_action(@commission_menu,'佣金规则试算','admin:jk:commission:v31:trial',1);
CALL jk_v31_seed_action(@commission_menu,'佣金规则发布/停用','admin:jk:commission:v31:publish',2);
CALL jk_v31_seed_action(@ledger_menu,'查看业绩账本','admin:jk:performance:list',1);
CALL jk_v31_seed_action(@ledger_menu,'查看经营收益','admin:jk:operation:profit:list',2);
CALL jk_v31_seed_action(@ledger_menu,'审核线下销售','admin:jk:offline:sale:audit',3);
CALL jk_v31_seed_action(@check_menu,'执行库存盘点','admin:jk:stock:check:manage',1);
CALL jk_v31_seed_action(@ops_menu,'查看订阅消息任务','admin:jk:subscription:task:list',1);
CALL jk_v31_seed_action(@ops_menu,'执行 MinIO 报表导出','admin:jk:report:export:task',2);
CALL jk_v31_seed_action(@ops_menu,'查看健康报告','admin:jk:health:report',3);
DROP PROCEDURE IF EXISTS `jk_v31_seed_action`;

-- 外部能力开关默认关闭。项目配置文件中的同名配置优先级更高。
INSERT INTO eb_system_config(name,title,value,status,form_type,create_time,update_time)
SELECT 'jk_subscription_message_enabled','九州康订阅消息总开关','false',1,'text',@now,@now
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='jk_subscription_message_enabled');
INSERT INTO eb_system_config(name,title,value,status,form_type,create_time,update_time)
SELECT 'jk_wechat_miniprogram_code_enabled','九州康微信小程序码总开关','false',1,'text',@now,@now
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='jk_wechat_miniprogram_code_enabled');
INSERT INTO eb_system_config(name,title,value,status,form_type,create_time,update_time)
SELECT 'jk_minio_enabled','九州康 MinIO 总开关','false',1,'text',@now,@now
WHERE NOT EXISTS (SELECT 1 FROM eb_system_config WHERE name='jk_minio_enabled');

-- 执行后核对
SELECT permission_code,permission_name,enabled,status,is_deleted FROM jk_business_permission
WHERE permission_code IN ('performance.view.self','operation.profit.view.self','offline.sale.manage.self','stock.check.self','health.report.view.self');
SELECT id,pid,name,perms,path,menu_type,is_del FROM eb_system_menu
WHERE path IN ('/operation/jzk/commissionV31','/operation/jzk/businessLedger','/operation/jzk/stockCheck','/operation/jzk/operationsV31')
   OR perms LIKE 'admin:jk:commission:v31:%' OR perms LIKE 'admin:jk:stock:check:%'
   OR perms IN ('admin:jk:performance:list','admin:jk:operation:profit:list','admin:jk:offline:sale:audit','admin:jk:subscription:task:list','admin:jk:report:export:task','admin:jk:health:report')
ORDER BY pid,id;
