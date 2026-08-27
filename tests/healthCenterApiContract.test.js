const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), 'utf8');
const controller = read('crmeb-front', 'src', 'main', 'java', 'com', 'zbkj', 'front', 'controller', 'jiuzhoukang', 'JkHealthController.java');
const service = read('crmeb-service', 'src', 'main', 'java', 'com', 'zbkj', 'service', 'service', 'jiuzhoukang', 'health', 'JkHealthService.java');
const processor = read('crmeb-service', 'src', 'main', 'java', 'com', 'zbkj', 'service', 'service', 'impl', 'jiuzhoukang', 'health', 'SinocareCallbackProcessor.java');
const callbackService = read('crmeb-service', 'src', 'main', 'java', 'com', 'zbkj', 'service', 'service', 'impl', 'jiuzhoukang', 'health', 'SinocareCallbackServiceImpl.java');

assert.match(controller, /@GetMapping\("\/sinocare\/device\/status"\)[\s\S]{0,240}@JkBizPermission[\s\S]{0,240}deviceStatus\(/,
  'device status must be an authenticated front endpoint');
assert.match(controller, /@GetMapping\("\/glucose\/trend"\)[\s\S]{0,240}@JkBizPermission[\s\S]{0,240}glucoseTrend\(/,
  'glucose trend must be an authenticated front endpoint');
assert.match(service, /JkSinocareDeviceStatusResponse\s+deviceStatus\(Long userId\)/,
  'health service must expose authorization/session device state');
assert.match(service, /JkGlucoseTrendResponse\s+glucoseTrend\(Long userId, (?:java\.util\.)?Date startAt, (?:java\.util\.)?Date endAt\)/,
  'health service must expose glucose trend data');

const deviceResponse = read('crmeb-common', 'src', 'main', 'java', 'com', 'zbkj', 'common', 'response', 'jiuzhoukang', 'JkSinocareDeviceStatusResponse.java');
for (const field of ['authorized', 'hasGlucoseData', 'productName', 'deviceSn', 'status', 'detectionStartTime', 'detectionEndTime', 'lastDataAt']) {
  assert.match(deviceResponse, new RegExp(`\\b${field}\\b`), `device response must contain ${field}`);
}
const trendResponse = read('crmeb-common', 'src', 'main', 'java', 'com', 'zbkj', 'common', 'response', 'jiuzhoukang', 'JkGlucoseTrendResponse.java');
for (const field of ['startAt', 'endAt', 'average', 'minimum', 'maximum', 'count', 'points']) {
  assert.match(trendResponse, new RegExp(`\\b${field}\\b`), `trend response must contain ${field}`);
}

assert.match(processor, /safeDate\(body,\s*"detectionStartTime"\)/,
  '1002 must tolerate absent detection start timestamp');
assert.match(processor, /safeDate\(body,\s*"detectionEndTime"\)/,
  '1002 must tolerate absent detection end timestamp');
assert.match(processor, /markSessionHasData\(body, log\.getUniqueId\(\)\)/,
  '1003 must mark the matching device session after receiving data');
assert.match(processor, /row\.setStatus\(1\)\.setDetectionEndTime\(null\)/,
  'a newer CGM callback must reactivate a stale device session and clear its obsolete end time');
assert.match(processor, /Date startAt\s*=\s*safeDate\(body,\s*"detectionStartTime"\);\s*if\s*\(startAt\s*!=\s*null\)\s*row\.setDetectionStartTime\(startAt\)/,
  '1002 must keep a previously stored detection start time when a replay omits it');
assert.match(processor, /Date endAt\s*=\s*safeDate\(body,\s*"detectionEndTime"\);\s*if\s*\(endAt\s*!=\s*null\)\s*row\.setDetectionEndTime\(endAt\)/,
  '1002 must keep a previously stored detection end time when a replay omits it');
assert.match(processor, /if\(row==null\)row=new JkSinocareDeviceSession\(\)\.setUniqueId\(uniqueId\)\.setDeviceSn\(sn\)/,
  '1003 must create a device session when its 1002 callback has not arrived');
assert.match(processor, /!"1001"\.equals\(log\.getEventType\(\)\)\s*&&\s*!"AUTHORIZED"\.equals\(auth\.getStatus\(\)\)/,
  'callbacks other than authorization must not mutate state for a non-authorized mapping');
const cgmIngestion = read('crmeb-service', 'src', 'main', 'java', 'com', 'zbkj', 'service', 'service', 'impl', 'jiuzhoukang', 'health', 'SinocareCgmIngestionService.java');
assert.match(cgmIngestion, /healthService\.evaluateAlerts\(/,
  'Sinocare glucose ingestion must use the normal alert/risk evaluation');
assert.doesNotMatch(callbackService, /setEventId\("RECEIVED-"/,
  'callback idempotency must not use a generated event id that changes during processing');
assert.match(callbackService, /callbackEventKey\(eventType, envelope\)/,
  'callback idempotency must use a stable event key before insertion');
assert.match(controller, /@GetMapping\("\/sinocare\/report\/list"\)[\s\S]{0,260}sinocareReports\(/,
  'report list must be an authenticated owner endpoint');
assert.match(controller, /@GetMapping\("\/sinocare\/report\/\{id\}"\)[\s\S]{0,260}sinocareReport\(/,
  'report detail must be an authenticated owner endpoint');
assert.match(service, /List<JkSinocareReportResponse>\s+sinocareReports\(Long userId\)/,
  'health service must expose owner-scoped report list');
assert.match(service, /JkSinocareReportResponse\s+sinocareReport\(Long userId, Long reportId\)/,
  'health service must expose owner-scoped report detail');
assert.match(controller, /@DateTimeFormat\(iso = DateTimeFormat\.ISO\.DATE_TIME\)/,
  'trend query dates must have explicit parsing');
assert.doesNotMatch(callbackService, /new StringBuilder\("RCV-"\)/,
  'receipt id must fit callback_log.event_id varchar(64)');
assert.match(callbackService, /return value\.toString\(\);/,
  'receipt id must be the raw 64-character SHA-256 hex');
assert.match(processor, /String providerEventId=body\.getString\("id"\)/,
  'processor must keep the provider business id separate from immutable receipt id');
assert.match(processor, /applyReport\(log, body, providerEventId\)/,
  'report dedupe must use the decrypted provider business id');
assert.match(cgmIngestion, /@Transactional\(rollbackFor = Exception\.class\)/,
  'CGM insert and alert evaluation must be wrapped in an independent transaction');
assert.match(processor, /cgmIngestionService\.ingest\(/,
  'callback processor must use the transactional CGM ingestion service');
console.log('health center API contract test passed');
