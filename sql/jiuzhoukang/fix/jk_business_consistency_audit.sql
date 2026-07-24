-- 九州康商品、库存、价格规则一致性只读审计脚本
-- 说明：本脚本默认只查询，不直接修改生产数据。

-- 1. 价格规则关联的商品不存在、已删除，或 SKU 不存在/不属于所选商品
SELECT
    r.id,
    r.rule_no,
    r.product_id,
    p.store_name,
    p.is_show,
    p.is_del AS product_is_del,
    r.sku_id,
    s.product_id AS sku_product_id,
    s.suk,
    s.`unique` AS sku_code,
    s.is_del AS sku_is_del,
    r.status
FROM jk_product_price_rule r
LEFT JOIN eb_store_product p ON p.id = r.product_id
LEFT JOIN eb_store_product_attr_value s ON s.id = r.sku_id
WHERE r.is_deleted = 0
  AND (
      p.id IS NULL
      OR p.is_del = 1
      OR (
          r.sku_id IS NOT NULL
          AND (s.id IS NULL OR s.is_del = 1 OR s.product_id <> r.product_id)
      )
  )
ORDER BY r.id DESC;

-- 2. 九州康库存明细关联的商品/SKU 不存在或关系错误
SELECT
    i.id,
    i.stock_account_id,
    i.product_id,
    p.store_name,
    i.sku_id,
    i.sku_code,
    s.product_id AS sku_product_id,
    s.suk,
    i.available_qty,
    i.frozen_qty
FROM jk_stock_item i
LEFT JOIN eb_store_product p ON p.id = i.product_id
LEFT JOIN eb_store_product_attr_value s ON s.id = i.sku_id
WHERE i.is_deleted = 0
  AND (
      p.id IS NULL
      OR p.is_del = 1
      OR (i.sku_id IS NOT NULL AND (s.id IS NULL OR s.is_del = 1 OR s.product_id <> i.product_id))
  )
ORDER BY i.id DESC;

-- 3. 同一库存主体、商品、SKU 出现重复库存明细
SELECT
    stock_account_id,
    product_id,
    IFNULL(sku_id, 0) AS normalized_sku_id,
    COUNT(*) AS duplicate_count,
    SUM(available_qty) AS available_qty,
    SUM(frozen_qty) AS frozen_qty
FROM jk_stock_item
WHERE is_deleted = 0
GROUP BY stock_account_id, product_id, IFNULL(sku_id, 0)
HAVING COUNT(*) > 1;

-- 4. 库存总账和批次账可用/冻结数量不一致
SELECT
    i.id AS stock_item_id,
    i.stock_account_id,
    i.product_id,
    i.sku_id,
    i.available_qty AS item_available_qty,
    IFNULL(SUM(b.available_qty), 0) AS batch_available_qty,
    i.frozen_qty AS item_frozen_qty,
    IFNULL(SUM(b.frozen_qty), 0) AS batch_frozen_qty
FROM jk_stock_item i
LEFT JOIN jk_stock_batch b
       ON b.stock_account_id = i.stock_account_id
      AND b.product_id = i.product_id
      AND IFNULL(b.sku_id, 0) = IFNULL(i.sku_id, 0)
      AND b.is_deleted = 0
WHERE i.is_deleted = 0
GROUP BY i.id, i.stock_account_id, i.product_id, i.sku_id, i.available_qty, i.frozen_qty
HAVING i.available_qty <> IFNULL(SUM(b.available_qty), 0)
    OR i.frozen_qty <> IFNULL(SUM(b.frozen_qty), 0);

-- 5. 平台九州康库存与 CRMEB SKU 库存差异，仅用于识别当前双库存状态
SELECT
    a.id AS platform_account_id,
    i.product_id,
    p.store_name,
    i.sku_id,
    s.suk,
    s.stock AS crmeb_sku_stock,
    i.available_qty AS jk_available_qty,
    i.frozen_qty AS jk_frozen_qty,
    (i.available_qty + i.frozen_qty) AS jk_total_qty,
    (IFNULL(s.stock, 0) - IFNULL(i.available_qty, 0) - IFNULL(i.frozen_qty, 0)) AS difference_qty
FROM jk_stock_account a
JOIN jk_stock_item i ON i.stock_account_id = a.id AND i.is_deleted = 0
LEFT JOIN eb_store_product p ON p.id = i.product_id
LEFT JOIN eb_store_product_attr_value s ON s.id = i.sku_id
WHERE a.account_type = 'PLATFORM'
  AND a.is_deleted = 0
ORDER BY ABS(IFNULL(s.stock, 0) - IFNULL(i.available_qty, 0) - IFNULL(i.frozen_qty, 0)) DESC;

-- 6. 批次初始化执行前检查：存在冻结库存时禁止直接执行 openingFromStockItems
SELECT
    COUNT(*) AS frozen_item_count,
    IFNULL(SUM(frozen_qty), 0) AS total_frozen_qty
FROM jk_stock_item
WHERE is_deleted = 0
  AND frozen_qty > 0;

-- 7. 用户身份关联的用户、角色、区域不存在
SELECT
    ubr.id,
    ubr.user_id,
    u.nickname,
    ubr.role_code,
    br.role_name,
    ubr.region_code,
    rg.region_name,
    ubr.audit_status,
    ubr.freeze_status
FROM jk_user_business_role ubr
LEFT JOIN eb_user u ON u.uid = ubr.user_id
LEFT JOIN jk_business_role br ON br.role_code = ubr.role_code AND br.is_deleted = 0
LEFT JOIN jk_region rg ON rg.region_code = ubr.region_code AND rg.is_deleted = 0
WHERE ubr.is_deleted = 0
  AND (u.uid IS NULL OR br.id IS NULL OR (ubr.region_code IS NOT NULL AND rg.id IS NULL));

-- 修复原则：
-- 1. 先备份异常记录；
-- 2. 商品/SKU 无法恢复时，优先禁用配置，不直接删除历史单据；
-- 3. 库存数量差异必须结合真实仓库盘点和业务流水处理，禁止直接以任意一侧覆盖；
-- 4. 平台库存主账切换前必须回归普通零售、平台订货、取消释放、发货和退款流程。