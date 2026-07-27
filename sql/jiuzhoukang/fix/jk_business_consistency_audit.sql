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
      OR (r.sku_id IS NOT NULL AND (s.id IS NULL OR s.is_del = 1 OR s.product_id <> r.product_id))
  );

-- 2. CRMEB SKU 库存与九州康 PLATFORM 镜像库存对照
SELECT
    a.id AS stock_account_id,
    a.account_no,
    i.id AS stock_item_id,
    i.product_id,
    i.sku_id,
    p.store_name,
    s.suk,
    s.stock AS crmeb_available_stock,
    i.available_qty AS jk_mirror_available_qty,
    i.frozen_qty AS jk_frozen_qty,
    (COALESCE(i.available_qty, 0) + COALESCE(i.frozen_qty, 0)) AS jk_total_qty,
    (COALESCE(s.stock, 0) - COALESCE(i.available_qty, 0)) AS available_difference
FROM jk_stock_account a
JOIN jk_stock_item i ON i.stock_account_id = a.id AND i.is_deleted = 0
LEFT JOIN eb_store_product p ON p.id = i.product_id
LEFT JOIN eb_store_product_attr_value s ON s.id = i.sku_id
WHERE a.account_type = 'PLATFORM'
  AND a.is_deleted = 0
ORDER BY ABS(COALESCE(s.stock, 0) - COALESCE(i.available_qty, 0)) DESC;

-- 3. 商品总库存与 SKU 库存汇总对照
SELECT
    p.id AS product_id,
    p.store_name,
    p.stock AS product_stock,
    SUM(CASE WHEN s.is_del = 0 THEN s.stock ELSE 0 END) AS sku_stock_sum,
    p.stock - SUM(CASE WHEN s.is_del = 0 THEN s.stock ELSE 0 END) AS difference
FROM eb_store_product p
LEFT JOIN eb_store_product_attr_value s ON s.product_id = p.id
WHERE p.is_del = 0
GROUP BY p.id, p.store_name, p.stock
HAVING difference <> 0;

-- 4. 代理库存明细与批次可用、冻结汇总不一致
SELECT
    i.id AS stock_item_id,
    a.account_type,
    a.owner_user_id,
    i.product_id,
    i.sku_id,
    i.available_qty AS item_available_qty,
    i.frozen_qty AS item_frozen_qty,
    COALESCE(SUM(CASE WHEN b.is_deleted = 0 THEN b.available_qty ELSE 0 END), 0) AS batch_available_qty,
    COALESCE(SUM(CASE WHEN b.is_deleted = 0 THEN b.frozen_qty ELSE 0 END), 0) AS batch_frozen_qty
FROM jk_stock_item i
JOIN jk_stock_account a ON a.id = i.stock_account_id AND a.is_deleted = 0
LEFT JOIN jk_stock_batch b ON b.stock_account_id = i.stock_account_id
    AND b.product_id = i.product_id
    AND ((b.sku_id = i.sku_id) OR (b.sku_id IS NULL AND i.sku_id IS NULL))
WHERE i.is_deleted = 0
  AND a.account_type <> 'PLATFORM'
GROUP BY i.id, a.account_type, a.owner_user_id, i.product_id, i.sku_id, i.available_qty, i.frozen_qty
HAVING item_available_qty <> batch_available_qty
    OR item_frozen_qty <> batch_frozen_qty;

-- 5. 库存流水缺少商品、SKU、账户或业务单号
SELECT
    f.id,
    f.flow_no,
    f.business_type,
    f.business_no,
    f.stock_account_id,
    f.product_id,
    f.sku_id,
    a.id AS account_exists,
    p.id AS product_exists,
    s.id AS sku_exists
FROM jk_stock_flow f
LEFT JOIN jk_stock_account a ON a.id = f.stock_account_id AND a.is_deleted = 0
LEFT JOIN eb_store_product p ON p.id = f.product_id
LEFT JOIN eb_store_product_attr_value s ON s.id = f.sku_id
WHERE f.is_deleted = 0
  AND (
      a.id IS NULL
      OR p.id IS NULL
      OR (f.sku_id IS NOT NULL AND (s.id IS NULL OR s.product_id <> f.product_id))
      OR f.business_no IS NULL
      OR TRIM(f.business_no) = ''
  );

-- 6. 生效身份缺少 CRMEB 用户、业务角色或区域
SELECT
    ur.id,
    ur.user_id,
    u.nickname,
    u.phone,
    ur.role_code,
    br.role_name,
    ur.region_code,
    r.region_name,
    ur.audit_status,
    ur.effective_status,
    ur.freeze_status
FROM jk_user_business_role ur
LEFT JOIN eb_user u ON u.uid = ur.user_id
LEFT JOIN jk_business_role br ON br.role_code = ur.role_code AND br.is_deleted = 0
LEFT JOIN jk_region r ON r.region_code = ur.region_code AND r.is_deleted = 0
WHERE ur.is_deleted = 0
  AND ur.audit_status = 'EFFECTIVE'
  AND (u.uid IS NULL OR br.id IS NULL OR (ur.region_code IS NOT NULL AND r.id IS NULL));

-- 7. 同一用户存在多个有效主身份
SELECT
    user_id,
    COUNT(*) AS primary_role_count,
    GROUP_CONCAT(role_code ORDER BY id) AS role_codes
FROM jk_user_business_role
WHERE is_deleted = 0
  AND is_primary = 1
  AND audit_status = 'EFFECTIVE'
  AND effective_status = 'ENABLED'
  AND status = 1
GROUP BY user_id
HAVING primary_role_count > 1;

-- 8. 同一用户存在多个有效直属关系
SELECT
    user_id,
    COUNT(*) AS active_relation_count,
    GROUP_CONCAT(parent_user_id ORDER BY id) AS parent_user_ids
FROM jk_agent_relation
WHERE is_deleted = 0
  AND status = 1
GROUP BY user_id
HAVING active_relation_count > 1;

-- 9. 换绑申请审核前阻断项概览
SELECT
    a.id,
    a.apply_no,
    a.user_id,
    a.status,
    (SELECT COUNT(*) FROM jk_stock_transfer t
      WHERE t.user_id = a.user_id AND t.is_deleted = 0
        AND t.status NOT IN ('STOCK_IN', 'CLOSED', 'CANCELLED', 'AUDIT_REJECTED')) AS active_transfer_count,
    (SELECT COALESCE(SUM(i.available_qty + i.frozen_qty), 0)
       FROM jk_stock_account sa
       JOIN jk_stock_item i ON i.stock_account_id = sa.id AND i.is_deleted = 0
      WHERE sa.owner_user_id = a.user_id AND sa.status = 1 AND sa.is_deleted = 0) AS stock_balance_qty,
    (SELECT COALESCE(SUM(c.pending_settle_amount), 0)
       FROM jk_commission_account c
      WHERE c.user_id = a.user_id AND c.is_deleted = 0) AS pending_commission_amount,
    (SELECT COUNT(*) FROM jk_withdraw_apply w
      WHERE w.user_id = a.user_id AND w.is_deleted = 0
        AND w.status IN ('SUBMITTED', 'AUDITING', 'APPROVED')) AS processing_withdraw_count
FROM jk_agent_relation_change_apply a
WHERE a.is_deleted = 0
  AND a.status = 'PENDING';

-- 10. 团队成员贡献口径检查
-- 当前 jk_commission_record 没有 source_user_id / buyer_user_id 等下单时来源成员快照，
-- 因此不能准确按团队成员统计“为上级贡献的佣金”。在新增快照字段前，App 必须显示不可核算状态。
SELECT
    COUNT(*) AS commission_record_count,
    SUM(CASE WHEN receiver_user_id IS NULL THEN 1 ELSE 0 END) AS missing_receiver_count
FROM jk_commission_record
WHERE is_deleted = 0;
