# V3.1 补漏批次 C：商业方案、模板化奖励与真实单据试算

## 1. 修改文件清单

### 后端
- `crmeb-common/.../jiuzhoukang/JkBusinessRulePlan.java`
- `crmeb-common/.../jiuzhoukang/JkCommissionRule.java`
- `crmeb-common/.../request/jiuzhoukang/JkBusinessRulePlanSaveRequest.java`
- `crmeb-common/.../request/jiuzhoukang/JkBusinessRulePlanPublishRequest.java`
- `crmeb-common/.../request/jiuzhoukang/JkCommissionTemplateSaveRequest.java`
- `crmeb-common/.../request/jiuzhoukang/JkCommissionSourceTrialRequest.java`
- `crmeb-common/.../request/jiuzhoukang/JkCommissionRuleSaveRequest.java`
- `crmeb-common/.../request/jiuzhoukang/JkCommissionRuleTrialRequest.java`
- `crmeb-service/.../dao/jiuzhoukang/JkBusinessRulePlanDao.java`
- `crmeb-service/.../business/JkBusinessRulePlanService.java`
- `crmeb-service/.../business/JkBusinessRulePlanServiceImpl.java`
- `crmeb-service/.../commission/JkCommissionTemplateService.java`
- `crmeb-service/.../commission/JkCommissionTemplateServiceImpl.java`
- `crmeb-service/.../commission/JkCommissionSourceTrialService.java`
- `crmeb-service/.../commission/JkCommissionSourceTrialServiceImpl.java`
- `crmeb-service/.../commission/CommissionRuleServiceImpl.java`
- `crmeb-service/.../commission/CommissionScenarioServiceImpl.java`
- `crmeb-admin/.../JkBusinessRulePlanController.java`
- `crmeb-admin/.../JkCommissionRuleController.java`
- `crmeb-common/.../constants/jiuzhoukang/JkV31PermissionCodes.java`

### 管理端
- `jzk-vue/src/api/jkGapfix.js`
- `jzk-vue/src/views/jkBusiness/businessPlan/index.vue`
- `jzk-vue/src/views/jkBusiness/commissionRule/index.vue`
- `jzk-vue/src/router/modules/jkBusiness.js`

## 2. SQL 清单

- `sql/jiuzhoukang/v3.1-gapfix/03-jk-v31-business-plan-template.sql`

核心结构：
- 新增 `jk_business_rule_plan` 不可变版本行；
- 新增商业方案发布/停用日志表；
- 收益奖励规则补 `plan_code`、`plan_version_no`、`template_code`、业务范围快照和 `income_nature`；
- 模板数据不插入任何已启用或已发布规则；
- 技术高级权限不自动授予普通运营角色。

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
- 原底层 `save`、手工快照 `trial`、规则 item 接口改为高级权限；普通运营页面不调用。

## 4. 页面清单

- 新增“商业方案”角色卡片、版本列表、复制、发布、停用和历史详情页；
- 重构“收益奖励规则”为业务模板向导；
- 普通运营不选择 `source_type`、`beneficiary_type`、`base_type`、`trigger_timing`、`stack_policy` 等技术字段；
- 试算只输入真实订单号、线下销售单号、订货单号、调拨单号或业绩编号；
- 删除无标签“快照用户”数字输入；
- 规则金额为空时显示“未配置 / 未发布”，不显示 0 元。

## 5. 自动化检查结果

- 新规则保存逻辑强制 `status=false`、`publish_status=DRAFT`；
- 发布规则必须关联商业方案和不可变版本；
- 佣金规则收益性质强制为 `PLATFORM_PAYABLE`；
- `OFFLINE_REALIZED` 继续只在经营收益账本，不通过模板进入佣金账户；
- 规则引擎增加商品、区域、周期门槛范围检查和 `TIER_PERCENT` 受控计算；
- 真实业务试算服务从归属快照、线下销售、订货、调拨和业绩账本加载上下文，不接受运营手工拼装用户关系。

当前仍需以最新提交的完整编译和管理端生产构建结果为准；编译成功仅是开发自检。

## 6. 未完成的真实环境验证

- 未在真实 MySQL 5.7 创建商业方案表并验证旧规则兼容；
- 未使用真实权限角色验证普通运营无法调用高级规则接口；
- 未用真实订单、线下销售、订货、调拨和业绩记录逐模板执行试算；
- 未验证周期封顶、总预算、阶梯多档和结算任务在真实并发数据下的最终扣减；
- 未完成运营比例、固定金额、封顶和生效时间的商业确认；
- 未发布任何默认模板。

## 7. PR 描述更新

- 后端 `jzk-boot#5` 与管理端 `jzk-vue#4` 增加商业方案、模板化奖励、真实业务试算和待验说明；
- 小程序 `jzk-app#4` 标注本批次无新增佣金配置入口；
- 三个 PR 继续保持 Draft，不以编译成功替代真实验收。
