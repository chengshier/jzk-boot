# V3.1 补漏批次 D：周期业绩、阶梯奖励、封顶预算与关联解释

## 1. 修改文件清单

### 后端
- `crmeb-common/.../jiuzhoukang/JkPerformancePeriod.java`
- `crmeb-common/.../jiuzhoukang/JkPerformancePeriodItem.java`
- `crmeb-common/.../jiuzhoukang/JkTierRule.java`
- `crmeb-common/.../jiuzhoukang/JkTierRuleItem.java`
- `crmeb-common/.../jiuzhoukang/JkPeriodRewardRecord.java`
- `crmeb-common/.../jiuzhoukang/JkCommissionLimitUsage.java`
- `crmeb-common/.../jiuzhoukang/JkCommissionLimitReservation.java`
- 对应 7 个 DAO
- `crmeb-common/.../request/jiuzhoukang/JkPerformancePeriodBuildRequest.java`
- `crmeb-common/.../request/jiuzhoukang/JkPerformancePeriodCloseRequest.java`
- `crmeb-common/.../request/jiuzhoukang/JkCommissionRuleTrialRequest.java`
- `crmeb-service/.../performance/JkPerformancePeriodService.java`
- `crmeb-service/.../performance/JkPerformancePeriodServiceImpl.java`
- `crmeb-service/.../commission/JkCommissionLimitService.java`
- `crmeb-service/.../commission/JkCommissionLimitServiceImpl.java`
- `crmeb-service/.../commission/CommissionScenarioServiceImpl.java`
- `crmeb-admin/.../JkPerformancePeriodAdminController.java`
- `crmeb-common/.../constants/jiuzhoukang/JkV31PermissionCodes.java`

### 管理端
- `jzk-vue/src/api/jkGapfix.js`
- `jzk-vue/src/views/jkBusiness/performancePeriod/index.vue`
- `jzk-vue/src/router/modules/jkBusiness.js`

## 2. SQL 清单

- `sql/jiuzhoukang/v3.1-gapfix/04-jk-v31-period-tier-limit.sql`

新增：
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

## 4. 页面清单

- 新增“周期业绩与阶梯奖励”页面；
- 支持周期构建、成员贡献、来源明细、奖励试算、审核关闭和关闭后锁定；
- 构建条件使用业务方案、身份和区域选项，不要求运营手填数据库用户 ID；
- 详情抽屉保留周期上下文，明确试算与真实发奖的区别。

## 5. 自动化检查结果

- 周期构建查询明确只纳入 `RETAIL_ORDER` 与 `OFFLINE_SALE`，排除 `PLATFORM_ORDER` 和 `STOCK_TRANSFER`；
- 周期关闭前必须先试算进入 `PENDING_REVIEW`；
- 周期关闭后状态为 `CLOSED`，禁止直接重算覆盖；
- 佣金真实分发前调用限额服务；
- `action_key` 防止业务重试重复消耗封顶和预算；
- `usage_type + rule_id + user_id + period_key` 唯一占用行配合 `FOR UPDATE` 行锁；
- 规则总预算公共占用行使用 `user_id=0`，避免 MySQL 可空唯一索引失效；
- 只有 `PLATFORM_PAYABLE` 进入佣金与提现体系，`OFFLINE_REALIZED` 不参与此占用。

以上为开发自检，最终结果仍以最新完整编译、SQL 实跑和并发测试为准。

## 6. 未完成的真实环境验证

- 未在真实 MySQL 5.7 执行周期和限额表结构升级；
- 未用大量真实零售、线下销售、部分退款数据核对周期净业绩；
- 未并发执行同一规则、同一受益人、同一周期最后一笔封顶占用；
- 未并发执行规则总预算最后额度占用；
- 未验证关闭事务中佣金写入失败时周期和限额占用完整回滚；
- 未验证已关闭周期后续退款冲正与补偿周期的真实闭环；
- 未进行财务对账和全量 UAT。

## 7. PR 描述更新

- 后端 `jzk-boot#5` 增加周期业绩、限额预算和真实验证边界；
- 管理端 `jzk-vue#4` 增加周期业绩页面与构建结果；
- 小程序 `jzk-app#4` 标注本批次无运营配置入口；
- 三个 PR 继续保持 Draft，不把编译或页面构建写成验收通过。
