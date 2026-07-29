-- 九州康 JZK V3.1 第三批补丁：Admin 菜单与 authority（MySQL 5.7）
-- 只初始化权限点和菜单，不自动把高风险按钮授予普通后台角色。
SET @now = NOW();
SET @operation_root_id = (SELECT id FROM eb_system_menu WHERE path='/operation' AND is_del=0 ORDER BY id LIMIT 1);
SET @operation_root_id = IFNULL(@operation_root_id, 0);
SET @jk_root_id = (SELECT id FROM eb_system_menu WHERE (path='/operation/jzk' OR name IN ('九州康管理','九州康')) ORDER BY is_del,id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @operation_root_id,'九州康管理','s-data',NULL,'/operation/jzk','M',50,1,0,@now,@now
WHERE @jk_root_id IS NULL;
SET @jk_root_id = (SELECT id FROM eb_system_menu WHERE (path='/operation/jzk' OR name IN ('九州康管理','九州康')) ORDER BY is_del,id LIMIT 1);

-- 业务权限点用于统一权限目录和审计；Admin authority 仍以 eb_system_menu.perms 为准。
INSERT INTO jk_business_permission(permission_code,permission_name,permission_type,parent_code,status,remark,is_deleted,create_time,update_time)
SELECT 'admin:jk:stock:check:list','库存盘点查询','MENU',NULL,1,'第三批库存盘点列表与详情',0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:stock:check:list' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,permission_type,parent_code,status,remark,is_deleted,create_time,update_time)
SELECT 'admin:jk:stock:check:audit','库存盘点审核','BUTTON','admin:jk:stock:check:list',1,'通过统一库存流水应用盘盈盘亏',0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:stock:check:audit' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,permission_type,parent_code,status,remark,is_deleted,create_time,update_time)
SELECT 'admin:jk:promotion:scene:manage','真实微信推广码场景','MENU',NULL,1,'管理真实微信小程序码场景，默认关闭',0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:promotion:scene:manage' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,permission_type,parent_code,status,remark,is_deleted,create_time,update_time)
SELECT 'admin:jk:subscription:task:list','微信消息任务查询','MENU',NULL,1,'查询订阅消息真实发送状态',0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:subscription:task:list' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,permission_type,parent_code,status,remark,is_deleted,create_time,update_time)
SELECT 'admin:jk:subscription:task:manage','微信消息任务执行','BUTTON','admin:jk:subscription:task:list',1,'手动处理和重新入队，不绕过微信授权',0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:subscription:task:manage' AND is_deleted=0);
INSERT INTO jk_business_permission(permission_code,permission_name,permission_type,parent_code,status,remark,is_deleted,create_time,update_time)
SELECT 'admin:jk:health:report:list','健康周报月报查询','MENU',NULL,1,'只查询真实健康记录汇总报告',0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:health:report:list' AND is_deleted=0);

-- 库存盘点
SET @stock_check_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/stockCheck' OR perms='admin:jk:stock:check:list' ORDER BY is_del,id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @jk_root_id,'库存盘点',NULL,'admin:jk:stock:check:list','/operation/jzk/stockCheck','C',71,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND @stock_check_menu_id IS NULL;
SET @stock_check_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/stockCheck' OR perms='admin:jk:stock:check:list' ORDER BY is_del,id LIMIT 1);
UPDATE eb_system_menu SET pid=@jk_root_id,name='库存盘点',perms='admin:jk:stock:check:list',path='/operation/jzk/stockCheck',menu_type='C',sort=71,is_show=1,is_del=0,update_time=@now WHERE id=@stock_check_menu_id;
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @stock_check_menu_id,'审核库存盘点',NULL,'admin:jk:stock:check:audit','','A',1,1,0,@now,@now
WHERE @stock_check_menu_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE perms='admin:jk:stock:check:audit' AND is_del=0);

-- 异常收货 V2 方案复用异常工作台查询/处理 authority，不新造第二套越权口径。
SET @receive_v2_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/receiveExceptionResolution' ORDER BY is_del,id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @jk_root_id,'异常收货V2方案',NULL,'admin:jk:receive:exception:list','/operation/jzk/receiveExceptionResolution','C',69,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND @receive_v2_menu_id IS NULL;
SET @receive_v2_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/receiveExceptionResolution' ORDER BY is_del,id LIMIT 1);
UPDATE eb_system_menu SET pid=@jk_root_id,name='异常收货V2方案',perms='admin:jk:receive:exception:list',path='/operation/jzk/receiveExceptionResolution',menu_type='C',sort=69,is_show=1,is_del=0,update_time=@now WHERE id=@receive_v2_menu_id;
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @receive_v2_menu_id,'处理异常收货V2方案',NULL,'admin:jk:receive:exception:handle','','A',1,1,0,@now,@now
WHERE @receive_v2_menu_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE pid=@receive_v2_menu_id AND perms='admin:jk:receive:exception:handle' AND is_del=0);

-- 真实微信推广码
SET @promotion_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/promotionCode' OR perms='admin:jk:promotion:scene:manage' ORDER BY is_del,id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @jk_root_id,'真实微信推广码',NULL,'admin:jk:promotion:scene:manage','/operation/jzk/promotionCode','C',61,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND @promotion_menu_id IS NULL;
SET @promotion_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/promotionCode' OR perms='admin:jk:promotion:scene:manage' ORDER BY is_del,id LIMIT 1);
UPDATE eb_system_menu SET pid=@jk_root_id,name='真实微信推广码',perms='admin:jk:promotion:scene:manage',path='/operation/jzk/promotionCode',menu_type='C',sort=61,is_show=1,is_del=0,update_time=@now WHERE id=@promotion_menu_id;

-- 健康报告
SET @health_report_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/healthReport' OR perms='admin:jk:health:report:list' ORDER BY is_del,id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @jk_root_id,'健康周报月报',NULL,'admin:jk:health:report:list','/operation/jzk/healthReport','C',42,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND @health_report_menu_id IS NULL;
SET @health_report_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/healthReport' OR perms='admin:jk:health:report:list' ORDER BY is_del,id LIMIT 1);
UPDATE eb_system_menu SET pid=@jk_root_id,name='健康周报月报',perms='admin:jk:health:report:list',path='/operation/jzk/healthReport',menu_type='C',sort=42,is_show=1,is_del=0,update_time=@now WHERE id=@health_report_menu_id;

-- 微信订阅消息任务
SET @subscription_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/subscriptionTask' OR perms='admin:jk:subscription:task:list' ORDER BY is_del,id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @jk_root_id,'微信消息任务',NULL,'admin:jk:subscription:task:list','/operation/jzk/subscriptionTask','C',21,1,0,@now,@now
WHERE @jk_root_id IS NOT NULL AND @subscription_menu_id IS NULL;
SET @subscription_menu_id = (SELECT id FROM eb_system_menu WHERE path='/operation/jzk/subscriptionTask' OR perms='admin:jk:subscription:task:list' ORDER BY is_del,id LIMIT 1);
UPDATE eb_system_menu SET pid=@jk_root_id,name='微信消息任务',perms='admin:jk:subscription:task:list',path='/operation/jzk/subscriptionTask',menu_type='C',sort=21,is_show=1,is_del=0,update_time=@now WHERE id=@subscription_menu_id;
INSERT INTO eb_system_menu(pid,name,icon,perms,path,menu_type,sort,is_show,is_del,create_time,update_time)
SELECT @subscription_menu_id,'处理微信消息任务',NULL,'admin:jk:subscription:task:manage','','A',1,1,0,@now,@now
WHERE @subscription_menu_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE perms='admin:jk:subscription:task:manage' AND is_del=0);

SELECT id,pid,name,perms,path,menu_type,is_show,is_del FROM eb_system_menu
WHERE path IN ('/operation/jzk/stockCheck','/operation/jzk/receiveExceptionResolution','/operation/jzk/promotionCode','/operation/jzk/healthReport','/operation/jzk/subscriptionTask')
   OR perms IN ('admin:jk:stock:check:audit','admin:jk:receive:exception:handle','admin:jk:subscription:task:manage')
ORDER BY pid,id;
