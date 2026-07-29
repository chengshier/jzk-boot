# 九州康 V3.1 第三批订阅消息接入说明

## 一、默认状态

订阅消息总开关默认关闭：

```properties
jk.wechat.subscribe-enabled=false
```

默认关闭时，业务状态机仍正常完成，系统只创建 `WAIT_CONFIG` 任务，不调用微信接口，也不会把排队误报为发送成功。

## 二、已接入业务节点

| 业务节点 | 模板编码 | 接收人 | 幂等依据 |
|---|---|---|---|
| 身份申请通过/驳回 | `AUDIT_RESULT` | 身份申请人 | 业务类型 + 申请 ID + 结果状态 |
| 平台订货付款通过/驳回 | `AUDIT_RESULT` | 区县代理 | 订货单 ID + 结果状态 |
| 平台订货发货 | `RECEIVE_REMINDER` | 区县代理 | 订货单 ID + 待收货状态 |
| 调拨申请通过/驳回 | `AUDIT_RESULT` | 创客/合伙人 | 调拨单 ID + 结果状态 |
| 调拨付款通过/驳回 | `TRANSFER_STATUS` | 创客/合伙人 | 调拨单 ID + 付款状态 |
| 调拨拨货 | `RECEIVE_REMINDER` | 创客/合伙人 | 调拨单 ID + 待收货状态 |
| 下级确认调拨收货 | `TRANSFER_STATUS` | 所属区县代理 | 调拨单 ID + 已收货状态 |
| 提现申请提交 | `WITHDRAW_STATUS` | 提现申请人 | 提现单 ID + 已提交状态 |
| 提现审核通过/驳回 | `WITHDRAW_STATUS` | 提现申请人 | 提现单 ID + 审核状态 |
| 提现确认打款 | `WITHDRAW_STATUS` | 提现申请人 | 提现单 ID + 已打款状态 |

所有通知均在主业务事务提交后入队。入队或微信发送失败不会回滚已完成的审核、库存、收货或资金业务。

## 三、可信接收人

接收人的 openId 只从 `eb_user_token` 中读取：

```text
uid = 业务接收用户 ID
type = 2（小程序）
token = 已认证小程序 openId
```

接口不接受前端传入任意 openId。找不到可信 openId 时，任务进入：

```text
WAIT_RECIPIENT
```

管理员补齐用户小程序登录关系后，可在订阅任务管理页重新入队。

## 四、微信配置

正式启用前至少配置：

```properties
jk.wechat.appid=
jk.wechat.secret=
jk.wechat.subscribe-enabled=false
jk.wechat.subscribe.audit-template-id=
jk.wechat.subscribe.transfer-template-id=
jk.wechat.subscribe.receive-template-id=
jk.wechat.subscribe.withdraw-template-id=
```

完成真实模板核对后再将：

```properties
jk.wechat.subscribe-enabled=true
```

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

## 六、任务状态

```text
PENDING          待发送
RETRY_WAIT       等待重试
WAIT_CONFIG      总开关、微信凭据、模板或字段配置未就绪
WAIT_RECIPIENT   缺少可信小程序 openId
SENT             微信明确返回 errcode=0
FAILED           超过最大重试次数
```

## 七、真实环境测试顺序

1. 保持总开关关闭，执行身份审核、调拨拨货和提现审核，确认业务成功且任务为 `WAIT_CONFIG`。
2. 配置微信凭据、真实模板 ID 和字段映射，但仍保持总开关关闭。
3. 使用已登录小程序的测试用户核对 `eb_user_token.type=2` 和 openId。
4. 开启总开关，只对测试账号执行一条审核结果通知。
5. 确认微信返回 `errcode=0` 后，再验证调拨、收货和提现通知。
6. 验证同一业务状态重复调用不会生成重复任务。
7. 验证无 openId 用户进入 `WAIT_RECIPIENT`，不会无限重试。
8. 验证微信超时或临时错误进入指数退避重试，超过上限后为 `FAILED`。

## 八、上线边界

代码编译通过不等于微信真实发送通过。以下内容必须由真实环境完成：

- 小程序订阅授权；
- 模板 ID 与字段映射；
- appid/secret；
- 用户 openId；
- 微信接口返回码；
- 真机点击消息后的页面跳转。
