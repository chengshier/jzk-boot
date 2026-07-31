-- 九州康 JZK V3.1 第三批补丁：订阅消息任务真实发送字段与权限（MySQL 5.7）
SET @db = DATABASE();
SET @now = NOW();

SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='jk_subscription_task' AND COLUMN_NAME='template_id')=0,
  'ALTER TABLE jk_subscription_task ADD COLUMN template_id varchar(128) DEFAULT NULL AFTER template_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='jk_subscription_task' AND COLUMN_NAME='recipient_open_id')=0,
  'ALTER TABLE jk_subscription_task ADD COLUMN recipient_open_id varchar(128) DEFAULT NULL AFTER receiver_user_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='jk_subscription_task' AND COLUMN_NAME='wechat_message_id')=0,
  'ALTER TABLE jk_subscription_task ADD COLUMN wechat_message_id varchar(128) DEFAULT NULL AFTER sent_at', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='jk_subscription_task' AND COLUMN_NAME='error_code')=0,
  'ALTER TABLE jk_subscription_task ADD COLUMN error_code varchar(64) DEFAULT NULL AFTER wechat_message_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO jk_business_permission(permission_code,permission_name,permission_type,parent_code,status,remark,is_deleted,create_time,update_time)
SELECT 'admin:jk:subscription:task:manage','订阅消息任务执行','BUTTON','admin:jk:subscription:task:list',1,'允许手动处理与重新入队，不代表可以绕过微信授权',0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission WHERE permission_code='admin:jk:subscription:task:manage' AND is_deleted=0);
