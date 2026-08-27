-- JZK V3.1 补漏批次 E：推广效果事件与统计（MySQL 5.7）
-- 客户端只能写 OPEN；RETAIL_COMPLETED 必须由后端零售归属完成事件写入。

CREATE TABLE IF NOT EXISTS jk_promotion_effect_event (
  id bigint NOT NULL AUTO_INCREMENT,
  event_no varchar(64) NOT NULL,
  scene_code varchar(128) DEFAULT NULL,
  promoter_user_id bigint DEFAULT NULL,
  visitor_user_id bigint DEFAULT NULL,
  event_type varchar(32) NOT NULL COMMENT 'OPEN/RETAIL_COMPLETED',
  source_type varchar(64) DEFAULT NULL,
  source_id bigint DEFAULT NULL,
  source_item_id bigint DEFAULT NULL,
  source_no varchar(128) DEFAULT NULL,
  amount decimal(18,2) NOT NULL DEFAULT 0.00,
  attribution_snapshot_json longtext,
  metadata_json longtext,
  request_no varchar(160) NOT NULL,
  idempotency_key varchar(200) NOT NULL,
  occurred_at datetime NOT NULL,
  is_deleted tinyint(1) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_jk_promotion_effect_no(event_no),
  UNIQUE KEY uk_jk_promotion_effect_action(idempotency_key),
  KEY idx_jk_promotion_effect_scene(scene_code,event_type,occurred_at),
  KEY idx_jk_promotion_effect_promoter(promoter_user_id,event_type,occurred_at),
  KEY idx_jk_promotion_effect_source(source_type,source_id,source_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广打开和后端成交效果事件';

SET @now=NOW();
SET @tenant_id=COALESCE((SELECT tenant_id FROM jk_business_permission WHERE tenant_id IS NOT NULL ORDER BY id LIMIT 1),'000000');
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT 'admin:jk:promotion:effect:view','推广效果统计','PRODUCT_PROMOTION','MENU',1,'查看推广打开、有效成交和金额；成交事件只由后端写入',1,0,0,0,@now,@now,@tenant_id,0
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:promotion:effect:view' AND is_deleted=0);

SET @jk_root_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk' OR name IN ('九州康管理','九州康')) ORDER BY id LIMIT 1);
SET @product_group_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/product-promotion' ORDER BY id LIMIT 1);
SET @effect_menu_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/promotionEffect' OR perms='admin:jk:promotion:effect:view') ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @product_group_id,'推广效果统计',NULL,'admin:jk:promotion:effect:view','/operation/jzk/promotionEffect','C',78,1,0,@now,@now
WHERE @product_group_id IS NOT NULL AND @effect_menu_id IS NULL;
SET @effect_menu_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/promotionEffect' OR perms='admin:jk:promotion:effect:view') ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@product_group_id,name='推广效果统计',component='/operation/jzk/promotionEffect',perms='admin:jk:promotion:effect:view',is_delete=0,update_time=@now WHERE id=@effect_menu_id;
