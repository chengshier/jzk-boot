# V3.1 补漏批次 B：个人资料区域与零售订单归属

## 1. 修改文件清单

### 后端
- `crmeb-common/.../user/User.java`
- `crmeb-common/.../user/UserAddress.java`
- `crmeb-common/.../order/StoreOrder.java`
- `crmeb-common/.../jiuzhoukang/JkRetailOrderAttribution.java`
- `crmeb-common/.../jiuzhoukang/JkRetailOrderAttributionAdjustment.java`
- `crmeb-common/.../request/jiuzhoukang/JkUserProfileRegionSaveRequest.java`
- `crmeb-common/.../request/jiuzhoukang/JkRetailAttributionResolveRequest.java`
- `crmeb-common/.../response/jiuzhoukang/JkUserProfileRegionResponse.java`
- `crmeb-service/.../profile/JkUserProfileRegionService.java`
- `crmeb-service/.../profile/JkUserProfileRegionServiceImpl.java`
- `crmeb-service/.../order/RetailOrderAttributionServiceImpl.java`
- `crmeb-service/.../order/JkRetailAttributionAdminService.java`
- `crmeb-service/.../order/JkRetailAttributionAdminServiceImpl.java`
- `crmeb-front/.../JkUserProfileRegionController.java`
- `crmeb-admin/.../JkUserProfileRegionAdminController.java`
- `crmeb-admin/.../JkRetailAttributionAdminController.java`
- `crmeb-service/.../OrderServiceImpl.java`
- `crmeb-service/.../UserAddressServiceImpl.java`

### 管理端
- `jzk-vue/src/api/jkGapfix.js`
- `jzk-vue/src/views/jkBusiness/retailAttribution/index.vue`
- `jzk-vue/src/router/modules/jkBusiness.js`

### 小程序
- `jzk-app/api/jkV31.js`
- `jzk-app/components/jk-region-picker/index.vue`
- `jzk-app/pages/infos/user_info/index.vue`
- `jzk-app/pages/users/user_address/index.vue`

## 2. SQL 清单

- `sql/jiuzhoukang/v3.1-gapfix/01-jk-v31-gapfix-structure.sql`
- `sql/jiuzhoukang/v3.1-gapfix/02-jk-v31-attribution-menu-permission.sql`

核心结构：
- 复用 `eb_user` 增加标准个人资料区域字段；
- `eb_user_address` 和 `eb_store_order` 保存本地址、本订单标准收货区域快照；
- 扩展 `jk_retail_order_attribution` 为逐订单明细不可变快照；
- 新增 `jk_retail_order_attribution_adjustment`，锁定后只记录冲正和补偿请求；
- 不新增 `jk_user_region_profile` 或独立“服务区域”菜单。

## 3. 接口清单

- `GET /api/front/jk/user-profile/region`
- `POST /api/front/jk/user-profile/region`
- `GET /api/front/jk/region/options`
- `GET /api/admin/jk/user/{userId}/region`
- `POST /api/admin/jk/user/{userId}/region`
- `GET /api/admin/jk/retail-attribution/list`
- `GET /api/admin/jk/retail-attribution/{id}`
- `GET /api/admin/jk/retail-attribution/{id}/overview`
- `GET /api/admin/jk/retail-attribution/{id}/adjustments`
- `POST /api/admin/jk/retail-attribution/{id}/resolve`
- `POST /api/admin/jk/retail-attribution/{id}/adjust`

## 4. 页面清单

- 复用小程序现有个人资料页增加“所在地区”；
- 复用小程序现有收货地址页增加独立“标准区县”；
- 新增管理端“零售订单归属”列表、解释抽屉和补偿式调整；
- 管理端未新增独立“用户服务区域”页面；
- 区县代理使用姓名/手机号业务选择器，不要求运营手填数据库用户 ID。

## 5. 自动化检查结果

- 后端一致性 CI 曾发现 `Integer` 到 `Long` 的订单地址快照类型错误，已修复；修复后当时三项后端检查通过。
- App CI 增加个人资料区域与收货区域隔离断言：收货地址页不得调用个人资料区域保存接口。
- Admin CI 在唯一累计分支执行生产构建。
- 归属服务静态核对：只按下单时关系、个人资料标准区域、本单标准收货区域和平台默认顺序生成快照；佣金端不重新查询当前关系。

以上是开发自检，不等于真实验收通过。

## 6. 未完成的真实环境验证

- 未在真实 MySQL 5.7 执行两份升级 SQL 和历史数据兼容检查；
- 未使用真实普通用户完成个人资料区域保存、收货地址保存、下单、支付、完成和退款；
- 未验证多商品优惠分摊、积分、满减和部分退款金额在真实订单数据上的结果；
- 未使用真实普通管理员 Token 验证数据范围、归属处理按钮和锁定后补偿权限；
- 未验证区域无代理、地址无标准编码和历史地址缺失时的平台默认回退。

## 7. PR 描述更新

- 后端 `jzk-boot#5`、管理端 `jzk-vue#4`、小程序 `jzk-app#4` 增加本批次文件、SQL、接口、页面、自检和真实环境待验项；
- 三个 PR 继续保持 Draft；
- 不将编译或生产构建成功表述为验收通过。
