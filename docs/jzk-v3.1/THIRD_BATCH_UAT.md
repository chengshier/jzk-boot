# 九州康 JZK V3.1 第三批真实环境统一验收清单

> 适用分支：`feat/jzk-v3.1-phase3-operations`  
> 前置基线：第一批、第二批对应分支与 SQL 已按顺序执行  
> 本文用于真实 MySQL 5.7、真实 Token、管理端浏览器、微信开发者工具和真机验收，不以编译或静态检查代替 UAT。

## 一、验收前备份

至少备份：

```text
jk_stock_account
jk_stock_item
jk_stock_batch
jk_stock_flow
jk_platform_order
jk_platform_order_item
jk_stock_transfer
jk_stock_transfer_item
jk_trade_receive_exception
jk_trade_receive_exception_item
jk_commission_rule
jk_commission_record
jk_fund_account
jk_withdraw_apply
eb_system_menu
eb_user_token
```

同时记录：

- 当前后端 Jar 版本和 Git commit；
- 管理端和小程序 commit；
- 数据库备份时间；
- 测试环境域名；
- 测试微信小程序 appid；
- MinIO 测试 Bucket；
- 使用的普通管理员、平台超管、区县代理、创客、合伙人账号。

## 二、SQL 执行顺序

严格按顺序执行：

```text
第一批
1. sql/jiuzhoukang/fix/jk_v31_phase1_relation_quota_upgrade.sql
2. sql/jiuzhoukang/fix/jk_v31_commission_templates_disabled.sql

第二批
3. sql/jiuzhoukang/fix/jk_v31_phase2_business_closure.sql
4. sql/jiuzhoukang/fix/jk_v31_phase2_menu_permission_upgrade.sql

第三批
5. sql/jiuzhoukang/fix/jk_v31_phase3_operations_upgrade.sql
6. sql/jiuzhoukang/fix/jk_v31_phase3_receive_exception_v2_item_patch.sql
7. sql/jiuzhoukang/fix/jk_v31_phase3_subscription_task_patch.sql
8. sql/jiuzhoukang/fix/jk_v31_phase3_menu_permission_patch.sql
```

执行后核对：

- 所有脚本无报错；
- 重复执行不会产生重复表、重复菜单或重复模板；
- 创客、合伙人、区县代理佣金模板仍为关闭状态；
- 微信推广场景 `AGENT_BIND` 仍为关闭状态；
- 普通管理员未被自动授予盘点审核、异常处理、订阅任务执行等高风险按钮。

## 三、默认关闭配置核对

首次启动保持：

```properties
jk.health.provider-enabled=false
jk.health.callback-enabled=false

jk.file.storage.enabled=false
jk.wechat.enabled=false
jk.wechat.subscribe-enabled=false
jk.wechat.subscribe-auto-process-enabled=false
```

期望：

- 健康手工记录、趋势、提醒、档案、授权正常；
- 不执行厂商拉取或回调；
- 不返回伪上传成功；
- 不生成占位小程序码；
- 业务节点可以正常完成；
- 订阅任务进入 `WAIT_CONFIG`，不调用微信接口，不产生周期性失败日志。

## 四、测试账号与数据

建议准备：

| 账号 | 作用 |
|---|---|
| 平台超管 | 菜单、配置、跨区域核对 |
| 普通管理员 A | 被授予第三批查询权限 |
| 普通管理员 B | 不授予高风险按钮，用于越权测试 |
| 区县代理 A | 平台订货、发起库存盘点、处理下级调拨 |
| 创客 A | 调拨、收货、线下销售、库存盘点 |
| 合伙人 A | 调拨、收货、经营数据 |
| 普通用户 A | 健康手工记录、身份申请 |

准备至少两个真实商品、两个 SKU 和可追溯库存批次。

## 五、库存盘点

### 5.1 正常盘点

1. 选择真实库存主体创建盘点；
2. 核对账面快照数量、商品名、SKU 名和成本；
3. 逐项录入实盘数量；
4. 保存后重新进入，确认数据未丢失；
5. 提交审核；
6. 管理员审核通过。

期望：

- 盘盈走统一入库流水；
- 盘亏先冻结再出库；
- 盘点单、明细、日志、库存流水一致；
- 不直接覆盖库存数量字段；
- 重复审核不重复调整库存。

### 5.2 快照过期保护

1. 创建盘点快照；
2. 在提交或审核前完成一笔真实订货、调拨或线下销售，使库存发生变化；
3. 再审核原盘点。

期望：系统拒绝用过期快照覆盖真实库存，并提示重新盘点。

### 5.3 并发

同时提交两次审核请求。

期望：只有一次成功应用库存差异，另一请求被状态或幂等机制拦截。

## 六、异常收货 V2

### 6.1 少货补发

1. 对平台订货或调拨上报少货；
2. 管理端进入异常详情；
3. 确认商品、SKU、应收、实收、短缺数量来自真实异常单；
4. 创建纯补发方案；
5. 填写补发数量和真实物流信息；
6. 确认补发真实完成。

期望：

- 无需手填异常明细 ID；
- 处理数量不超过短缺与破损剩余数量；
- 待确认方案也占用可处理数量；
- 全部纯补发完成后才恢复原业务待收货；
- 重新收货只执行一次真实入库。

### 6.2 差额退款

创建含退款数量的方案但退款金额为 0。

期望：系统拒绝保存或完成。

填写真实退款金额后：

- 异常单继续保持锁定；
- 不仅通过修改状态伪造退款完成；
- 待真实资金动作和冲正链路完成后才能最终关闭。

### 6.3 退回、接受现状和组合方案

分别验证：

- 退回数量；
- 接受数量；
- 补发 + 退款组合；
- 同一 SKU 多个待确认方案超额占用；
- 重复 requestNo；
- 旧工作台直接提交 `RESOLVED`。

期望：数量超限、重复请求和旧完成入口全部被拦截。

## 七、统一私有文件与 MinIO

### 7.1 默认关闭

文件存储关闭时上传图片或 PDF。

期望：明确返回不可用，不保存任意公网 URL，不返回伪成功。

### 7.2 LOCAL_PRIVATE

启用本地私有存储，上传允许类型文件。

核对：

- `jk_file_object` 元数据；
- 文件哈希、大小、业务类型、业务 ID、所有者和访问级别；
- 登录用户可下载本人文件；
- 其他普通用户无法越权下载；
- 管理员权限符合数据范围；
- 过期文件不可继续访问。

### 7.3 MinIO

配置真实 endpoint、access key、secret key 和预建 Bucket 后启用。

验证：

- 上传与读取成功；
- Bucket 不存在、密钥错误、网络超时均明确失败；
- 失败时不回退为伪成功或任意公网地址；
- 数据库对象键与 MinIO 实际对象一致。

## 八、真实微信小程序码

保持 `AGENT_BIND` 场景关闭时生成：应明确不可用。

配置：

```properties
jk.wechat.enabled=true
jk.wechat.appid=真实值
jk.wechat.secret=真实值
```

并启用私有文件存储、人工开启推广场景后验证：

- 微信返回真实 PNG；
- 结果写入 `jk_file_object`；
- 同一用户、场景和 scene 使用缓存；
- scene 超长、角色不匹配和场景停用被拒绝；
- 扫码后进入预期页面；
- 重复扫码不覆盖已有有效关系；
- 换绑仍走审核流程。

## 九、微信订阅消息

### 9.1 默认关闭

保持三层开关关闭，依次执行：

- 身份审核；
- 平台订货付款审核；
- 平台订货发货；
- 调拨审核；
- 调拨付款确认；
- 调拨拨货；
- 下级确认收货；
- 提现提交、审核和打款。

期望：主业务成功，任务为 `WAIT_CONFIG`，不调用微信。

### 9.2 配置与用户授权

配置真实 appid、secret、四类模板 ID 和字段映射：

```properties
jk.wechat.enabled=true
jk.wechat.subscribe-enabled=true
jk.wechat.subscribe-auto-process-enabled=false
```

在真机验证：

- 业务中心“消息提醒”最多请求三个业务模板；
- 提现页点击提交时只请求一个提现模板；
- 用户拒绝授权不阻断业务；
- 用户允许后 `eb_user_token.type=2` 对应真实小程序 openId；
- 小程序不上传或覆盖 openId。

### 9.3 发送和状态

先保持自动处理关闭，在后台手动处理一条任务。

期望：

- `PENDING → PROCESSING → SENT`；
- 只有微信返回 `errcode=0` 才成功；
- 缺 openId 进入 `WAIT_RECIPIENT`；
- 用户后续登录小程序再重新入队时刷新 openId；
- 缺模板或总开关关闭进入 `WAIT_CONFIG`；
- 临时错误进入 `RETRY_WAIT`；
- 超过上限进入 `FAILED`。

### 9.4 最终状态语义

制造平台订货库存不足：管理员请求通过，但系统最终自动驳回。

期望：通知内容为“已驳回”，不得按请求参数发送“已通过”。

### 9.5 多实例

同时启动两个服务实例并开启：

```properties
jk.wechat.subscribe-auto-process-enabled=true
```

期望：

- 同一任务只有一个实例抢占；
- 不重复发送；
- 模拟发送进程中断后，超过十分钟的 `PROCESSING` 可被重新抢占；
- 自动处理关闭时不创建订阅任务调度器。

## 十、健康周报/月报

准备真实手工血糖、饮食、运动和用药记录后生成：

- 7 天周报；
- 31 天月报。

验证：

- 只统计本人真实记录；
- 平均、最高、最低和风险数量可回溯；
- 数据来源统计正确；
- 无记录时不虚构数值；
- 不输出诊断、处方或设备测量结论；
- 未授权人员无法查看健康明细；
- 管理端访问产生审计日志并受数据范围限制。

## 十一、权限与菜单

分别使用平台超管、已授权普通管理员、未授权普通管理员验证：

- 页面菜单；
- 列表接口；
- 盘点审核按钮；
- 异常处理按钮；
- 推广场景保存；
- 订阅任务处理和重新入队；
- 健康报告查询。

期望：菜单、按钮和接口 authority 一致；隐藏按钮不能通过直接调用接口绕过。

## 十二、跨批次回归

第三批验收后仍需回归：

- 普通用户浏览、购物车、下单、支付、订单和售后；
- 首次绑定、换绑审核、人数额度和管理员强制调整；
- 平台订货和库存调拨正常链路；
- 线下销售、业绩、经营收益和平台佣金分账；
- 创客、合伙人、区县代理佣金模板默认关闭；
- 提现冻结、驳回释放和确认打款；
- 退款、退货和调拨退回冲正；
- 无真实健康厂商时设备入口保持未开放。

## 十三、验收证据

每个用例至少保留：

```text
用例编号
测试账号和角色
请求参数/requestNo
接口返回
业务表状态
库存/资金/佣金/业绩流水
管理端截图
小程序截图
微信返回码或 MinIO 对象信息
是否通过
失败原因和修复 commit
```

未取得真实证据的项目统一标记为“待验证”，不得写成“已通过”。
