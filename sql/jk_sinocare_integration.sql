-- 三诺爱看 CGM 云端 API 对接。仅接受由本平台三方 H5 授权时签发的 unique_id。
CREATE TABLE IF NOT EXISTS `jk_sinocare_authorization` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `unique_id` varchar(64) NOT NULL COMMENT '服务商签发并传给三诺的用户标识',
  `user_id` bigint NOT NULL COMMENT '本地用户ID',
  `status` varchar(16) NOT NULL COMMENT 'AUTHORIZED/REVOKED',
  `authorized_at` datetime DEFAULT NULL,
  `revoked_at` datetime DEFAULT NULL,
  `source_event_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_jk_sinocare_unique_id` (`unique_id`), KEY `idx_jk_sinocare_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三诺授权与本地用户映射';

CREATE TABLE IF NOT EXISTS `jk_sinocare_callback_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_type` varchar(24) NOT NULL COMMENT '1001/1002/1003/1004/1005',
  `event_id` varchar(64) DEFAULT NULL COMMENT '解密后业务主键',
  `unique_id` varchar(64) DEFAULT NULL,
  `payload_cipher` longtext NOT NULL COMMENT '原始密文信封，应用层加密后保存',
  `signature` text NOT NULL,
  `process_status` varchar(16) NOT NULL COMMENT 'RECEIVED/PROCESSING/SUCCESS/UNMATCHED/FAILED',
  `error_message` varchar(500) DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_jk_sinocare_event` (`event_type`,`event_id`), KEY `idx_jk_sinocare_unique` (`unique_id`,`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三诺加密回调审计日志';

CREATE TABLE IF NOT EXISTS `jk_sinocare_device_session` (
 `id` bigint NOT NULL AUTO_INCREMENT, `unique_id` varchar(64) NOT NULL, `device_sn` varchar(32) NOT NULL,
 `status` tinyint NOT NULL COMMENT '1监测中 2已结束', `product_name` varchar(64) DEFAULT NULL,
 `detection_start_time` datetime DEFAULT NULL, `detection_end_time` datetime DEFAULT NULL,
 `create_time` datetime NOT NULL, `update_time` datetime NOT NULL, PRIMARY KEY (`id`), UNIQUE KEY `uk_sino_device_session` (`unique_id`,`device_sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三诺设备监测周期';

CREATE TABLE IF NOT EXISTS `jk_sinocare_report` (
 `id` bigint NOT NULL AUTO_INCREMENT, `event_id` varchar(64) NOT NULL, `unique_id` varchar(64) NOT NULL, `device_sn` varchar(32) DEFAULT NULL,
 `report_type` varchar(16) NOT NULL COMMENT 'DIGITAL/PDF', `payload_cipher` longtext NOT NULL, `create_time` datetime NOT NULL, `update_time` datetime NOT NULL,
 PRIMARY KEY (`id`), UNIQUE KEY `uk_sino_report` (`report_type`,`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三诺数字和PDF健康报告';

-- 三诺资料模板填写的相对路径（生产/测试域名单独填写）：
-- 1001 /api/front/jk/health/sinocare/authorization
-- 1002 /api/front/jk/health/sinocare/device
-- 1003 /api/front/jk/health/sinocare/cgm
-- 1004 /api/front/jk/health/sinocare/report
-- 1005 /api/front/jk/health/sinocare/report-file
