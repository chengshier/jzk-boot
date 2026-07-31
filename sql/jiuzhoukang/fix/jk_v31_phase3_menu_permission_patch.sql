-- 九州康 JZK V3.1 菜单与权限纠错补丁（MySQL 5.7）
-- 真实字段：eb_system_menu.component / is_delete；jk_business_permission 无 parent_code。
-- 本脚本可重复执行，不使用固定父菜单 ID，不自动给普通运营角色授权高风险按钮。

SET @now = NOW();
SET @tenant_id = COALESCE((SELECT tenant_id FROM jk_business_permission WHERE tenant_id IS NOT NULL ORDER BY id LIMIT 1), '000000');

-- 九州康根目录：通过名称和 component 查找，不依赖固定 ID，并纠正历史根菜单地址。
SET @operation_root_id = (SELECT id FROM eb_system_menu WHERE is_delete = 0 AND name IN ('运营','运营管理','业务管理') ORDER BY id LIMIT 1);
SET @operation_root_id = IFNULL(@operation_root_id, 0);
SET @jk_root_id = (SELECT id FROM eb_system_menu WHERE is_delete = 0 AND (component = '/operation/jzk' OR name IN ('九州康管理','九州康','九州康业务')) ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @operation_root_id,'九州康业务','s-data',NULL,'/operation/jzk','M',50,1,0,@now,@now
WHERE @jk_root_id IS NULL;
SET @jk_root_id = (SELECT id FROM eb_system_menu WHERE is_delete = 0 AND (component = '/operation/jzk' OR name IN ('九州康管理','九州康','九州康业务')) ORDER BY id LIMIT 1);
UPDATE eb_system_menu
SET name='九州康业务',component='/operation/jzk',menu_type='M',is_show=1,is_delete=0,update_time=@now
WHERE id=@jk_root_id;

-- V3.1 分组目录。禁止创建没有任何真实页面的“身份与关系”空目录；身份页面继续复用现有身份分组。
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @jk_root_id,'商业规则中心',NULL,NULL,'/operation/jzk/group/business-rule','M',90,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/business-rule');
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @jk_root_id,'订单与库存',NULL,NULL,'/operation/jzk/group/order-stock','M',80,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/order-stock');
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @jk_root_id,'业绩与收益',NULL,NULL,'/operation/jzk/group/performance-income','M',70,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/performance-income');
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @jk_root_id,'运营工具',NULL,NULL,'/operation/jzk/group/operation-tools','M',60,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/operation-tools');
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @jk_root_id,'健康管理',NULL,NULL,'/operation/jzk/group/health','M',50,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/health');

SET @rule_group_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/business-rule' ORDER BY id LIMIT 1);
SET @order_group_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/order-stock' ORDER BY id LIMIT 1);
SET @operation_tools_group_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/operation-tools' ORDER BY id LIMIT 1);
SET @health_group_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/health' ORDER BY id LIMIT 1);

-- 业务权限目录：按真实列写入，禁止 parent_code。
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT 'admin:jk:stock:check:list','库存盘点查询','ORDER_STOCK','MENU',1,'第三批库存盘点列表与详情',1,0,0,0,@now,@now,@tenant_id,0
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:stock:check:list' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT 'admin:jk:stock:check:audit','库存盘点审核','ORDER_STOCK','BUTTON',1,'通过统一库存流水应用盘盈盘亏',1,0,0,0,@now,@now,@tenant_id,0
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:stock:check:audit' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT 'admin:jk:promotion:scene:manage','真实微信推广码场景','OPERATION_TOOLS','MENU',1,'真实微信小程序码场景，默认关闭',1,0,0,0,@now,@now,@tenant_id,0
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:promotion:scene:manage' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT 'admin:jk:subscription:task:list','微信消息任务查询','OPERATION_TOOLS','MENU',1,'查询订阅消息真实发送状态',1,0,0,0,@now,@now,@tenant_id,0
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:subscription:task:list' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT 'admin:jk:subscription:task:manage','微信消息任务执行','OPERATION_TOOLS','BUTTON',1,'手动处理和重新入队，不绕过微信授权',1,0,0,0,@now,@now,@tenant_id,0
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:subscription:task:manage' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT 'admin:jk:health:report:list','健康周报月报查询','HEALTH','MENU',1,'只查询真实健康记录汇总报告',1,0,0,0,@now,@now,@tenant_id,0
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:health:report:list' AND is_deleted=0);

-- 页面菜单。
SET @stock_check_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/stockCheck' OR perms='admin:jk:stock:check:list') ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @order_group_id,'库存盘点',NULL,'admin:jk:stock:check:list','/operation/jzk/stockCheck','C',71,1,0,@now,@now
WHERE @order_group_id IS NOT NULL AND @stock_check_menu_id IS NULL;
SET @stock_check_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/stockCheck' OR perms='admin:jk:stock:check:list') ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@order_group_id,name='库存盘点',perms='admin:jk:stock:check:list',component='/operation/jzk/stockCheck',menu_type='C',sort=71,is_show=1,is_delete=0,update_time=@now WHERE id=@stock_check_menu_id;
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @stock_check_menu_id,'审核库存盘点',NULL,'admin:jk:stock:check:audit','', 'A',1,1,0,@now,@now
WHERE @stock_check_menu_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE is_delete=0 AND perms='admin:jk:stock:check:audit');

SET @receive_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND component='/operation/jzk/receiveExceptionResolution' ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @order_group_id,'异常收货处理',NULL,'admin:jk:receive:exception:list','/operation/jzk/receiveExceptionResolution','C',69,1,0,@now,@now
WHERE @order_group_id IS NOT NULL AND @receive_menu_id IS NULL;
SET @receive_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND component='/operation/jzk/receiveExceptionResolution' ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@order_group_id,name='异常收货处理',perms='admin:jk:receive:exception:list',component='/operation/jzk/receiveExceptionResolution',menu_type='C',is_show=1,is_delete=0,update_time=@now WHERE id=@receive_menu_id;

SET @promotion_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/promotionCode' OR perms='admin:jk:promotion:scene:manage') ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @operation_tools_group_id,'真实微信推广码',NULL,'admin:jk:promotion:scene:manage','/operation/jzk/promotionCode','C',61,1,0,@now,@now
WHERE @operation_tools_group_id IS NOT NULL AND @promotion_menu_id IS NULL;
SET @promotion_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/promotionCode' OR perms='admin:jk:promotion:scene:manage') ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@operation_tools_group_id,component='/operation/jzk/promotionCode',menu_type='C',is_show=1,is_delete=0,update_time=@now WHERE id=@promotion_menu_id;

SET @health_report_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/healthReport' OR perms='admin:jk:health:report:list') ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @health_group_id,'健康周报月报',NULL,'admin:jk:health:report:list','/operation/jzk/healthReport','C',42,1,0,@now,@now
WHERE @health_group_id IS NOT NULL AND @health_report_menu_id IS NULL;
SET @health_report_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/healthReport' OR perms='admin:jk:health:report:list') ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@health_group_id,component='/operation/jzk/healthReport',menu_type='C',is_show=1,is_delete=0,update_time=@now WHERE id=@health_report_menu_id;

SET @subscription_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/subscriptionTask' OR perms='admin:jk:subscription:task:list') ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @operation_tools_group_id,'微信消息任务',NULL,'admin:jk:subscription:task:list','/operation/jzk/subscriptionTask','C',21,1,0,@now,@now
WHERE @operation_tools_group_id IS NOT NULL AND @subscription_menu_id IS NULL;
SET @subscription_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/subscriptionTask' OR perms='admin:jk:subscription:task:list') ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@operation_tools_group_id,component='/operation/jzk/subscriptionTask',menu_type='C',is_show=1,is_delete=0,update_time=@now WHERE id=@subscription_menu_id;
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @subscription_menu_id,'处理微信消息任务',NULL,'admin:jk:subscription:task:manage','', 'A',1,1,0,@now,@now
WHERE @subscription_menu_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE is_delete=0 AND perms='admin:jk:subscription:task:manage');

-- 任何没有真实子节点的九州康分组都不能保持可见，否则分栏菜单会把它当成首个跳转目标。
UPDATE eb_system_menu m
LEFT JOIN eb_system_menu c ON c.pid=m.id AND c.is_delete=0
SET m.is_show=0,m.is_delete=1,m.update_time=@now
WHERE m.pid=@jk_root_id
  AND m.menu_type='M'
  AND m.component LIKE '/operation/jzk/group/%'
  AND c.id IS NULL;

SELECT id,pid,name,perms,component,menu_type,is_show,is_delete
FROM eb_system_menu
WHERE is_delete=0 AND (pid=@jk_root_id OR component LIKE '/operation/jzk/%')
ORDER BY pid,sort DESC,id;
