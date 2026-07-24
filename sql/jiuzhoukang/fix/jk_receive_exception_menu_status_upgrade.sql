-- 九州康异常收货菜单、权限与状态字典升级（MySQL 5.7）
--
-- 适用代码：
--   jzk-boot / jzk-vue / jzk-app 的 fix/jk-business-consistency 分支
--
-- 执行前提：
-- 1. 已执行 jk_trade_receive_exception_upgrade.sql；
-- 2. 已部署包含 JkPermissionCodes.ADMIN_RECEIVE_EXCEPTION_* 的后端；
-- 3. 已备份 eb_system_menu、jk_dict_type、jk_dict_item。
--
-- 本脚本可重复执行，不自动给普通后台角色授权。
-- 执行后请在“角色管理”中把“异常收货处理”页面及“处理异常收货”按钮权限授予负责订货/调拨的运营角色。

SET @now = NOW();

-- ============================================================================
-- 一、状态字典
-- ============================================================================
-- NOT EXISTS 不过滤软删除状态，避免历史软删除记录仍受唯一键约束时重复插入失败；
-- 随后的 UPDATE 会统一恢复状态、文案与标签。

INSERT INTO `jk_dict_type`
(`dict_type`,`dict_name`,`remark`,`status`,`is_deleted`,`create_time`,`update_time`)
SELECT 'platform_order_status','平台订货状态','九州康平台订货状态',1,0,@now,@now
WHERE NOT EXISTS (
  SELECT 1 FROM `jk_dict_type` WHERE `dict_type`='platform_order_status'
);

INSERT INTO `jk_dict_type`
(`dict_type`,`dict_name`,`remark`,`status`,`is_deleted`,`create_time`,`update_time`)
SELECT 'stock_transfer_status','库存调拨状态','九州康库存调拨状态',1,0,@now,@now
WHERE NOT EXISTS (
  SELECT 1 FROM `jk_dict_type` WHERE `dict_type`='stock_transfer_status'
);

INSERT INTO `jk_dict_type`
(`dict_type`,`dict_name`,`remark`,`status`,`is_deleted`,`create_time`,`update_time`)
SELECT 'receive_status','收货状态','九州康订货与调拨收货状态',1,0,@now,@now
WHERE NOT EXISTS (
  SELECT 1 FROM `jk_dict_type` WHERE `dict_type`='receive_status'
);

UPDATE `jk_dict_type`
SET `status`=1,
    `is_deleted`=0,
    `update_time`=@now
WHERE `dict_type` IN ('platform_order_status','stock_transfer_status','receive_status');

INSERT INTO `jk_dict_item`
(`dict_type`,`item_code`,`item_label`,`item_tag`,`sort`,`remark`,`status`,`is_deleted`,`create_time`,`update_time`)
SELECT 'platform_order_status','RECEIVE_EXCEPTION','收货异常处理中','warning',75,
       '用户已上报短缺、破损或其他收货异常，异常关闭前禁止正常入库',1,0,@now,@now
WHERE NOT EXISTS (
  SELECT 1 FROM `jk_dict_item`
  WHERE `dict_type`='platform_order_status' AND `item_code`='RECEIVE_EXCEPTION'
);

INSERT INTO `jk_dict_item`
(`dict_type`,`item_code`,`item_label`,`item_tag`,`sort`,`remark`,`status`,`is_deleted`,`create_time`,`update_time`)
SELECT 'stock_transfer_status','RECEIVE_EXCEPTION','收货异常处理中','warning',85,
       '下级已上报调拨收货异常，异常关闭前禁止正常入库',1,0,@now,@now
WHERE NOT EXISTS (
  SELECT 1 FROM `jk_dict_item`
  WHERE `dict_type`='stock_transfer_status' AND `item_code`='RECEIVE_EXCEPTION'
);

INSERT INTO `jk_dict_item`
(`dict_type`,`item_code`,`item_label`,`item_tag`,`sort`,`remark`,`status`,`is_deleted`,`create_time`,`update_time`)
SELECT 'receive_status','EXCEPTION','收货异常处理中','warning',20,
       '收货差异已上报，暂不执行库存入库',1,0,@now,@now
WHERE NOT EXISTS (
  SELECT 1 FROM `jk_dict_item`
  WHERE `dict_type`='receive_status' AND `item_code`='EXCEPTION'
);

INSERT INTO `jk_dict_item`
(`dict_type`,`item_code`,`item_label`,`item_tag`,`sort`,`remark`,`status`,`is_deleted`,`create_time`,`update_time`)
SELECT 'receive_status','UNRECEIVED','待收货','warning',10,
       '已发货或已拨货，等待收货人确认',1,0,@now,@now
WHERE NOT EXISTS (
  SELECT 1 FROM `jk_dict_item`
  WHERE `dict_type`='receive_status' AND `item_code`='UNRECEIVED'
);

-- 已有记录存在但被停用或删除时恢复并统一文案。
UPDATE `jk_dict_item`
SET `item_label`='收货异常处理中',
    `item_tag`='warning',
    `status`=1,
    `is_deleted`=0,
    `update_time`=@now
WHERE `dict_type` IN ('platform_order_status','stock_transfer_status')
  AND `item_code`='RECEIVE_EXCEPTION';

UPDATE `jk_dict_item`
SET `item_label`='收货异常处理中',
    `item_tag`='warning',
    `status`=1,
    `is_deleted`=0,
    `update_time`=@now
WHERE `dict_type`='receive_status' AND `item_code`='EXCEPTION';

UPDATE `jk_dict_item`
SET `item_label`='待收货',
    `item_tag`='warning',
    `status`=1,
    `is_deleted`=0,
    `update_time`=@now
WHERE `dict_type`='receive_status' AND `item_code`='UNRECEIVED';

-- ============================================================================
-- 二、后台菜单与 authority
-- eb_system_menu 字段顺序以 CRMEB v1.4 为准：
-- id, pid, name, icon, perms, path, menu_type, sort, is_show, is_del,
-- create_time, update_time。
-- ============================================================================

SET @operation_root_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `path`='/operation' AND `is_del`=0
  ORDER BY `id` ASC LIMIT 1
);

-- 兼容历史数据中“设置”根菜单固定 ID=12 的情况。
SET @operation_root_id = IFNULL(@operation_root_id, 12);

-- 包含软删除记录，避免 path 或业务唯一约束下重复插入。
SET @jk_root_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk' OR `name` IN ('九州康管理','九州康'))
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);

INSERT INTO `eb_system_menu`
(`pid`,`name`,`icon`,`perms`,`path`,`menu_type`,`sort`,`is_show`,`is_del`,`create_time`,`update_time`)
SELECT @operation_root_id,'九州康管理','s-data',NULL,'/operation/jzk','M',50,1,0,@now,@now
WHERE @jk_root_id IS NULL;

SET @jk_root_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk' OR `name` IN ('九州康管理','九州康'))
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);

UPDATE `eb_system_menu`
SET `pid`=@operation_root_id,
    `name`='九州康管理',
    `path`='/operation/jzk',
    `menu_type`='M',
    `is_show`=1,
    `is_del`=0,
    `update_time`=@now
WHERE `id`=@jk_root_id;

SET @receive_exception_menu_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk/receiveException'
      OR `perms`='admin:jk:receive:exception:list')
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);

INSERT INTO `eb_system_menu`
(`pid`,`name`,`icon`,`perms`,`path`,`menu_type`,`sort`,`is_show`,`is_del`,`create_time`,`update_time`)
SELECT @jk_root_id,'异常收货处理',NULL,'admin:jk:receive:exception:list',
       '/operation/jzk/receiveException','C',72,1,0,@now,@now
WHERE @receive_exception_menu_id IS NULL;

SET @receive_exception_menu_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE (`path`='/operation/jzk/receiveException'
      OR `perms`='admin:jk:receive:exception:list')
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);

UPDATE `eb_system_menu`
SET `pid`=@jk_root_id,
    `name`='异常收货处理',
    `perms`='admin:jk:receive:exception:list',
    `path`='/operation/jzk/receiveException',
    `menu_type`='C',
    `sort`=72,
    `is_show`=1,
    `is_del`=0,
    `update_time`=@now
WHERE `id`=@receive_exception_menu_id;

SET @receive_exception_action_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `perms`='admin:jk:receive:exception:handle'
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);

INSERT INTO `eb_system_menu`
(`pid`,`name`,`icon`,`perms`,`path`,`menu_type`,`sort`,`is_show`,`is_del`,`create_time`,`update_time`)
SELECT @receive_exception_menu_id,'处理异常收货',NULL,
       'admin:jk:receive:exception:handle','','A',1,1,0,@now,@now
WHERE @receive_exception_action_id IS NULL;

SET @receive_exception_action_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `perms`='admin:jk:receive:exception:handle'
  ORDER BY `is_del` ASC, `id` ASC LIMIT 1
);

UPDATE `eb_system_menu`
SET `pid`=@receive_exception_menu_id,
    `name`='处理异常收货',
    `path`='',
    `menu_type`='A',
    `sort`=1,
    `is_show`=1,
    `is_del`=0,
    `update_time`=@now
WHERE `id`=@receive_exception_action_id;

-- ============================================================================
-- 三、执行后核对
-- ============================================================================

SELECT `id`,`pid`,`name`,`perms`,`path`,`menu_type`,`is_show`,`is_del`
FROM `eb_system_menu`
WHERE `perms` IN (
  'admin:jk:receive:exception:list',
  'admin:jk:receive:exception:handle'
)
ORDER BY `pid`,`id`;

SELECT `dict_type`,`item_code`,`item_label`,`item_tag`,`status`,`is_deleted`
FROM `jk_dict_item`
WHERE (`dict_type` IN ('platform_order_status','stock_transfer_status') AND `item_code`='RECEIVE_EXCEPTION')
   OR (`dict_type`='receive_status' AND `item_code` IN ('UNRECEIVED','EXCEPTION'))
ORDER BY `dict_type`,`sort`,`id`;

-- 预期：
-- 1. 菜单查询返回一条 C 页面和一条 A 按钮；
-- 2. 字典查询返回 4 条有效记录；
-- 3. 普通管理员若仍看不到页面，请在后台角色管理中分配页面和按钮权限，
--    然后重新登录以刷新 authority 缓存。
