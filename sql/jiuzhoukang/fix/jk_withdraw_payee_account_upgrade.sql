-- 九州康提现收款账户安全升级（MySQL 5.7）
-- 执行前提：先备份数据库；本脚本只新增表，不修改历史提现金额和状态。
-- 上线前必须在系统配置中新增：jk_sensitive_data_secret
-- 要求：长度至少16位，生产环境随机生成并妥善备份；密钥变更会导致历史银行卡快照无法解密。
-- 新数据使用 AES-GCM 认证加密；v1 AES-CBC 仅用于读取本修复分支早期测试密文。

CREATE TABLE IF NOT EXISTS `jk_withdraw_payee_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '前台用户ID',
  `account_type` varchar(20) NOT NULL DEFAULT 'BANK' COMMENT '收款账户类型，当前仅BANK',
  `account_name` varchar(64) NOT NULL COMMENT '收款人姓名',
  `bank_name` varchar(128) NOT NULL COMMENT '开户银行',
  `bank_account_cipher` varchar(512) NOT NULL COMMENT '银行卡号AES-GCM密文',
  `bank_account_hash` char(64) NOT NULL COMMENT '标准化银行卡号SHA-256，用于本人账户去重',
  `bank_account_mask` varchar(64) NOT NULL COMMENT '银行卡号掩码',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认账户',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '软删除',
  `version` int(11) NOT NULL DEFAULT '0' COMMENT '乐观锁版本',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_jk_payee_user_status` (`user_id`,`status`,`is_deleted`),
  KEY `idx_jk_payee_user_hash` (`user_id`,`bank_account_hash`,`is_deleted`),
  KEY `idx_jk_payee_user_default` (`user_id`,`is_default`,`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='九州康提现收款账户';

-- 审计：同一用户不应存在多个有效默认账户。
SELECT user_id, COUNT(*) AS default_count
FROM jk_withdraw_payee_account
WHERE is_deleted = 0 AND status = 1 AND is_default = 1
GROUP BY user_id
HAVING COUNT(*) > 1;

-- 审计：新版本提现快照不应继续保存前端拼装的明文 bankAccount。
-- 历史数据只查询，不自动覆盖；需按实际情况人工核对。
SELECT id, withdraw_no, user_id, create_time
FROM jk_withdraw_apply
WHERE is_deleted = 0
  AND payee_snapshot_json IS NOT NULL
  AND payee_snapshot_json LIKE '%"bankAccount"%'
  AND payee_snapshot_json NOT LIKE '%"bankAccountCipher"%';
