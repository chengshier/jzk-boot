const assert = require('assert');
const fs = require('fs');
const path = require('path');

const source = fs.readFileSync(path.join(
  __dirname,
  '..',
  'crmeb-service',
  'src',
  'main',
  'java',
  'com',
  'zbkj',
  'service',
  'service',
  'impl',
  'jiuzhoukang',
  'context',
  'JkUserContextServiceImpl.java'
), 'utf8');

const fallback = source.match(/private void fillAnonymousContext[\s\S]*?\n    }\n\n    private List<String> resolveCanApplyRoles/);
assert.ok(fallback, 'normal user context builder must exist');
assert.match(fallback[0], /businessRoleService\.getPermissionCodes\(normalUser\.getId\(\)\)/,
  'normal users must inherit the permissions configured for the normal_user role');
console.log('normal user permission context test passed');
