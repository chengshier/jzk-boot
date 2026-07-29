# 九州康 V3.1 第三批订阅消息接入说明

## 一、默认状态

微信能力、订阅发送和自动处理三层开关均默认关闭：

```properties
jk.wechat.enabled=false
jk.wechat.subscribe-enabled=false
jk.wechat.subscribe-auto-process-enabled=false
```

默认关闭时，业务状态机仍正常完成，系统只创建 `WAIT_CONFIG` 任务，不调用微信接口，也不会把排队误报为发送成功。

## 二、已接入业务节点

| 业务节点 | 模板编码 | 接收人 | 幂等依据 |
|---|---|---|---|
| 身份申请通过/驳回 | `AUDIT_RESULT` | 身份申请人 | 业务类型 + 申请 ID + 结果状态 |
| 平台订货付款通过/驳回 | `AUDIT_RESULT` | 区县代理 | 订货单 ID + 最终落库状态 |
| 平台订货发货 | `RECEIVE_REMINDER` | 区县代理 | 订货单 ID + 待收货状态 |
| 调拨申请通过/驳回 | `AUDIT_RESULT` | 创客/合伙人 | 调拨单 ID + 最终落库状态 |
| 调拨付款通过/驳回 | `TRANSFER_STATUS` | 创客/合伙人 | 调拨单 ID + 最终付款状态 |
| 调拨拨货 | `RECEIVE_REMINDER` | 创客/合伙人 | 调拨单 ID + 待收货状态 |
| 下级确认调拨收货 | `TRANSFER_STATUS` | 所属区县代理 | 调拨单 ID + 已收货状态 |
| 提现申请提交 | `WITHDRAW_STATUS` | 提现申请人 | 提现单 ID + 已提交状态 |
| 提现审核通过/驳回 | `WITHDRAW_STATUS` | 提现申请人 | 提现单 ID + 审核状态 |
| 提现确认打款 | `WITHDRAW_STATUS` | 提现申请人 | 提现单 ID + 已打款状态 |

所有通知均在主业务事务提交后入队。入队或微信发送失败不会回滚已完成的审核、库存、收货或资金业务。

平台订货和库存调拨的通知结果读取最终落库状态，不直接信任请求中的 `approved`。因此即使管理员点击通过后因库存不足被系统自动驳回，用户收到的仍是“已驳回”。

## 三、可信接收人

接收人的 openId 只从 `eb_user_token` 中读取：

```text
uid = 业务接收用户 ID
type = 2（小程序）
token = 已认证小程序 openId
```

任务服务不会信任调用方或前端传入的 openId，每次创建和重新入队都会按 `receiverUserId` 重新读取可信小程序登录关系。

找不到可信 openId 时，任务进入：

```text
WAIT_RECIPIENT
```

管理员补齐用户小程序登录关系后，可在订阅任务管理页重新入队。

## 四、微信配置

正式启用前至少配置：

```properties
jk.wechat.enabled=false
jk.wechat.appid=
jk.wechat.secret=

jk.wechat.subscribe-enabled=false
jk.wechat.subscribe.audit-template-id=
jk.wechat.subscribe.transfer-template-id=
jk.wechat.subscribe.receive-template-id=
jk.wechat.subscribe.withdraw-template-id=

jk.wechat.subscribe-auto-process-enabled=false
jk.wechat.subscribe-process-cron=0 */1 * * * ?
jk.wechat.subscribe-process-batch-size=20
```

完成真实模板核对后，按顺序启用：

```properties
jk.wechat.enabled=true
jk.wechat.subscribe-enabled=true
jk.wechat.subscribe-auto-process-enabled=true
```

也可以保持自动处理关闭，通过后台“订阅消息任务”页面手动处理到期任务。

## 五、模板字段映射

微信模板字段名由实际模板决定，不能写死为同一套字段。本次实现支持按模板类型配置语义字段映射：

```properties
jk.wechat.subscribe.audit-field-mapping=businessNo=character_string1,subject=thing2,status=phrase3,remark=thing4,time=time5
jk.wechat.subscribe.transfer-field-mapping=businessNo=character_string1,status=phrase2,remark=thing3,time=time4
jk.wechat.subscribe.receive-field-mapping=businessNo=character_string1,subject=thing2,remark=thing3,time=time4
jk.wechat.subscribe.withdraw-field-mapping=businessNo=character_string1,amount=amount2,status=phrase3,remark=thing4,time=time5
```

左侧为系统语义字段，右侧必须改成微信公众平台中实际模板的字段名。

支持语义字段：

```text
businessNo
subject
status
remark
time
amount
```

正式启用前必须在微信公众平台逐个核对模板字段和长度限制。默认映射仅用于代码结构和联调准备，不代表与当前小程序账号模板一致。

## 六、任务状态和多实例安全

```text
PENDING          待发送
PROCESSING       已被某个实例抢占，正在发送
RETRY_WAIT       等待重试
WAIT_CONFIG      总开关、微信凭据或模板未就绪
WAIT_RECIPIENT   缺少可信小程序 openId
SENT             微信明确返回 errcode=0
FAILED           超过最大重试次数
```

任务处理使用状态条件更新进行抢占。后台服务和前台服务同时运行，或者部署多个实例时，同一任务只能由一个实例进入 `PROCESSING`。

如果实例在发送期间异常退出，超过十分钟仍处于 `PROCESSING` 的任务允许被后续轮次重新抢占。

## 七、真实环境测试顺序

1. 保持三层开关关闭，执行身份审核、调拨拨货和提现审核，确认业务成功且任务为 `WAIT_CONFIG`。
2. 配置微信凭据、真实模板 ID 和字段映射，但仍保持发送开关关闭。
3. 使用已登录小程序的测试用户核对 `eb_user_token.type=2` 和 openId。
4. 开启微信能力和订阅发送，只对测试账号手动处理一条审核结果通知。
5. 确认微信返回 `errcode=0` 后，再开启自动任务处理。
6. 验证平台订货库存不足自动驳回时，通知结果为“已驳回”。
7. 验证同一业务状态重复调用不会生成重复任务。
8. 验证无 openId 用户进入 `WAIT_RECIPIENT`；用户登录小程序后重新入队可刷新 openId。
9. 验证两个服务实例同时处理时不会重复发送。
10. 验证模拟发送进程中断后，超时 `PROCESSING` 任务可以恢复。
11. 验证微信超时或临时错误进入指数退避重试，超过上限后为 `FAILED`。

## 八、上线边界

代码编译通过不等于微信真实发送通过。以下内容必须由真实环境完成：

- 小程序订阅授权；
- 模板 ID 与字段映射；
- appid/secret；
- 用户 openId；
- 微信接口返回码；
- 多实例任务抢占；
- 真机点击消息后的页面跳转。
