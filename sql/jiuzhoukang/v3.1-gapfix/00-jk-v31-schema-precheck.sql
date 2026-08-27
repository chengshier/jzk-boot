-- 九州康 JZK V3.1 补漏正式包：真实结构预检（MySQL 5.7）
-- 只读脚本。任何 REQUIRED_MISSING 不为 0 时禁止继续执行升级脚本。

SET @schema_name = DATABASE();

SELECT 'eb_system_menu.component' AS check_item,
       IF(COUNT(*) = 1, 'OK', 'REQUIRED_MISSING') AS check_result
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'eb_system_menu' AND COLUMN_NAME = 'component'
UNION ALL
SELECT 'eb_system_menu.is_delete', IF(COUNT(*) = 1, 'OK', 'REQUIRED_MISSING')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'eb_system_menu' AND COLUMN_NAME = 'is_delete'
UNION ALL
SELECT 'eb_system_menu.path must not be used', IF(COUNT(*) = 0, 'OK', 'LEGACY_COLUMN_PRESENT')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'eb_system_menu' AND COLUMN_NAME = 'path'
UNION ALL
SELECT 'eb_system_menu.is_del must not be used', IF(COUNT(*) = 0, 'OK', 'LEGACY_COLUMN_PRESENT')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'eb_system_menu' AND COLUMN_NAME = 'is_del'
UNION ALL
SELECT 'jk_business_permission.module_code', IF(COUNT(*) = 1, 'OK', 'REQUIRED_MISSING')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'jk_business_permission' AND COLUMN_NAME = 'module_code'
UNION ALL
SELECT 'jk_business_permission.enabled', IF(COUNT(*) = 1, 'OK', 'REQUIRED_MISSING')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'jk_business_permission' AND COLUMN_NAME = 'enabled'
UNION ALL
SELECT 'jk_business_permission.tenant_id', IF(COUNT(*) = 1, 'OK', 'REQUIRED_MISSING')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'jk_business_permission' AND COLUMN_NAME = 'tenant_id'
UNION ALL
SELECT 'jk_business_permission.parent_code must not be used', IF(COUNT(*) = 0, 'OK', 'LEGACY_COLUMN_PRESENT')
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'jk_business_permission' AND COLUMN_NAME = 'parent_code';

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @schema_name
  AND TABLE_NAME IN ('eb_system_menu', 'jk_business_permission', 'eb_user', 'jk_retail_order_attribution')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT 'legacy menu sql usage' AS audit_item, id, pid, name, perms, component, is_delete
FROM eb_system_menu
WHERE is_delete = 0
  AND (component IS NULL OR component = '')
  AND menu_type IN ('M', 'C')
ORDER BY pid, sort, id;
