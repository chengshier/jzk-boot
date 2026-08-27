# JZK V3.1 已确认商业口径与菜单收口说明

> 确认日期：2026-07-31  
> 累计分支：`feat/jzk-v3.1-phase3-operations`  
> 本文记录已经确认并固化到代码、SQL 和页面的规则，不再列为“待业务确认”。真实数据库、真实订单、微信、并发和终端 UAT 仍单独验收。

## 一、奖励规则口径

### 1. 线上零售计奖基数

线上零售奖励按 `jk_retail_order_attribution` 的逐订单明细实付快照计算：

```text
商品实付净额
= 商品原始金额
- 优惠券分摊
- 积分抵扣分摊
- 满减及其他优惠分摊
- 已退款商品金额
```

运费不计入佣金和团队销售奖励。多明细优惠先分摊，最后一条明细吸收尾差，保证明细合计与订单真实金额一致。

### 2. 奖励值与算法

- 比例奖励、固定金额奖励和阶梯奖励必须配置大于 `0` 的真实数值后才能发布；
- 不为运营预填具有业务含义的比例或金额；
- 未配置时显示“未配置 / 未发布”，不得显示 `¥0.00`；
- 同一条规则中比例、固定金额、按数量金额互斥；组合奖励使用多条独立规则；
- 普通运营只选择业务模板，不直接组装来源类型、受益人来源、计算基数、触发时机和叠加策略等技术枚举。

### 3. 封顶和预算

- 单笔奖励封顶、用户周期封顶和规则总预算均为可选；
- 留空表示“不限制”；
- 一旦填写必须大于 `0`；
- `0` 不代表未配置；
- 周期封顶和规则总预算继续通过唯一动作键、占用行和 `FOR UPDATE` 数据库锁控制并发。

### 4. 生效与版本

- 生效开始时间必填，结束时间可空；
- 新规则默认 `DRAFT/DISABLED`；
- 已发布版本不可直接修改，只能复制为新版本；
- 新版本只影响生效时间后的新业务，不自动重算历史订单；
- 停用只阻止后续新业务匹配，不覆盖历史记录。

### 5. 收益性质

- 奖励规则只生成 `PLATFORM_PAYABLE`；
- 线下调拨价差等已经实现的经营收益属于 `OFFLINE_REALIZED`；
- `OFFLINE_REALIZED` 不重复进入佣金和提现账户。

## 二、退款后的佣金口径

退款完成后继续使用下单时不可变归属和金额快照：

- 原佣金记录、结算记录和提现记录不删除、不覆盖；
- 部分退款按本次明细退款净额占退款前剩余计奖基数的比例冲正；
- 累计全额退款时将该明细剩余佣金全部冲正；
- 待结算或冻结中的金额减少对应待结算/冻结余额；
- 已结算金额生成反向佣金流水；
- 已提现部分不修改历史提现，形成后续待抵扣；
- 退款请求号、订单明细和佣金记录共同形成幂等动作键，重复回调不得重复冲正。

现有统一入口：

```text
StoreOrderTaskServiceImpl.refundOrder
→ CommissionTriggerService.onRefundCompleted
→ RetailOrderAttributionService.allocateRefund
→ 业绩冲正 + 佣金冲正 + 推广退款反向事件
```

## 三、退款后的推广效果口径

### 1. 可信事件

- 小程序客户端只允许上报 `OPEN`；
- 真实订单完成由后端生成 `RETAIL_COMPLETED`；
- 真实退款完成由后端生成 `RETAIL_REFUND`；
- 客户端不得提交推广人、成交金额、成交事件或退款事件；
- 成交和退款均读取订单明细不可变归属快照，不读取当前上下级关系。

### 2. 净效果计算

```text
净有效成交金额
= RETAIL_COMPLETED 金额
- RETAIL_REFUND 金额
```

- 部分退款只减少有效成交金额，订单仍计一笔有效成交；
- 多次部分退款累计达到全额后，该订单不再计为有效成交；
- 重复退款回调不重复冲减；
- 推广打开次数和已识别访客不因退款删除；
- 报表按订单聚合，不按订单明细重复计算订单数。

### 3. 转化率

```text
打开转化率 = 净有效成交订单数 ÷ 推广打开次数
访客转化率 = 净有效成交订单数 ÷ 已识别推广访客数
```

分母为 `0` 时后端返回空值，页面显示 `--`，不显示误导性的 `0%`。

## 四、九州康菜单 404 收口

问题根因：九州康一级菜单原先递归选择第一个子节点，但数据库第一项可能是无子页面的 `M` 类型目录，例如 `/operation/jzk/group/identity`，前端将不存在的目录地址作为页面跳转，最终命中 `/404`。

已实施：

- `src/utils/system.js`：递归查找第一个真实页面，跳过 `M` 目录和 `A` 按钮，并继续检查兄弟节点；
- `src/layout/component/columnsAside.vue`：找不到真实页面时回退一级菜单自身路由，由 `/operation/jzk` 的 redirect 处理；
- `jk_v31_phase3_menu_permission_patch.sql`：不再创建空“身份与关系”目录，并隐藏、逻辑删除所有无真实子节点的 `/operation/jzk/group/%` 目录；
- 永久 CI 守卫菜单首跳逻辑和空目录清理 SQL。

本地数据库若已经存在空目录，重新执行最新菜单补丁并退出重登、清除 `MerPlatAdmin_MenuList` 与 `MerPlatAdmin_oneLvRoutes` 缓存后生效。

## 五、本次修改文件

### 后端

- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkCommissionTemplateSaveRequest.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/promotion/JkPromotionEffectService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/promotion/JkPromotionEffectServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/JkCommissionTemplateServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/CommissionRuleServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/CommissionTriggerServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/context/JkBusinessContextOverviewServiceImpl.java`
- `sql/jiuzhoukang/fix/jk_v31_phase3_menu_permission_patch.sql`
- `.github/workflows/jk-v31-gapfix-guards.yml`

### 管理端

- `src/utils/system.js`
- `src/layout/component/columnsAside.vue`
- `src/views/jkBusiness/commissionRule/index.vue`
- `src/views/jkBusiness/promotionEffect/index.vue`
- `.github/workflows/jk-v31-gapfix-ui-guards.yml`

### 小程序

- `.github/workflows/jk-consistency-ci.yml`：修正多行 `import` 被误判为 Vue 语法错误的问题；业务页面代码不因检查器误报而改写。

## 六、仍需真实环境验证

- MySQL 5.7 实跑全部补丁、历史数据兼容和空菜单清理；
- 真实多商品订单的优惠券、积分、满减、运费和尾差分摊；
- 部分退款、多次退款、累计全额退款和重复回调；
- 已结算、已提现佣金的真实冲正和后续抵扣；
- 推广打开、真实成交、退款净额及两种转化率；
- 周期封顶和规则总预算最后额度并发；
- 后台普通角色菜单、按钮、数据范围及登录缓存刷新。

以上验证完成前，三个 PR 继续保持 Draft，不能表述为真实环境或上线验收通过。
