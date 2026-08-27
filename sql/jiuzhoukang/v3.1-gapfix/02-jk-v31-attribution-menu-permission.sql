-- JZK V3.1 补漏批次 B：个人资料区域、零售订单归属菜单与权限（MySQL 5.7）
-- 真实字段：eb_system_menu.component/is_delete；jk_business_permission 不存在 parent_code。

SET @now = NOW();
SET @tenant_id = COALESCE((SELECT tenant_id FROM jk_business_permission WHERE tenant_id IS NOT NULL ORDER BY id LIMIT 1), '000000');
SET @jk_root_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk' OR name IN ('九州康管理','九州康')) ORDER BY id LIMIT 1);
SET @identity_group_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/identity' ORDER BY id LIMIT 1);
SET @order_group_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/order-stock' ORDER BY id LIMIT 1);

INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT code,name,module_code,type,1,remark,1,0,0,0,@now,@now,@tenant_id,0
FROM (
    SELECT 'admin:jk:user:profile:region:view' code,'用户资料区域查看' name,'IDENTITY_RELATION' module_code,'BUTTON' type,'复用现有用户资料页查看标准区域' remark
    UNION ALL SELECT 'admin:jk:user:profile:region:edit','用户资料区域修改','IDENTITY_RELATION','BUTTON','修改只影响后续订单并写审计日志'
    UNION ALL SELECT 'admin:jk:retail:attribution:list','零售订单归属查询','ORDER_STOCK','MENU','逐订单明细查询不可变归属快照'
    UNION ALL SELECT 'admin:jk:retail:attribution:detail','零售订单归属详情','ORDER_STOCK','BUTTON','查看关系、个人资料、收货地址和金额分摊解释链'
    UNION ALL SELECT 'admin:jk:retail:attribution:resolve','零售订单归属处理','ORDER_STOCK','BUTTON','仅处理未锁定 PENDING_MANUAL/CONFLICT'
    UNION ALL SELECT 'admin:jk:retail:attribution:adjust','零售订单归属调整','ORDER_STOCK','BUTTON','锁定后只创建冲正与补偿记录'
    UNION ALL SELECT 'admin:jk:retail:attribution:export','零售订单归属导出','ORDER_STOCK','BUTTON','受控导出，不包含无权限敏感字段'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM jk_business_permission p WHERE p.permission_code=seed.code AND p.is_deleted=0
);

SET @attribution_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/retailAttribution' OR perms='admin:jk:retail:attribution:list') ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @order_group_id,'零售订单归属',NULL,'admin:jk:retail:attribution:list','/operation/jzk/retailAttribution','C',85,1,0,@now,@now
WHERE @order_group_id IS NOT NULL AND @attribution_menu_id IS NULL;
SET @attribution_menu_id = (SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/retailAttribution' OR perms='admin:jk:retail:attribution:list') ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@order_group_id,name='零售订单归属',perms='admin:jk:retail:attribution:list',component='/operation/jzk/retailAttribution',menu_type='C',sort=85,is_show=1,is_delete=0,update_time=@now WHERE id=@attribution_menu_id;

INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @attribution_menu_id,button_name,NULL,permission_code,'','A',button_sort,1,0,@now,@now
FROM (
    SELECT '查看详情' button_name,'admin:jk:retail:attribution:detail' permission_code,10 button_sort
    UNION ALL SELECT '处理待定归属','admin:jk:retail:attribution:resolve',9
    UNION ALL SELECT '创建调整补偿','admin:jk:retail:attribution:adjust',8
    UNION ALL SELECT '导出','admin:jk:retail:attribution:export',7
) button_seed
WHERE @attribution_menu_id IS NOT NULL
  AND NOT EXISTS(SELECT 1 FROM eb_system_menu m WHERE m.is_delete=0 AND m.perms=button_seed.permission_code);

-- 用户资料区域复用现有用户页面，只增加按钮权限，不创建独立“服务区域”菜单。
SELECT @identity_group_id AS identity_group_id,@attribution_menu_id AS attribution_menu_id;
