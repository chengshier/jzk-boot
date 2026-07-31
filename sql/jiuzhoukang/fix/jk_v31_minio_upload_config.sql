-- MinIO 文件上传配置（CRMEB v1.4）
-- 可重复执行：新增 MinIO 表单、上传类型、配置项和连接测试权限。

SET @now = NOW();

-- 基础“文件上传-本地配置”表单的上传类型增加 MinIO（type=6）。
UPDATE `eb_system_form_temp`
SET `content` = JSON_SET(
  `content`,
  '$.fields[5].__slot__.options',
  JSON_ARRAY(
    JSON_OBJECT('label', '本地', 'value', 1),
    JSON_OBJECT('label', '七牛云', 'value', 2),
    JSON_OBJECT('label', '阿里云', 'value', 3),
    JSON_OBJECT('label', '腾讯云', 'value', 4),
    JSON_OBJECT('label', '京东云', 'value', 5),
    JSON_OBJECT('label', 'MinIO', 'value', 6)
  )
)
WHERE `id` = 108 AND JSON_VALID(`content`);

-- MinIO 动态表单。secret key 使用 el-input 的 password 配置。
SET @minio_form_name = '文件上传-MinIO配置';
SET @minio_form_content = '{"formRef":"elForm","formModel":"formData","size":"medium","labelPosition":"right","labelWidth":150,"formRules":"rules","gutter":15,"disabled":false,"span":24,"formBtns":true,"fields":[{"__config__":{"label":"MinIO Endpoint：","labelWidth":150,"showLabel":true,"changeTag":true,"tag":"el-input","tagIcon":"input","required":true,"layout":"colFormItem","span":24,"document":"https://element.eleme.cn/#/zh-CN/component/input","regList":[],"tips":false},"__slot__":{"prepend":"","append":""},"placeholder":"例如：https://minio.example.com","style":{"width":"50%"},"clearable":true,"prefix-icon":"","suffix-icon":"","maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"minioEndpoint"},{"__config__":{"label":"Bucket：","labelWidth":150,"showLabel":true,"changeTag":true,"tag":"el-input","tagIcon":"input","required":true,"layout":"colFormItem","span":24,"document":"https://element.eleme.cn/#/zh-CN/component/input","regList":[],"tips":false},"__slot__":{"prepend":"","append":""},"placeholder":"请输入存储桶名称","style":{"width":"50%"},"clearable":true,"prefix-icon":"","suffix-icon":"","maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"minioBucket"},{"__config__":{"label":"Access Key：","labelWidth":150,"showLabel":true,"changeTag":true,"tag":"el-input","tagIcon":"input","required":true,"layout":"colFormItem","span":24,"document":"https://element.eleme.cn/#/zh-CN/component/input","regList":[],"tips":false},"__slot__":{"prepend":"","append":""},"placeholder":"请输入 Access Key","style":{"width":"50%"},"clearable":true,"prefix-icon":"","suffix-icon":"","maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"minioAccessKey"},{"__config__":{"label":"Secret Key：","labelWidth":150,"showLabel":true,"changeTag":true,"tag":"el-input","tagIcon":"password","required":true,"layout":"colFormItem","span":24,"document":"https://element.eleme.cn/#/zh-CN/component/input","regList":[],"tips":false},"__slot__":{"prepend":"","append":""},"placeholder":"请输入 Secret Key","show-password":true,"style":{"width":"50%"},"clearable":true,"prefix-icon":"","suffix-icon":"","maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"minioSecretKey"},{"__config__":{"label":"Region：","labelWidth":150,"showLabel":true,"changeTag":true,"tag":"el-input","tagIcon":"input","required":true,"layout":"colFormItem","span":24,"document":"https://element.eleme.cn/#/zh-CN/component/input","regList":[],"tips":false},"__slot__":{"prepend":"","append":""},"placeholder":"例如：us-east-1","style":{"width":"50%"},"clearable":true,"prefix-icon":"","suffix-icon":"","maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"minioRegion"},{"__config__":{"label":"对象前缀：","labelWidth":150,"showLabel":true,"changeTag":true,"tag":"el-input","tagIcon":"input","required":false,"layout":"colFormItem","span":24,"document":"https://element.eleme.cn/#/zh-CN/component/input","regList":[],"tips":false},"__slot__":{"prepend":"","append":""},"placeholder":"可选，例如：uploads/","style":{"width":"50%"},"clearable":true,"prefix-icon":"","suffix-icon":"","maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"minioPrefix"},{"__config__":{"label":"访问域名：","labelWidth":150,"showLabel":true,"changeTag":true,"tag":"el-input","tagIcon":"input","required":true,"layout":"colFormItem","span":24,"document":"https://element.eleme.cn/#/zh-CN/component/input","regList":[],"tips":false},"__slot__":{"prepend":"","append":""},"placeholder":"文件访问域名，未单独配置时与 Endpoint 相同","style":{"width":"50%"},"clearable":true,"prefix-icon":"","suffix-icon":"","maxlength":null,"show-word-limit":false,"readonly":false,"disabled":false,"__vModel__":"minioUploadUrl"}]}';
SET @minio_form_content = JSON_SET(@minio_form_content, '$.fields[3].type', 'password');

INSERT INTO `eb_system_form_temp` (`name`, `info`, `content`, `create_time`, `update_time`)
SELECT @minio_form_name, '文件上传-MinIO配置', @minio_form_content, @now, @now
WHERE NOT EXISTS (
  SELECT 1 FROM `eb_system_form_temp` WHERE `name` = @minio_form_name
);

UPDATE `eb_system_form_temp`
SET `info` = '文件上传-MinIO配置', `content` = @minio_form_content, `update_time` = @now
WHERE `name` = @minio_form_name;

SET @minio_form_id = (
  SELECT `id` FROM `eb_system_form_temp` WHERE `name` = @minio_form_name ORDER BY `id` ASC LIMIT 1
);

-- 将 MinIO 表单作为“文件上传配置”的子 Tab。
INSERT INTO `eb_category` (`pid`, `path`, `name`, `type`, `url`, `extra`, `status`, `sort`, `create_time`, `update_time`)
SELECT 108, '/0/108/', 'MinIO配置', 6, 'MinIO配置', CAST(@minio_form_id AS CHAR), 1, 1, @now, @now
WHERE @minio_form_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `eb_category` WHERE `pid` = 108 AND `type` = 6 AND `name` = 'MinIO配置'
  );

UPDATE `eb_category`
SET `path` = '/0/108/', `type` = 6, `url` = 'MinIO配置', `extra` = CAST(@minio_form_id AS CHAR), `status` = 1, `update_time` = @now
WHERE `pid` = 108 AND `type` = 6 AND `name` = 'MinIO配置';

-- MinIO 配置项。已有值不会被覆盖。
INSERT INTO `eb_system_config` (`name`, `title`, `form_id`, `value`, `status`, `create_time`, `update_time`)
SELECT config_seed.`name`, config_seed.`title`, @minio_form_id, config_seed.`value`, 0, @now, @now
FROM (
  SELECT 'minioEndpoint' AS `name`, 'minioEndpoint' AS `title`, '' AS `value`
  UNION ALL SELECT 'minioBucket', 'minioBucket', ''
  UNION ALL SELECT 'minioAccessKey', 'minioAccessKey', ''
  UNION ALL SELECT 'minioSecretKey', 'minioSecretKey', ''
  UNION ALL SELECT 'minioRegion', 'minioRegion', 'us-east-1'
  UNION ALL SELECT 'minioPrefix', 'minioPrefix', ''
  UNION ALL SELECT 'minioUploadUrl', 'minioUploadUrl', ''
) AS config_seed
WHERE @minio_form_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `eb_system_config` config_item WHERE config_item.`name` = config_seed.`name`
  );

-- 兼容已存在的 MinIO 配置项：只修复表单关联，绝不覆盖现有值。
UPDATE `eb_system_config`
SET `form_id` = @minio_form_id, `update_time` = @now
WHERE @minio_form_id IS NOT NULL
  AND `name` IN ('minioEndpoint', 'minioBucket', 'minioAccessKey', 'minioSecretKey', 'minioRegion', 'minioPrefix', 'minioUploadUrl')
  AND (`form_id` IS NULL OR `form_id` <> @minio_form_id);

-- 连接测试权限挂在既有“系统设置”菜单下，并赋予已有该菜单权限的角色。
SET @system_config_menu_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `is_delte` = 0 AND `perms` = 'admin:system:config:info'
  ORDER BY `id` ASC LIMIT 1
);

INSERT INTO `eb_system_menu` (`pid`, `name`, `icon`, `perms`, `component`, `menu_type`, `sort`, `is_show`, `is_delte`, `create_time`, `update_time`)
SELECT @system_config_menu_id, '测试 MinIO 连接', NULL, 'admin:system:config:minio:test', '', 'A', 1, 1, 0, @now, @now
WHERE @system_config_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `eb_system_menu` WHERE `perms` = 'admin:system:config:minio:test'
  );

SET @minio_test_menu_id = (
  SELECT `id` FROM `eb_system_menu`
  WHERE `perms` = 'admin:system:config:minio:test'
  ORDER BY `is_delte` ASC, `id` ASC LIMIT 1
);

UPDATE `eb_system_menu`
SET `pid` = @system_config_menu_id, `name` = '测试 MinIO 连接', `component` = '', `menu_type` = 'A',
    `sort` = 1, `is_show` = 1, `is_delte` = 0, `update_time` = @now
WHERE `id` = @minio_test_menu_id AND @system_config_menu_id IS NOT NULL;

INSERT INTO `eb_system_role_menu` (`rid`, `menu_id`)
SELECT role_menu.`rid`, @minio_test_menu_id
FROM `eb_system_role_menu` role_menu
WHERE role_menu.`menu_id` = @system_config_menu_id
  AND @minio_test_menu_id IS NOT NULL
ON DUPLICATE KEY UPDATE `menu_id` = VALUES(`menu_id`);
