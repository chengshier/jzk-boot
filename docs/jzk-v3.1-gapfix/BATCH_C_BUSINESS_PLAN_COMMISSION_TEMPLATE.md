# V3.1 补漏批次 C：商业方案、模板化奖励与真实单据试算

## 1. 修改文件清单

### 后端

- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkBusinessRulePlan.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkCommissionRule.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkBusinessRulePlanSaveRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkBusinessRulePlanPublishRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkCommissionTemplateSaveRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkCommissionSourceTrialRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkCommissionRuleSaveRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkCommissionRuleTrialRequest.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkBusinessRulePlanDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/business/JkBusinessRulePlanService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/business/JkBusinessRulePlanServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/commission/JkCommissionTemplateService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/JkCommissionTemplateServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/commission/JkCommissionSourceTrialService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/JkCommissionSourceTrialServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/CommissionRuleServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/CommissionScenarioServiceImpl.java`
- `crmeb-admin/src/main/java/com/zbkj/admin/controller/jiuzhoukang/JkBusinessRulePlanController.java`
- `crmeb-admin/src/main/java/com/zbkj/admin/controller/jiuzhoukang/JkCommissionRuleController.java`

### 管理端

- `jzk-vue/src/api/jkGapfix.js`
- `jzk-vue/src/views/jkBusiness/businessPlan/index.vue`
- `jzk-vue/src/views/jkBusiness/commissionRule/index.vue`
- `jzk-vue/src/router/modules/jkBusiness.js`

## 2. SQL 清单

- `sql/jiuzhoukang/v3.1-gapfix/03-jk-v31-business-plan-template.sql`

核心结构：

- `jk_business_rule_plan` 使用不可变版本行；
- 商业方案发布和停用保留日志；
- 奖励规则保存 `plan_code`、`plan_version_no`、`template_code`、业务范围快照和 `income_nature`；
- 模板 SQL 不插入任何已启用或已发布规则；
- 高级技术权限不自动授予普通运营角色。

## 3. 接口清单

### 商业方案

- `GET /api/admin/jk/business-plan/list`
- `GET /api/admin/jk/business-plan/{id}`
- `GET /api/admin/jk/business-plan/role-cards`
- `POST /api/admin/jk/business-plan/save`
- `POST /api/admin/jk/business-plan/{id}/copy`
- `POST /api/admin/jk/business-plan/publish`
- `POST /api/admin/jk/business-plan/{id}/disable`

### 收益奖励规则

- `GET /api/admin/jk/commission/rule/templates`
- `POST /api/admin/jk/commission/rule/template/save`
- `POST /api/admin/jk/commission/rule/trial/source`
- 原底层 `save`、手工快照 `trial` 和规则 item 接口使用高级权限；普通运营页面不调用。

## 4. 页面清单

- 商业方案角色卡片、版本列表、复制、发布、停用和历史详情；
- 收益奖励规则业务模板向导；
- 普通运营不选择 `source_type`、`beneficiary_type`、`base_type`、`trigger_timing`、`stack_policy` 等技术字段；
- 试算只输入真实订单号、线下销售单号、订货单号、调拨单号或业绩编号；
- 删除无标签快照用户数字输入；
- 规则金额为空时显示“未配置 / 未发布”，不显示 `0` 元；
- 页面增加规则总预算，并明确封顶和预算留空表示不限制。

## 5. 已确认奖励规则口径

- 线上零售基数为逐订单明细商品实付净额，优惠券、积分、满减和其他优惠按明细分摊，运费排除；
- 比例、固定金额和阶梯奖励必须大于 `0` 才能保存为有效配置并发布；
- 不预填具有真实业务含义的比例或金额；
- 同一条规则中比例、固定金额和按数量金额互斥；
- 单笔封顶、用户周期封顶和规则总预算留空表示不限制，填写后必须大于 `0`；
- 生效开始时间必填、结束时间可空；
- 发布版本不可直接编辑，只能复制新版本；
- 新版本不自动重算历史业务，停用只影响后续匹配；
- 所有新规则默认 `status=false`、`publish_status=DRAFT`；
- 收益性质固定为 `PLATFORM_PAYABLE`，`OFFLINE_REALIZED` 继续只在经营收益账本。

以上已经完成业务确认，不再列为待确认项；具体运营数值仍由有权限人员在草稿方案中配置，并需真实业务试算后发布。

## 6. 自动化检查结果

- 发布必须关联商业方案及不可变版本；
- 奖励值、封顶和预算具有正数校验；
- 计算方式字段互斥；
- 规则引擎包含商品、区域、周期门槛和 `TIER_PERCENT` 受控计算；
- 真实业务试算从不可变归属、线下销售、订货、调拨和业绩账本加载上下文，不接受手工拼装关系；
- 管理端生产构建和页面守卫已通过。

以上为开发自检，不等于真实业务验收。

## 7. 未完成的真实环境验证

- 在真实 MySQL 5.7 创建商业方案表并验证旧规则兼容；
- 使用真实权限角色验证普通运营无法调用高级规则接口；
- 使用真实订单、线下销售、订货、调拨和业绩记录逐模板试算；
- 验证多商品优惠、积分、满减、运费和尾差的真实计奖基数；
- 验证周期封顶、规则总预算、阶梯多档和结算任务在并发数据下的最终扣减；
- 由运营人员配置具体比例、金额、封顶和预算，并完成业务审批后再发布；
- 不发布任何默认模板。

## 8. PR 状态

后端和管理端 PR 已增加确认后的奖励口径、代码约束和真实环境待验说明；小程序无佣金配置入口。三个 PR 继续保持 Draft。
