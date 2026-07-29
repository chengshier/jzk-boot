-- JZK V3.1 补漏批次 D：周期业绩、阶梯奖励、周期封顶和规则总预算（MySQL 5.7）
-- 只汇总有效线上零售和经核验线下终端销售；平台订货和内部调拨不直接计入团队销售奖励。

CREATE TABLE IF NOT EXISTS jk_performance_period (
  id bigint NOT NULL AUTO_INCREMENT,
  period_no varchar(64) NOT NULL,
  period_type varchar(32) NOT NULL COMMENT 'MONTH/QUARTER/YEAR/CUSTOM',
  start_time datetime NOT NULL,
  end_time datetime NOT NULL,
  plan_id bigint DEFAULT NULL,
  rule_id bigint DEFAULT NULL,
  owner_role_code varchar(64) DEFAULT NULL,
  region_code varchar(64) DEFAULT NULL,
  status varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_REVIEW/CLOSED/REVERSED',
  total_performance_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  total_refund_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  net_performance_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  member_count int NOT NULL DEFAULT 0,
  trial_reward_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  approved_reward_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  snapshot_json longtext,
  request_no varchar(128) NOT NULL,
  created_by bigint NOT NULL,
  closed_by bigint DEFAULT NULL,
  closed_at datetime DEFAULT NULL,
  version int NOT NULL DEFAULT 0,
  is_deleted tinyint(1) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL,
  update_time datetime NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_jk_performance_period_no(period_no),
  UNIQUE KEY uk_jk_performance_period_request(request_no),
  KEY idx_jk_performance_period_window(start_time,end_time,status),
  KEY idx_jk_performance_period_plan(plan_id,rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='有效终端销售周期业绩';

CREATE TABLE IF NOT EXISTS jk_performance_period_item (
  id bigint NOT NULL AUTO_INCREMENT,
  period_id bigint NOT NULL,
  performance_record_id bigint NOT NULL,
  owner_user_id bigint DEFAULT NULL,
  source_user_id bigint DEFAULT NULL,
  source_type varchar(64) NOT NULL,
  source_id bigint DEFAULT NULL,
  source_item_id bigint DEFAULT NULL,
  performance_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  refund_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  net_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  relation_snapshot_json longtext,
  status varchar(32) NOT NULL DEFAULT 'INCLUDED',
  idempotency_key varchar(160) NOT NULL,
  is_deleted tinyint(1) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL,
  update_time datetime NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_jk_performance_period_record(period_id,performance_record_id),
  UNIQUE KEY uk_jk_performance_period_action(idempotency_key),
  KEY idx_jk_performance_period_owner(period_id,owner_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周期业绩来源快照';

CREATE TABLE IF NOT EXISTS jk_tier_rule (
  id bigint NOT NULL AUTO_INCREMENT,
  rule_code varchar(64) NOT NULL,
  rule_name varchar(128) NOT NULL,
  plan_id bigint NOT NULL,
  plan_code varchar(64) NOT NULL,
  plan_version_no int NOT NULL,
  receiver_role_code varchar(64) NOT NULL,
  period_type varchar(32) NOT NULL,
  region_code varchar(64) DEFAULT NULL,
  per_user_period_cap decimal(18,2) DEFAULT NULL,
  total_budget decimal(18,2) DEFAULT NULL,
  priority int NOT NULL DEFAULT 0,
  publish_status varchar(32) NOT NULL DEFAULT 'DRAFT',
  effective_start_time datetime DEFAULT NULL,
  effective_end_time datetime DEFAULT NULL,
  published_by bigint DEFAULT NULL,
  published_at datetime DEFAULT NULL,
  status tinyint(1) NOT NULL DEFAULT 0,
  is_deleted tinyint(1) NOT NULL DEFAULT 0,
  version int NOT NULL DEFAULT 0,
  create_time datetime NOT NULL,
  update_time datetime NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_jk_tier_rule_version(rule_code,plan_version_no,is_deleted),
  KEY idx_jk_tier_rule_publish(publish_status,status,effective_start_time,effective_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶梯奖励规则主档；默认关闭';

CREATE TABLE IF NOT EXISTS jk_tier_rule_item (
  id bigint NOT NULL AUTO_INCREMENT,
  tier_rule_id bigint NOT NULL,
  threshold_min decimal(18,2) NOT NULL,
  threshold_max decimal(18,2) DEFAULT NULL,
  reward_mode varchar(32) NOT NULL COMMENT 'PERCENT/FIXED_PER_PERIOD',
  reward_value decimal(18,6) NOT NULL,
  tier_cap decimal(18,2) DEFAULT NULL,
  sort_no int NOT NULL DEFAULT 0,
  status tinyint(1) NOT NULL DEFAULT 0,
  is_deleted tinyint(1) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL,
  update_time datetime NOT NULL,
  PRIMARY KEY(id),
  KEY idx_jk_tier_rule_item_rule(tier_rule_id,status,threshold_min,sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶梯奖励档位；默认关闭';

CREATE TABLE IF NOT EXISTS jk_period_reward_record (
  id bigint NOT NULL AUTO_INCREMENT,
  reward_no varchar(64) NOT NULL,
  period_id bigint NOT NULL,
  owner_user_id bigint NOT NULL,
  tier_rule_id bigint DEFAULT NULL,
  tier_rule_item_id bigint DEFAULT NULL,
  performance_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  raw_reward_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  approved_reward_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  status varchar(32) NOT NULL COMMENT 'COMMISSION_CREATED/NO_ACTIVE_RULE/REVERSED',
  commission_record_id bigint DEFAULT NULL,
  calculation_snapshot_json longtext,
  request_no varchar(160) NOT NULL,
  is_deleted tinyint(1) NOT NULL DEFAULT 0,
  create_time datetime NOT NULL,
  update_time datetime NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_jk_period_reward_no(reward_no),
  UNIQUE KEY uk_jk_period_reward_request(request_no),
  KEY idx_jk_period_reward_period(period_id,owner_user_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周期业绩关闭奖励结果';

CREATE TABLE IF NOT EXISTS jk_commission_limit_usage (
  id bigint NOT NULL AUTO_INCREMENT,
  usage_type varchar(32) NOT NULL COMMENT 'USER_PERIOD/RULE_BUDGET',
  rule_id bigint NOT NULL,
  user_id bigint NOT NULL DEFAULT 0 COMMENT '规则总预算公共行使用0',
  period_key varchar(32) NOT NULL,
  limit_amount decimal(18,2) NOT NULL,
  used_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  version int NOT NULL DEFAULT 0,
  create_time datetime NOT NULL,
  update_time datetime NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_jk_commission_limit_usage(usage_type,rule_id,user_id,period_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金周期封顶与规则预算占用';

CREATE TABLE IF NOT EXISTS jk_commission_limit_reservation (
  id bigint NOT NULL AUTO_INCREMENT,
  action_key varchar(200) NOT NULL,
  rule_id bigint NOT NULL,
  user_id bigint DEFAULT NULL,
  period_key varchar(32) NOT NULL,
  requested_amount decimal(18,2) NOT NULL,
  approved_amount decimal(18,2) NOT NULL DEFAULT 0.00,
  result_code varchar(32) NOT NULL,
  result_message varchar(255) DEFAULT NULL,
  create_time datetime NOT NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_jk_commission_limit_action(action_key),
  KEY idx_jk_commission_limit_rule(rule_id,user_id,period_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='佣金限额预算幂等占用动作';

SET @now=NOW();
SET @tenant_id=COALESCE((SELECT tenant_id FROM jk_business_permission WHERE tenant_id IS NOT NULL ORDER BY id LIMIT 1),'000000');
INSERT INTO jk_business_permission(permission_code,permission_name,module_code,permission_type,enabled,remark,status,is_deleted,create_user_id,update_user_id,create_time,update_time,tenant_id,create_dept)
SELECT code,name,'PERFORMANCE_INCOME',type,1,remark,1,0,0,0,@now,@now,@tenant_id,0
FROM (
  SELECT 'admin:jk:performance:period:list' code,'周期业绩查询' name,'MENU' type,'查询周期、成员贡献和奖励结果' remark
  UNION ALL SELECT 'admin:jk:performance:period:build','周期业绩构建','BUTTON','只汇总有效终端销售业绩'
  UNION ALL SELECT 'admin:jk:performance:period:trial','周期奖励试算','BUTTON','试算不写佣金'
  UNION ALL SELECT 'admin:jk:performance:period:close','周期审核关闭','BUTTON','关闭后按已发布规则生成真实佣金并锁定'
) seed
WHERE NOT EXISTS(SELECT 1 FROM jk_business_permission p WHERE p.permission_code=seed.code AND p.is_deleted=0);

SET @jk_root_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk' OR name IN ('九州康管理','九州康')) ORDER BY id LIMIT 1);
SET @income_group_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND pid=@jk_root_id AND component='/operation/jzk/group/performance-income' ORDER BY id LIMIT 1);
SET @period_menu_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/performancePeriod' OR perms='admin:jk:performance:period:list') ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @income_group_id,'周期业绩与阶梯奖励',NULL,'admin:jk:performance:period:list','/operation/jzk/performancePeriod','C',95,1,0,@now,@now
WHERE @income_group_id IS NOT NULL AND @period_menu_id IS NULL;
SET @period_menu_id=(SELECT id FROM eb_system_menu WHERE is_delete=0 AND (component='/operation/jzk/performancePeriod' OR perms='admin:jk:performance:period:list') ORDER BY id LIMIT 1);
UPDATE eb_system_menu SET pid=@income_group_id,name='周期业绩与阶梯奖励',component='/operation/jzk/performancePeriod',perms='admin:jk:performance:period:list',is_delete=0,update_time=@now WHERE id=@period_menu_id;
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delete,create_time,update_time)
SELECT @period_menu_id,button_name,NULL,permission_code,'','A',button_sort,1,0,@now,@now
FROM (
  SELECT '构建周期' button_name,'admin:jk:performance:period:build' permission_code,10 button_sort
  UNION ALL SELECT '试算奖励','admin:jk:performance:period:trial',9
  UNION ALL SELECT '审核关闭','admin:jk:performance:period:close',8
) buttons
WHERE @period_menu_id IS NOT NULL
  AND NOT EXISTS(SELECT 1 FROM eb_system_menu m WHERE m.is_delete=0 AND m.perms=buttons.permission_code);

SELECT COUNT(*) AS default_enabled_tier_rules
FROM jk_tier_rule WHERE is_deleted=0 AND status=1 AND publish_status='PUBLISHED';
