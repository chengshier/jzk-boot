# V3.1 补漏批次 A：数据库、菜单和权限纠错

## 1. 修改文件清单

- `sql/jiuzhoukang/v3.1-gapfix/00-jk-v31-schema-precheck.sql`
- `sql/jiuzhoukang/fix/jk_v31_phase3_menu_permission_patch.sql`
- `jzk-vue/src/utils/system.js`
- `jzk-vue/src/layout/component/columnsAside.vue`
- `jzk-boot/.github/workflows/jk-v31-gapfix-guards.yml`
- `jzk-vue/.github/workflows/jk-v31-gapfix-ui-guards.yml`

## 2. SQL 清单

- 真实结构预检：检查 `eb_system_menu.component/is_delete`，禁止真实写入继续依赖旧字段；
- 业务权限预检：检查 `jk_business_permission.module_code/enabled/tenant_id`；
- 菜单纠错：按 `component` 查询并纠正九州康根目录和第三批页面；
- 权限纠错：按真实非空列写入，不自动给普通运营角色授予高风险按钮；
- 不再创建没有真实页面的“身份与关系”空目录；
- 脚本结束时隐藏并逻辑删除所有无真实子节点的 `/operation/jzk/group/%` 目录。

## 3. 九洲康菜单 404 根因与修复

根因：九州康一级菜单点击时递归选择第一个子节点。数据库第一项可能是 `menu_type=M`、没有子页面的 `/operation/jzk/group/identity`，旧逻辑将该目录当作可跳转页面，最终命中前端通配路由 `/404`。

修复：

- `findFirstNonNullChildren` 跳过 `M` 目录和 `A` 按钮，并继续检查后续兄弟节点；
- 一级菜单找不到真实页面时回退 `/operation/jzk` 自身，由路由 redirect 进入可用页面；
- SQL 自动清理空分组，防止数据库重新执行补丁后复现；
- 不把平台订货地址硬编码为所有账号的唯一入口。

当前数据库已存在空目录时，应重新执行最新菜单补丁，并退出重登、清除 `MerPlatAdmin_MenuList` 与 `MerPlatAdmin_oneLvRoutes` 缓存。

## 4. 接口和页面

本批次不新增业务接口。库存盘点、异常收货、推广码、健康报告和订阅消息任务继续挂入真实业务分组；目录只负责展开，页面节点才负责跳转。

## 5. 自动化检查结果

- SQL 守卫只检查真实 `INSERT/UPDATE`，不会把预检查脚本中对旧字段的文字检测误报为违规；
- 菜单和权限种子使用 `NOT EXISTS` 或先查后更新，父菜单动态解析；
- 后端守卫检查空分组清理 SQL；
- 管理端守卫检查首跳必须跳过目录和按钮节点。

以上属于代码与自动化自检，不等于真实权限验收。

## 6. 未完成的真实环境验证

- 在真实 MySQL 5.7 执行结构预检、菜单补丁和历史重复项清理；
- 使用超管、普通管理员和不同角色验证菜单、按钮、数据范围和接口权限；
- 验证退出重登和浏览器本地菜单缓存刷新；
- 验证没有平台订货权限的账号仍能进入其拥有权限的第一个九州康页面。

## 7. PR 状态

三个 PR 已补充菜单 404 根因、修复和验收边界；真实环境验证完成前继续保持 Draft。
