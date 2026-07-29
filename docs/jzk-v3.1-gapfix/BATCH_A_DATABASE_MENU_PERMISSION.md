# V3.1 补漏批次 A：数据库、菜单和权限纠错

## 1. 修改文件清单

- `sql/jiuzhoukang/v3.1-gapfix/00-jk-v31-schema-precheck.sql`
- `sql/jiuzhoukang/fix/jk_v31_phase3_menu_permission_patch.sql`

## 2. SQL 清单

- 真实结构预检：检查 `eb_system_menu.component/is_delete`、禁止继续依赖 `path/is_del`。
- 业务权限预检：检查 `jk_business_permission.module_code/enabled/tenant_id`、禁止继续依赖 `parent_code`。
- 菜单纠错：按 `component` 查询和写入九州康根目录、V3.1 分组和第三批页面。
- 权限纠错：按真实非空列写入，不自动给普通运营角色授予高风险按钮。

## 3. 接口清单

本批次不新增业务接口；只修复接口对应的菜单和 authority 初始化。

## 4. 页面清单

本批次不新增页面；将现有库存盘点、异常收货、推广码、健康报告和订阅消息任务纳入正确分组。

## 5. 自动化检查结果

- 已完成 SQL 静态字段检查：新脚本不包含 `eb_system_menu.path` 写入、不包含 `is_del` 写入、不包含 `jk_business_permission.parent_code` 写入。
- 已完成幂等性检查：所有菜单和权限使用 `NOT EXISTS` 或先查后更新。
- 已完成固定 ID 检查：父菜单通过名称和 `component` 动态解析。

以上属于代码与静态检查，不等同于验收通过。

## 6. 未完成的真实环境验证

- 尚未在真实 MySQL 5.7 执行结构预检和升级脚本。
- 尚未验证实际租户默认值、历史菜单重复项和角色授权数据。
- 尚未使用真实普通管理员 Token 验证菜单、路由、按钮和接口 authority 一致性。

## 7. PR 描述更新

- `jzk-boot#5`、`jzk-vue#4`、`jzk-app#4` 增加“V1.1 补漏批次 A”说明。
- PR 继续保持 Draft；不宣称 UAT 或真实数据库验收通过。
