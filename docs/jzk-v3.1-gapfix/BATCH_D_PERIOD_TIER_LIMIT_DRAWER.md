# V3.1 补漏批次 D：周期业绩、阶梯奖励、封顶预算与关联解释

## 1. 修改文件清单

### 后端模型

- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkPerformancePeriod.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkPerformancePeriodItem.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkTierRule.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkTierRuleItem.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkPeriodRewardRecord.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkCommissionLimitUsage.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkCommissionLimitReservation.java`

### 后端 DAO

- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkPerformancePeriodDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkPerformancePeriodItemDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkTierRuleDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkTierRuleItemDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkPeriodRewardRecordDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkCommissionLimitUsageDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkCommissionLimitReservationDao.java`

### 后端请求、服务与控制器

- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkPerformancePeriodBuildRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkPerformancePeriodCloseRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkCommissionRuleTrialRequest.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/performance/JkPerformancePeriodService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/performance/JkPerformancePeriodServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/commission/JkCommissionLimitService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/JkCommissionLimitServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/CommissionScenarioServiceImpl.java`
- `crmeb-admin/src/main/java/com/zbkj/admin/controller/jiuzhoukang/JkPerformancePeriodAdminController.java`
- `crmeb-common/src/main/java/com/zbkj/common/constants/jiuzhoukang/JkV31PermissionCodes.java`

### 管理端

- `jzk-vue/src/api/jkGapfix.js`
- `jzk-vue/src/views/jkBusiness/performancePeriod/index.vue`
- `jzk-vue/src/components/jk/JkBusinessDrawer.vue`
- `jzk-vue/src/router/modules/jkBusiness.js`

## 2. SQL 清单

- `sql/jiuzhoukang/v3.1-gapfix/04-jk-v31-period-tier-limit.sql`

新增表：

- `jk_performance_period`
- `jk_performance_period_item`
- `jk_tier_rule`
- `jk_tier_rule_item`
- `jk_period_reward_record`
- `jk_commission_limit_usage`
- `jk_commission_limit_reservation`

## 3. 接口清单

- `GET /api/admin/jk/performance-period/list`
- `GET /api/admin/jk/performance-period/{id}`
- `POST /api/admin/jk/performance-period/build`
- `POST /api/admin/jk/performance-period/{id}/trial`
- `POST /api/admin/jk/performance-period/{id}/close`
- `GET /api/admin/jk/context/overview`

## 4. 页面清单

- 周期业绩与阶梯奖励页面；
- 周期构建、成员贡献、来源明细、奖励试算、审核关闭和关闭后锁定；
- 构建条件使用商业方案、身份和区域选项，不要求运营手填数据库用户 ID；
- 公共业务抽屉保留归属、业绩、佣金、退款和调整上下文；
- 明确试算与真实发奖的区别。

## 5. 自动化检查结果

- 周期只纳入 `RETAIL_ORDER` 与 `OFFLINE_SALE`，排除 `PLATFORM_ORDER` 和 `STOCK_TRANSFER`；
- 周期关闭前必须试算进入 `PENDING_REVIEW`；
- 关闭后状态为 `CLOSED`，禁止直接重算覆盖；
- 真实分发前调用限额服务；
- `action_key` 防止重试重复消耗封顶和预算；
- `usage_type + rule_id + user_id + period_key` 唯一占用行配合 `FOR UPDATE`；
- 规则总预算公共占用行使用 `user_id=0`；
- 只有 `PLATFORM_PAYABLE` 参与佣金与提现限额，`OFFLINE_REALIZED` 不参与。

以上为开发自检，不能替代真实 SQL 和并发测试。

## 6. 未完成的真实环境验证

- MySQL 5.7 执行周期和限额表升级；
- 大量真实零售、线下销售和部分退款数据核对周期净业绩；
- 同一规则、同一受益人、同一周期最后一笔封顶并发；
- 规则总预算最后额度并发；
- 关闭事务中佣金写入失败时的完整回滚；
- 已关闭周期后退款冲正和补偿周期；
- 财务对账和全量 UAT。

## 7. PR 状态

后端 PR 已记录周期业绩、限额预算和真实验证边界；管理端 PR 已记录页面和公共抽屉；小程序本批次无运营配置入口。三个 PR 继续保持 Draft。
