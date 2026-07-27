-- 九州康平台订货与库存调拨物流字段升级（MySQL 5.7）
-- 执行前请备份数据库；重复执行前先确认字段是否已存在。

ALTER TABLE `jk_platform_order`
  ADD COLUMN `logistics_company` varchar(100) DEFAULT NULL COMMENT '物流公司或SELF_PICKUP' AFTER `logistics_status`,
  ADD COLUMN `logistics_no` varchar(100) DEFAULT NULL COMMENT '物流单号或SELF_PICKUP' AFTER `logistics_company`,
  ADD COLUMN `shipping_time` datetime DEFAULT NULL COMMENT '实际发货时间' AFTER `logistics_no`;

ALTER TABLE `jk_stock_transfer`
  ADD COLUMN `logistics_company` varchar(100) DEFAULT NULL COMMENT '物流公司或SELF_PICKUP' AFTER `receive_status`,
  ADD COLUMN `logistics_no` varchar(100) DEFAULT NULL COMMENT '物流单号或SELF_PICKUP' AFTER `logistics_company`,
  ADD COLUMN `shipping_time` datetime DEFAULT NULL COMMENT '实际拨货时间' AFTER `logistics_no`;

-- 审计：已发货/已拨货但缺少物流信息的历史单据。
SELECT id, platform_order_no, status, logistics_company, logistics_no, shipping_time
FROM jk_platform_order
WHERE is_deleted = 0 AND status IN ('SHIPPED','STOCK_IN')
  AND (logistics_company IS NULL OR logistics_no IS NULL OR shipping_time IS NULL);

SELECT id, transfer_no, status, logistics_company, logistics_no, shipping_time
FROM jk_stock_transfer
WHERE is_deleted = 0 AND status IN ('TRANSFERRED','STOCK_IN')
  AND (logistics_company IS NULL OR logistics_no IS NULL OR shipping_time IS NULL);
