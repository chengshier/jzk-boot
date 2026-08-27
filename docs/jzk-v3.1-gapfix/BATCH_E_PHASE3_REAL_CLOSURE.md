# V3.1 补漏批次 E：第三批真实闭环、推广效果与可信事件边界

> 本批次不以“存在类、表或页面”判定闭环。异常收货、MinIO 导出、订阅消息和健康隐私能力若未执行真实副作用验证，必须继续列为待验，不能写成验收通过。

## 1. 修改文件清单

### 后端新增/修改

- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkPromotionEffectEvent.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkPromotionOpenEventRequest.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkPromotionEffectEventDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/promotion/JkPromotionEffectService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/promotion/JkPromotionEffectServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/CommissionScenarioServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/CommissionTriggerServiceImpl.java`
- `crmeb-front/src/main/java/com/zbkj/front/controller/jiuzhoukang/JkPromotionEffectFrontController.java`
- `crmeb-admin/src/main/java/com/zbkj/admin/controller/jiuzhoukang/JkPromotionEffectAdminController.java`

### 管理端新增/修改

- `jzk-vue/src/api/jkPromotionEffect.js`
- `jzk-vue/src/views/jkBusiness/promotionEffect/index.vue`
- `jzk-vue/src/router/modules/jkBusiness.js`

### 小程序新增/修改

- `jzk-app/api/jkPromotionEffect.js`
- `jzk-app/App.vue`

### 自动化守卫

- `jzk-boot/.github/workflows/jk-v31-gapfix-guards.yml`
- `jzk-vue/.github/workflows/jk-v31-gapfix-ui-guards.yml`
- `jzk-app/.github/workflows/jk-v31-gapfix-app-guards.yml`

## 2. SQL 清单

- `sql/jiuzhoukang/v3.1-gapfix/05-jk-v31-promotion-effect.sql`

新增：

- `jk_promotion_effect_event`
- `admin:jk:promotion:effect:view`
- “推广效果统计”菜单及真实字段结构

数据库约束：

- `event_no` 唯一；
- `idempotency_key` 唯一；
- 按场景、推广人、事件类型和发生时间建立索引；
- 客户端打开、后端成交和后端退款事件使用不同动作键。

## 3. 接口和可信事件

### 小程序接口

- `POST /api/front/jk/promotion-effect/open`
  - 只接收推广场景、入口页、渠道和 `requestNo`；
  - 不允许客户端传成交金额、推广人归属、订单受益人、`RETAIL_COMPLETED` 或 `RETAIL_REFUND`。

### 管理端接口

- `GET /api/admin/jk/promotion-effect/summary`
- `GET /api/admin/jk/promotion-effect/list`

### 后端业务事件

- 零售订单完成：从 `jk_retail_order_attribution` 不可变快照生成 `RETAIL_COMPLETED`；
- 零售退款完成：在统一 `CommissionTriggerService.onRefundCompleted` 分摊循环中生成 `RETAIL_REFUND`；
- 成交和退款均不读取当前上下级关系；
- 即使当前没有已发布佣金规则，推广成交和退款效果仍独立记录；
- 退款事件与业绩、佣金冲正使用同一份明细归属快照、同一退款请求号和同一事务入口。

## 4. 已确认的推广退款净效果口径

```text
净有效成交金额
= RETAIL_COMPLETED 金额
- RETAIL_REFUND 金额
```

- 部分退款只冲减成交金额，订单仍算一笔有效成交；
- 多次部分退款累计达到全额后，该订单不再计为有效成交；
- 重复退款回调通过幂等键防止重复冲减；
- 推广打开次数和已识别访客不因退款删除；
- 订单数按订单聚合，不因多商品明细重复计算；
- 打开转化率 = 净有效订单数 ÷ 打开次数；
- 访客转化率 = 净有效订单数 ÷ 已识别访客数；
- 分母为零时后端返回空值，页面显示 `--`。

该口径已确认，不再列为待业务确认；仍需真实订单和退款数据执行 UAT。

## 5. 页面清单

管理端“推广效果统计”展示：

- 推广打开次数；
- 已识别访客；
- 净有效成交订单；
- 净有效成交金额；
- 打开转化率；
- 访客转化率；
- 推广场景数；
- `OPEN`、`RETAIL_COMPLETED`、`RETAIL_REFUND` 原始事件及可信写入来源。

推广人筛选使用姓名/手机号业务选择器，不要求填写数据库用户 ID。小程序不新增可伪造成交或退款的表单。

## 6. 自动化检查结果

### 后端永久守卫

- V3.1 SQL 禁止真实写入继续使用错误字段；
- 禁止独立用户服务区域表或页面；
- 新奖励规则默认 `DRAFT/DISABLED`；
- 佣金服务不得写入 `OFFLINE_REALIZED`；
- 锁定归属必须存在补偿记录路径；
- 周期封顶和预算必须存在数据库并发锁；
- 推广成交和退款必须由后端可信业务链生成；
- 必须同时输出打开转化率和访客转化率。

### 管理端永久守卫

- 禁止普通运营表单暴露快照用户、计算基数或数据库 ID；
- 禁止普通运营直接配置技术枚举；
- 空奖励显示“未配置 / 未发布”；
- 奖励封顶和预算留空表示不限制；
- 推广页面必须识别 `RETAIL_REFUND` 和访客转化率；
- 菜单首跳必须跳过目录和按钮节点。

### 小程序永久守卫

- 收货地址页不得调用个人资料区域保存接口；
- 普通用户区域流程不得创建业务身份；
- 客户端只能调用推广打开接口；
- 静态检查兼容 Vue 多行 `import`，不再把检查器误报当成页面语法错误。

以上均属于开发自检，不等于真实环境验收。

## 7. 未完成的真实环境验证

### 推广效果

- 真实微信小程序码 `scene` 编码、解码和重复启动去重；
- 登录前后访客识别；
- 真实多商品订单完成、部分退款、多次退款、累计全额退款和重复退款回调；
- 净有效订单、净成交金额和两种转化率对账。

### 异常收货 V2

- 真实退款、退回、差额库存、冻结释放、重复回调和并发处理副作用。

### MinIO 报表导出

- 任务生成、文件上传、私有访问、临时 URL、权限隔离、过期和失败重试。

### 订阅消息

- 真实微信模板 ID、用户授权、发送凭证、回执、失败重试和发送日志。

### 健康授权、访问与隐私

- 真实普通用户、被授权人和管理员的授权范围、撤销即时生效、访问日志、越权拒绝、隐私字段和报告访问；
- 真实血糖仪厂商接入仍不在本轮范围。

## 8. 结论边界

- 推广退款统计的商业口径已经确认并落实到代码与页面；
- 不能把编译、静态检查、页面构建或“存在实现文件”表述为真实环境验收通过；
- 三个 PR 在真实环境 UAT 完成前继续保持 Draft。
