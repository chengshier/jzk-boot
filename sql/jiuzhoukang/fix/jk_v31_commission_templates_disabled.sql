-- 九州康 V3.1：创客、合伙人、区县代理佣金能力模板（默认关闭）
--
-- 说明：
-- 1. 本脚本只初始化可识别的角色佣金模板，不填写生产比例，不自动启用；
-- 2. status=0 表示未发布，当前业务事件仍可记录业绩，但不得生成可提现佣金；
-- 3. 调拨线下价差属于 OFFLINE_REALIZED 经营收益，不得使用本模板重复发放；
-- 4. 第二批规则引擎完成并经过试算、审核后，才能复制模板创建正式生效版本；
-- 5. 本脚本可重复执行，不修改已有规则状态。

SET @now = NOW();

-- 创客：直属推荐、自营销售、团队管理、阶梯奖励。
INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,
 `effective_time`,`expire_time`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_RETAIL_DIRECT_MAKER','创客直属推荐佣金模板',1,'RETAIL_ORDER','maker',NULL,0,
       NULL,NULL,0,
       '{"template":true,"rewardType":"RETAIL_DIRECT_RECOMMEND","beneficiaryType":"DIRECT_PARENT_SNAPSHOT","baseType":"ITEM_PAID_AMOUNT","calculationType":"PERCENT","rate":null,"stackPolicy":"MAX_ONE","enabled":false}',
       'V3.1 可配置能力模板，默认关闭；不得直接作为生产比例',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_RETAIL_DIRECT_MAKER' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_SELF_SALES_MAKER','创客自营销售平台奖励模板',1,'RETAIL_SALE','maker',NULL,0,0,
       '{"template":true,"rewardType":"RETAIL_SELF_SALES_INCENTIVE","beneficiaryType":"SELLER_SNAPSHOT","baseType":"ITEM_PAID_AMOUNT","calculationType":"PERCENT","rate":null,"stackPolicy":"MAX_ONE","enabled":false}',
       '线上或核验后的线下终端销售，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_SELF_SALES_MAKER' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_TEAM_MANAGE_MAKER','创客团队管理奖励模板',1,'PERFORMANCE_PERIOD','maker',NULL,0,0,
       '{"template":true,"rewardType":"MAKER_TEAM_MANAGEMENT","beneficiaryType":"PERFORMANCE_OWNER","baseType":"VALID_PERFORMANCE_AMOUNT","calculationType":"TIER_PERCENT","tierRuleId":null,"stackPolicy":"MAX_ONE","enabled":false}',
       '只统计有效终端销售业绩，不能按单纯发展人数发放，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_TEAM_MANAGE_MAKER' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_TIER_MAKER','创客阶梯奖励模板',1,'PERFORMANCE_PERIOD','maker',NULL,0,0,
       '{"template":true,"rewardType":"ROLE_TIER_BONUS","beneficiaryType":"PERFORMANCE_OWNER","baseType":"VALID_PERFORMANCE_AMOUNT","calculationType":"TIER_PERCENT","tierRuleId":null,"stackPolicy":"MAX_ONE","enabled":false}',
       '周期阶梯奖励模板，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_TIER_MAKER' AND `rule_version`=1);

-- 合伙人：直属推荐、自营销售、团队管理、阶梯奖励。
INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_RETAIL_DIRECT_PARTNER','合伙人直属推荐佣金模板',1,'RETAIL_ORDER','partner',NULL,0,0,
       '{"template":true,"rewardType":"RETAIL_DIRECT_RECOMMEND","beneficiaryType":"DIRECT_PARENT_SNAPSHOT","baseType":"ITEM_PAID_AMOUNT","calculationType":"PERCENT","rate":null,"stackPolicy":"MAX_ONE","enabled":false}',
       'V3.1 可配置能力模板，默认关闭；合伙人比例不得写死为高于创客',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_RETAIL_DIRECT_PARTNER' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_SELF_SALES_PARTNER','合伙人自营销售平台奖励模板',1,'RETAIL_SALE','partner',NULL,0,0,
       '{"template":true,"rewardType":"RETAIL_SELF_SALES_INCENTIVE","beneficiaryType":"SELLER_SNAPSHOT","baseType":"ITEM_PAID_AMOUNT","calculationType":"PERCENT","rate":null,"stackPolicy":"MAX_ONE","enabled":false}',
       '线上或核验后的线下终端销售，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_SELF_SALES_PARTNER' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_TEAM_MANAGE_PARTNER','合伙人团队管理奖励模板',1,'PERFORMANCE_PERIOD','partner',NULL,0,0,
       '{"template":true,"rewardType":"PARTNER_TEAM_MANAGEMENT","beneficiaryType":"PERFORMANCE_OWNER","baseType":"VALID_PERFORMANCE_AMOUNT","calculationType":"TIER_PERCENT","tierRuleId":null,"stackPolicy":"MAX_ONE","enabled":false}',
       '只统计有效终端销售业绩，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_TEAM_MANAGE_PARTNER' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_TIER_PARTNER','合伙人阶梯奖励模板',1,'PERFORMANCE_PERIOD','partner',NULL,0,0,
       '{"template":true,"rewardType":"ROLE_TIER_BONUS","beneficiaryType":"PERFORMANCE_OWNER","baseType":"VALID_PERFORMANCE_AMOUNT","calculationType":"TIER_PERCENT","tierRuleId":null,"stackPolicy":"MAX_ONE","enabled":false}',
       '周期阶梯奖励模板，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_TIER_PARTNER' AND `rule_version`=1);

-- 区县代理：直属推荐、区域管理、订货补贴、调拨平台补贴、阶梯奖励。
INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_RETAIL_DIRECT_COUNTY','区县代理直属推荐佣金模板',1,'RETAIL_ORDER','county_agent',NULL,0,0,
       '{"template":true,"rewardType":"RETAIL_DIRECT_RECOMMEND","beneficiaryType":"DIRECT_PARENT_SNAPSHOT","baseType":"ITEM_PAID_AMOUNT","calculationType":"PERCENT","rate":null,"stackGroup":"RETAIL_MANAGEMENT","stackPolicy":"MAX_ONE","enabled":false}',
       '与区域管理奖励是否叠加必须显式配置，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_RETAIL_DIRECT_COUNTY' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_REGION_MANAGE_COUNTY','区县代理区域管理奖励模板',1,'RETAIL_SALE','county_agent',NULL,0,0,
       '{"template":true,"rewardType":"COUNTY_REGION_MANAGEMENT","beneficiaryType":"COUNTY_AGENT_SNAPSHOT","baseType":"ITEM_PAID_AMOUNT","calculationType":"PERCENT","rate":null,"stackGroup":"RETAIL_MANAGEMENT","stackPolicy":"MAX_ONE","enabled":false}',
       '仅统计本区域有效终端零售，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_REGION_MANAGE_COUNTY' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_PURCHASE_SUBSIDY_COUNTY','区县代理平台订货补贴模板',1,'PLATFORM_ORDER','county_agent',NULL,0,0,
       '{"template":true,"rewardType":"PLATFORM_PURCHASE_SUBSIDY","beneficiaryType":"PURCHASER_SNAPSHOT","baseType":"PLATFORM_ORDER_AMOUNT","calculationType":"PERCENT","rate":null,"stackPolicy":"MAX_ONE","enabled":false}',
       '平台另付采购活动补贴，默认关闭；正常订货只记录库存成本和订货业绩',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_PURCHASE_SUBSIDY_COUNTY' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_TRANSFER_SUBSIDY_COUNTY','区县代理调拨平台补贴模板',1,'STOCK_TRANSFER','county_agent',NULL,0,0,
       '{"template":true,"rewardType":"TRANSFER_PLATFORM_SUBSIDY","beneficiaryType":"TRANSFER_SENDER_SNAPSHOT","baseType":"TRANSFER_AMOUNT","calculationType":"PERCENT","rate":null,"incomeNature":"PLATFORM_PAYABLE","stackPolicy":"MAX_ONE","enabled":false}',
       '平台另付调拨补贴，默认关闭；线下已实现调拨价差不得重复计佣',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_TRANSFER_SUBSIDY_COUNTY' AND `rule_version`=1);

INSERT INTO `jk_commission_rule`
(`rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`region_code`,`status`,`freeze_days`,`rule_config_json`,`remark`,`is_deleted`,`create_time`,`update_time`,`version`)
SELECT 'TPL_TIER_COUNTY','区县代理区域阶梯奖励模板',1,'PERFORMANCE_PERIOD','county_agent',NULL,0,0,
       '{"template":true,"rewardType":"ROLE_TIER_BONUS","beneficiaryType":"PERFORMANCE_OWNER","baseType":"VALID_PERFORMANCE_AMOUNT","calculationType":"TIER_PERCENT","tierRuleId":null,"stackPolicy":"MAX_ONE","enabled":false}',
       '周期区域阶梯奖励模板，默认关闭',0,@now,@now,0
WHERE NOT EXISTS (SELECT 1 FROM `jk_commission_rule` WHERE `rule_no`='TPL_TIER_COUNTY' AND `rule_version`=1);

-- 核对：所有 V3.1 模板必须保持关闭，且不应存在生产比例。
SELECT `rule_no`,`rule_name`,`rule_version`,`source_type`,`receiver_role_code`,`status`,
       JSON_EXTRACT(`rule_config_json`,'$.rewardType') AS `reward_type`,
       JSON_EXTRACT(`rule_config_json`,'$.rate') AS `configured_rate`
FROM `jk_commission_rule`
WHERE `rule_no` LIKE 'TPL_%'
ORDER BY `receiver_role_code`,`rule_no`;
