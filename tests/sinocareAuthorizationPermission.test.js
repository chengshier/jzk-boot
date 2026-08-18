const assert = require('assert');
const fs = require('fs');
const path = require('path');

const controller = fs.readFileSync(path.join(
  __dirname,
  '..',
  'crmeb-front',
  'src',
  'main',
  'java',
  'com',
  'zbkj',
  'front',
  'controller',
  'jiuzhoukang',
  'JkHealthController.java'
), 'utf8');

const prepareBlock = controller.match(/\/\*\* 供小程序获取[\s\S]*?public CommonResult<JkSinocareAuthorizationPrepareResponse> prepareSinocareAuthorization\(\)\{/);

assert.ok(prepareBlock, 'Sinocare authorization preparation endpoint must exist');
assert.doesNotMatch(prepareBlock[0], /@JkBizPermission/, 'Sinocare authorization preparation must not require a business role');
console.log('sinocare authorization permission test passed');
