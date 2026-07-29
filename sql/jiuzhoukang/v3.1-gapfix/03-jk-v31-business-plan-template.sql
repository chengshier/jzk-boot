-- JZK V3.1 补漏批次 C：商业方案、模板化奖励规则和真实单据试算（MySQL 5.7）
-- 不插入任何已启用规则；模板能力由代码提供，首次配置只生成 DRAFT/DISABLED。

CREATE TABLE IF NOT EXISTS jk_business_rule_plan (
    id bigint NOT NULL AUTO_INCREMENT,
    plan_code varchar(64) NOT NULL,
    plan_name varchar(128) NOT NULL,
    version_no int NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'DRAFT',
    applicable_role_codes text,
    applicable_region_codes text,
    effective_start_time datetime DEFAULT NULL,
    effective_end_time datetime DEFAULT NULL,
    priority int NOT NULL DEFAULT 0,
    publish_status varchar(32) NOT NULL DEFAULT 'DRAFT',
    published_by bigint DEFAULT NULL,
    published_at datetime DEFAULT NULL,
    disabled_by bigint DEFAULT NULL,
    disabled_at datetime DEFAULT NULL,
    change_summary varchar(500) DEFAULT NULL,
    remark varchar(500) DEFAULT NULL,
    is_deleted tinyint(1) NOT NULL DEFAULT 0,
    version int NOT NULL DEFAULT 0,
    create_time datetime NOT NULL,
    update_time datetime NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jk_business_plan_version (plan_code,version_no,is_deleted),
    KEY idx_jk_business_plan_publish (publish_status,effective_start_time,effective_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康商业方案不可变版本';

CREATE TABLE IF NOT EXISTS jk_business_rule_publish_log (
    id bigint NOT NULL AUTO_INCREMENT,
    plan_id bigint NOT NULL,
    plan_code varchar(64) NOT NULL,
    plan_version_no int NOT NULL,
    action_type varchar(32) NOT NULL COMMENT 'PUBLISH/DISABLE/COPY_VERSION',
    before_snapshot_json longtext,
    after_snapshot_json longtext,
    operator_user_id bigint NOT NULL,
    reason varchar(500) DEFAULT NULL,
    request_no varchar(64) NOT NULL,
    create_time datetime NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_jk_business_plan_log_request (request_no),
    KEY idx_jk_business_plan_log_plan (plan_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商业方案发布与停用记录';

DELIMITER $$
DROP PROCEDURE IF EXISTS jk_v31_add_column_if_missing$$
CREATE PROCEDURE jk_v31_add_column_if_missing(IN p_table VARCHAR(64),IN p_column VARCHAR(64),IN p_definition TEXT)
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND COLUMN_NAME=p_column) THEN
        SET @ddl=CONCAT('ALTER TABLE `',p_table,'` ADD COLUMN `',p_column,'` ',p_definition);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL jk_v31_add_column_if_missing('jk_commission_rule','plan_code',"varchar(64) DEFAULT NULL COMMENT '商业方案编码快照'");
CALL jk_v31_add_column_if_missing('jk_commission_rule','plan_version_no',"int DEFAULT NULL COMMENT '商业方案版本快照'");
CALL jk_v31_add_column_if_missing('jk_commission_rule','template_code',"varchar(64) DEFAULT NULL COMMENT '业务奖励模板编码'");
CALL jk_v31_add_column_if_missing('jk_commission_rule','scope_config_json',"longtext COMMENT '适用商品、区域、周期门槛等业务范围'");
CALL jk_v31_add_column_if_missing('jk_commission_rule','income_nature',"varchar(32) NOT NULL DEFAULT 'PLATFORM_PAYABLE' COMMENT '平台另行支付奖励；不得写 OFFLINE_REALIZED'");
DROP PROCEDURE IF EXISTS jk_v31_add_column_if_missing;

-- 旧规则仅补平台应付性质，不启用、不发布、不重算历史。
UPDATE jk_commission_rule
SET income_nature='PLATFORM_PAYABLE'
WHERE is_deleted=0 AND (income_nature IS NULL OR income_nature='');

SET @now=NOW();
SET @tenant_id=COALESCE((SELECT tenant_id FROM jk_business_permission WHERE tenant_id IS NOT NULL ORDER BY id LIMIT 1),'000000');
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT code,name,'BUSINESS_RULE',type,1,remark,1,0,0,0,@now,@now,@tenant_id,0
FROM (
  SELECT 'admin:jk:business:plan:list' code,'商业方案查看' name,'MENU' type,'查看角色方案、版本和发布记录' remark
  UNION ALL SELECT 'admin:jk:business:plan:edit','商业方案草稿编辑','BUTTON','保存草稿和复制新版本'
  UNION ALL SELECT 'admin:jk:business:plan:publish','商业方案发布','BUTTON','发布不可变版本，不重算历史'
  UNION ALL SELECT 'admin:jk:business:plan:disable','商业方案停用','BUTTON','只影响停用后的新业务'
  UNION ALL SELECT 'admin:jk:commission:rule:advanced','奖励规则高级配置','BUTTON','底层技术枚举，仅高级规则管理员可授权'
) seed
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission p WHERE p.permission_code=seed.code AND p.is_deleted=0);

SET @jk_root_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk' OR name IN ('九州康管理','九州康')) ORDER BY id LIMIT 1);
SET @rule_group_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/business-rule' ORDER BY id LIMIT 1);
SET @plan_menu_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/businessPlan' OR perms='admin:jk:business:plan:list') ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @rule_group_id,'商业方案',NULL,'admin:jk:business:plan:list','/operation/jzk/businessPlan','C',100,1,0,@now,@now
WHERE @rule_group_id IS NOT NULL AND @plan_menu_id IS NULL;
SET @plan_menu_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/businessPlan' OR perms='admin:jk:business:plan:list') ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@rule_group_id,name='商业方案',component='/operation/jzk/businessPlan',perms='admin:jk:business:plan:list',is_delete=0,update_time=@now WHERE id=@plan_menu_id;

INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @plan_menu_id,button_name,NULL,permission_code,'','A',button_sort,1,0,@now,@now
FROM (
  SELECT '编辑草稿' button_name,'admin:jk:business:plan:edit' permission_code,10 button_sort
  UNION ALL SELECT '发布方案','admin:jk:business:plan:publish',9
  UNION ALL SELECT '停用方案','admin:jk:business:plan:disable',8
) buttons
WHERE @plan_menu_id IS NOT NULL
  AND NOT EXISTS(SELECT 1 FROM eb_system_menu m WHERE m.is_delete=0 AND m.perms=buttons.permission_code);

-- 收益奖励规则归入商业规则中心；高级技术按钮不自动分配给普通运营角色。
UPDATE eb_system_menu SET pid=@rule_group_id,update_time=@now
WHERE is_delete=0 AND component='/operation/jzk/commissionRule' AND @rule_group_id IS NOT NULL;

SELECT 'commission templates enabled by seed' AS audit_item,COUNT(*) AS enabled_count
FROM jk_commission_rule
WHERE is_deleted=0 AND template_code IS NOT NULL AND status=1 AND publish_status='PUBLISHED';
