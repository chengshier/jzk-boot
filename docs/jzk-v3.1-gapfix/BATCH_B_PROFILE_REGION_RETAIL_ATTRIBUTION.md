# V3.1 补漏批次 B：个人资料区域与零售订单归属

## 1. 修改文件清单

### 后端

- `crmeb-common/src/main/java/com/zbkj/common/model/user/User.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/user/UserAddress.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/order/StoreOrder.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkRetailOrderAttribution.java`
- `crmeb-common/src/main/java/com/zbkj/common/model/jiuzhoukang/JkRetailOrderAttributionAdjustment.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/UserAddressRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkUserProfileRegionSaveRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/request/jiuzhoukang/JkRetailAttributionResolveRequest.java`
- `crmeb-common/src/main/java/com/zbkj/common/response/jiuzhoukang/JkUserProfileRegionResponse.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/profile/JkUserProfileRegionService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/profile/JkUserProfileRegionServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/order/RetailOrderAttributionService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/order/RetailOrderAttributionServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/order/JkRetailAttributionAdminService.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/order/JkRetailAttributionAdminServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/OrderServiceImpl.java`
- `crmeb-service/src/main/java/com/zbkj/service/service/impl/UserAddressServiceImpl.java`
- `crmeb-front/src/main/java/com/zbkj/front/controller/jiuzhoukang/JkUserProfileRegionController.java`
- `crmeb-admin/src/main/java/com/zbkj/admin/controller/jiuzhoukang/JkUserProfileRegionAdminController.java`
- `crmeb-admin/src/main/java/com/zbkj/admin/controller/jiuzhoukang/JkRetailAttributionAdminController.java`

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

- 复用 `eb_user` 保存标准个人资料区域；
- `eb_user_address` 和 `eb_store_order` 分别保存本地址、本订单标准收货区域快照；
- `jk_retail_order_attribution` 保存逐订单明细不可变归属和金额快照；
- `jk_retail_order_attribution_adjustment` 仅记录锁定后的冲正和补偿请求；
- 不新增 `jk_user_region_profile` 或独立“服务区域”页面。

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

- 个人资料页增加“所在地区”；
- 收货地址页增加独立“标准区县”；
- 两处共用选择器，但保存目标和接口严格隔离；
- 管理端增加零售订单归属列表、解释抽屉和补偿式调整；
- 区县代理使用姓名/手机号选择器，不要求运营手填数据库用户 ID。

## 5. 自动化检查结果

- 后端已修复订单地址快照的 `Integer`/`Long` 类型错误；
- App CI 断言收货地址页不得调用个人资料区域保存接口；
- Admin CI 执行生产构建；
- 归属服务只按下单时关系、个人资料区域、本单收货区域和平台默认生成快照；
- 佣金、业绩和推广效果不重新查询当前关系。

以上是开发自检，不等于真实验收。

## 6. 未完成的真实环境验证

- MySQL 5.7 实跑两份升级 SQL 和历史数据兼容；
- 普通用户完成个人资料区域、收货地址、下单、支付、完成和退款；
- 多商品优惠、积分、满减、运费、尾差和部分退款分摊；
- 普通管理员数据范围、归属处理按钮和锁定后补偿权限；
- 无代理区域、无标准编码和历史地址缺失时的平台默认回退。

## 7. PR 状态

三个 PR 已记录本批次文件、SQL、接口、页面、自检和真实环境待验项，并继续保持 Draft。
