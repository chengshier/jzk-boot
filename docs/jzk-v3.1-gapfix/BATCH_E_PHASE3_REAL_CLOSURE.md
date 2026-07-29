# V3.1 补漏批次 E：第三批真实闭环、推广效果与可信事件边界

> 本批次不以“存在类、表或页面”判定闭环。异常收货、MinIO 导出、订阅消息和健康隐私能力若未执行真实副作用验证，必须继续列为待验，不能写成验收通过。

## 1. 修改文件清单

### 后端新增/修改

- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkPromotionEffectEvent.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkPromotionOpenEventRequest.java`
- `crmeb-service/src/main/java/com/zbkj/service/dao/jiuzhoukang/JkPromotionEffectEventDao.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/promotion/JkPromotionEffectService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/promotion/JkPromotionEffectServiceImpl.java`
- `crmeb-front/src/main/java/com/zbkj/front/controller/jiuzhoukang/JkPromotionEffectFrontController.java`
- `crmeb-admin/src/main/java/com/zbkj/admin/controller/jiuzhoukang/JkPromotionEffectAdminController.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/commission/CommissionScenarioServiceImpl.java`
- `crmeb-common/src/main/java/com/zbkj/common/constants/jiuzhoukang/JkV31PermissionCodes.java`
- `docs/jzk-v3.1-gapfix/phase3-real-closure-discovery.txt`

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
- 客户端事件与后端成交事件使用不同动作键。

## 3. 接口清单

### 小程序

- `POST /api/front/jk/promotion-effect/open`
  - 只接收推广场景、入口页、渠道和 `requestNo`；
  - 不允许客户端传成交金额、推广人归属、订单受益人或 `RETAIL_COMPLETED`。

### 管理端

- `GET /api/admin/jk/promotion-effect/summary`
- `GET /api/admin/jk/promotion-effect/list`

### 后端业务事件

- 零售订单完成时，从 `jk_retail_order_attribution` 不可变快照生成 `RETAIL_COMPLETED` 推广效果事件；
- 即使当前没有已发布佣金规则，推广成交事件仍独立记录；
- 不读取当前上下级关系重新推导推广人。

## 4. 页面清单

- 新增管理端“推广效果统计”：
  - 推广打开次数；
  - 已识别访客；
  - 有效成交订单；
  - 有效成交金额；
  - 打开转化率；
  - 推广场景数；
  - 原始事件列表和写入来源。
- 推广人筛选使用姓名/手机号业务选择器，不要求填写数据库用户 ID。
- 小程序不新增可伪造成交的表单，只在真实入口启动时记录打开事件。

## 5. 自动化检查结果

新增永久守卫：

### 后端

- V3.1 SQL 禁止继续使用 `path/is_del/parent_code` 错误字段；
- 禁止新增独立用户服务区域表或页面；
- 新奖励规则必须默认为 `DRAFT/DISABLED`；
- 佣金服务不得写入 `OFFLINE_REALIZED`；
- 锁定归属必须存在补偿记录路径；
- 周期封顶和预算必须有 `FOR UPDATE` 数据库锁；
- 周期业绩只允许终端零售和经核验线下销售；
- 推广成交必须由后端零售完成事件写入。

### 管理端

- 禁止普通运营表单暴露快照用户、计算基数等数字输入；
- 禁止普通运营直接配置技术枚举；
- 禁止使用“用户 ID / 代理 ID / 数据库 ID”作为业务输入；
- 空奖励显示“未配置 / 未发布”；
- 校验商业方案、收益奖励、零售归属、周期业绩、推广效果和公共抽屉脚本语法。

### 小程序

- 收货地址页不得调用个人资料区域保存接口；
- 普通用户区域流程不得创建业务身份；
- 客户端只能调用推广打开接口，不得提交成交金额、推广人或成交事件；
- 校验本轮涉及页面和 API 的脚本语法。

以上均属于开发自检，不等于真实环境验收。

## 6. 未完成的真实环境验证

### 推广效果

- 未通过真实微信小程序码进入并验证 `scene` 编码格式、解码和多次启动去重；
- 未使用真实登录用户验证访客识别；
- 未完成真实下单、支付、完成、退款后的推广成交和金额回归；
- 退款是否需要同步生成推广效果冲减事件，仍需结合运营报表口径确认并执行 UAT。

### 异常收货 V2

- 已有代码和页面必须继续验证：退款、退回、差额库存、冻结释放、重复回调和并发处理是否产生真实库存/资金副作用；
- 未在本轮仅凭文件存在写成闭环通过。

### MinIO 报表导出

- 必须在真实 MinIO 配置下验证任务生成、文件上传、私有访问、临时 URL、权限隔离、过期和失败重试；
- 未将“存在文件底座”写成报表导出验收通过。

### 订阅消息

- 必须使用真实微信模板 ID、用户授权、发送凭证和回执验证业务场景、失败重试和发送日志；
- 未将“存在任务表/接口”写成真实发送通过。

### 健康授权、访问与隐私

- 必须用真实普通用户、被授权人和管理员验证授权范围、撤销即时生效、访问日志、越权拒绝、隐私字段和报告访问；
- 真实血糖仪厂商接入仍不在本轮范围。

## 7. PR 描述更新

- 后端 `chengshier/jzk-boot#5`：增加推广效果真实事件链、永久自动化守卫和第三批真实环境边界；
- 管理端 `chengshier/jzk-vue#4`：增加推广效果统计页、业务输入限制和 UI 守卫；
- 小程序 `chengshier/jzk-app#4`：增加推广打开采集，并明确客户端不得写成交事件；
- 三个 PR 保持 Draft；
- 不把编译、静态检查、页面构建或“存在实现文件”表述为验收通过。
