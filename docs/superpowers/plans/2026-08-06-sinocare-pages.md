# Sinocare Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide the Sinocare H5 authorization entry in the mini program and operational configuration, callback-log, and retry controls in admin.

**Architecture:** The front service creates the H5 URL from server-owned configuration and the authenticated user’s persisted uniqueId. Admin exposes only configuration readiness and sanitized callback-log metadata; retry reuses the existing callback processor rather than accepting arbitrary payloads.

**Tech Stack:** Java 8, Spring Boot, MyBatis-Plus, Vue 2/Element UI, uni-app.

---

### Task 1: Secure authorization-link API

**Files:**
- Modify: `crmeb-front/src/main/java/com/zbkj/front/controller/jiuzhoukang/JkHealthController.java`
- Modify: `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/health/SinocareAuthorizationService.java`
- Modify: `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/health/SinocareAuthorizationServiceImpl.java`
- Test: `crmeb-service/src/test/java/com/zbkj/service/service/impl/jiuzhoukang/health/SinocareAuthorizationLinkTest.java`

- [ ] Write a failing test asserting that a 32-character stored uniqueId produces an URL containing only server-configured `appId`, that uniqueId, and URL-encoded redirectUrl.
- [ ] Run the test and confirm it fails because `buildAuthorizationUrl` does not exist.
- [ ] Implement `buildAuthorizationUrl(Long userId, String redirectUrl)` with nonblank configuration validation and URL encoding.
- [ ] Return only `uniqueId` and `authorizationUrl` from the authenticated front endpoint.
- [ ] Re-run the focused test and commit the backend API.

### Task 2: Admin configuration/log/retry API

**Files:**
- Modify: `crmeb-admin/src/main/java/com/zbkj/admin/controller/jiuzhoukang/JkHealthAdminController.java`
- Modify: `crmeb-service/src/main/java/com/zbkj/service/service/jiuzhoukang/health/SinocareCallbackService.java`
- Modify: `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/health/SinocareCallbackServiceImpl.java`
- Modify: `crmeb-service/src/main/java/com/zbkj/service/service/impl/jiuzhoukang/health/SinocareCallbackProcessor.java`
- Test: `crmeb-service/src/test/java/com/zbkj/service/service/impl/jiuzhoukang/health/SinocareCallbackServiceTest.java`

- [ ] Write a failing test for listing callback logs without ciphertext/signature and retrying only a failed log.
- [ ] Run it and confirm the sanitised listing/retry API is absent.
- [ ] Add filtered listing and a retry operation that rejects statuses other than FAILED, then expose admin endpoints protected by the existing health-admin permission.
- [ ] Re-run focused tests and commit the API.

### Task 3: Mini-program entry

**Files:**
- Modify: `app/api/health.js`
- Modify: `app/pages/jk/health/device.vue`
- Test: `app/tests/unit/api/health.spec.js`

- [ ] Write a failing API test for POST `/api/front/jk/health/sinocare/authorization/prepare` with redirectUrl.
- [ ] Add the API wrapper, device card and click handler that opens the returned URL in the existing WebView-compatible navigation method.
- [ ] Re-run the focused test and build/lint available for the app.
- [ ] Commit in the app repository.

### Task 4: Admin screen

**Files:**
- Modify: `admin/src/api/jkBusiness.js`
- Modify: `admin/src/views/jkBusiness/healthIntegration/index.vue`
- Create: `admin/src/views/jkBusiness/sinocareCallback/index.vue`
- Modify: `admin/src/router/modules/jkBusiness.js` (or the repository’s existing health menu registration file)
- Test: `admin/tests/unit/views/sinocareCallback.spec.js`

- [ ] Write a failing component/API test for rendering readiness fields and callback rows without payload fields.
- [ ] Add admin API wrappers, the configuration tab and callback/retry tab, using the protected backend endpoints.
- [ ] Register the route/menu using the existing health-management pattern.
- [ ] Run focused test/lint and commit in the admin repository.

### Task 5: Integrated verification and handoff

**Files:**
- Modify: `sql/jk_sinocare_integration.sql` only if query/retry indexes are needed

- [ ] Run the focused backend tests under the explicit JDK 8 path, plus app/admin lint or focused tests.
- [ ] Record the pre-existing full-build failure separately from Sinocare verification.
- [ ] Inspect all diffs, merge the three temporary branches into the user’s current branches only after a clean merge, and remove temporary worktrees/branches as requested.
