package com.zbkj.service.service.jiuzhoukang.region;

import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.request.jiuzhoukang.JkRegionSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkRegionPathResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionTreeNodeResponse;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkCommissionRuleDao;
import com.zbkj.service.dao.jiuzhoukang.JkFundAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkIdentityApplyDao;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkProductPriceRuleDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionAgentDao;
import com.zbkj.service.dao.jiuzhoukang.JkRegionDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailRefundAdjustmentDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockAccountDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferReturnDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserBusinessRoleDao;
import com.zbkj.service.dao.jiuzhoukang.JkUserDataScopeDao;
import com.zbkj.service.service.impl.jiuzhoukang.region.JkRegionServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class JkRegionServiceImplTest {

    @Test
    public void listsOnlyDirectChildrenAndCalculatesChildSummary() {
        List<JkRegion> regions = Arrays.asList(
                region(1L, "410000", "河南省", null, 1, true),
                region(2L, "410400", "平顶山市", "410000", 2, true),
                region(3L, "410423", "鲁山县", "410400", 3, true),
                region(4L, "410425", "郏县", "410400", 3, false),
                region(5L, "410100", "郑州市", "410000", 2, true)
        );
        JkRegionServiceImpl service = serviceWithRegions(regions);

        List<JkRegionTreeNodeResponse> children = service.listChildren("410000", null);

        Assert.assertEquals(2, children.size());
        Assert.assertEquals("410100", children.get(0).getRegionCode());
        Assert.assertFalse(children.get(0).getHasChildren());
        Assert.assertEquals(Integer.valueOf(0), children.get(0).getChildCount());
        Assert.assertEquals("410400", children.get(1).getRegionCode());
        Assert.assertTrue(children.get(1).getHasChildren());
        Assert.assertEquals(Integer.valueOf(2), children.get(1).getChildCount());
    }

    @Test
    public void buildsFullPathForTargetRegion() {
        List<JkRegion> regions = Arrays.asList(
                region(1L, "410000", "河南省", null, 1, true),
                region(2L, "410400", "平顶山市", "410000", 2, true),
                region(3L, "410423", "鲁山县", "410400", 3, true)
        );
        JkRegionServiceImpl service = serviceWithRegions(regions);

        JkRegionPathResponse response = service.getRegionPath("410423");

        Assert.assertEquals("河南省 / 平顶山市 / 鲁山县", response.getFullPathName());
        Assert.assertEquals(Arrays.asList("410000", "410400", "410423"), response.getFullPathCodes());
        Assert.assertEquals("410423", response.getCurrent().getRegionCode());
        Assert.assertEquals(3, response.getNodes().size());
    }

    @Test
    public void rejectsStatusDisableWhenEnabledChildrenExist() {
        List<JkRegion> regions = Arrays.asList(
                region(1L, "410000", "河南省", null, 1, true),
                region(2L, "410400", "平顶山市", "410000", 2, true)
        );
        JkRegionServiceImpl service = serviceWithRegions(regions);

        try {
            service.updateStatus(1L, false, 99L);
            Assert.fail("expected enabled child validation");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("启用子区域"));
        }
    }

    @Test
    public void rejectsChangingRegionCodeWhenBusinessUsageExists() {
        JkRegion existing = region(3L, "410423", "鲁山县", "410400", 3, true);
        List<JkRegion> regions = Arrays.asList(
                region(1L, "410000", "河南省", null, 1, true),
                region(2L, "410400", "平顶山市", "410000", 2, true),
                existing
        );
        AtomicReference<JkRegion> updated = new AtomicReference<>();
        JkRegionServiceImpl service = serviceWithRegions(regions);
        ReflectionTestUtils.setField(service, "regionDao", proxy(JkRegionDao.class, (method, args) -> {
            String name = method.getName();
            if ("selectById".equals(name)) {
                return Long.valueOf(3L).equals(args[0]) ? existing : null;
            }
            if ("selectOne".equals(name)) {
                return findOne((com.baomidou.mybatisplus.core.conditions.Wrapper<JkRegion>) args[0], regions);
            }
            if ("selectList".equals(name)) {
                return findList((com.baomidou.mybatisplus.core.conditions.Wrapper<JkRegion>) args[0], regions);
            }
            if ("updateById".equals(name)) {
                updated.set((JkRegion) args[0]);
                return 1;
            }
            if ("insert".equals(name)) {
                updated.set((JkRegion) args[0]);
                return 1;
            }
            return null;
        }));
        ReflectionTestUtils.setField(service, "userBusinessRoleDao", countDao(JkUserBusinessRoleDao.class, 1L));
        wireZeroUsageDaos(service);

        JkRegionSaveRequest request = new JkRegionSaveRequest();
        request.setId(3L);
        request.setRegionCode("410424");
        request.setRegionName("鲁山县");
        request.setParentRegionCode("410400");
        request.setStatus(true);

        try {
            service.save(request, 88L);
            Assert.fail("expected region code usage validation");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("区域编码"));
        }
        Assert.assertNull(updated.get());
    }

    private JkRegionServiceImpl serviceWithRegions(List<JkRegion> regions) {
        JkRegionServiceImpl service = new JkRegionServiceImpl();
        ReflectionTestUtils.setField(service, "regionDao", proxy(JkRegionDao.class, (method, args) -> {
            String name = method.getName();
            if ("selectList".equals(name)) {
                return findList((com.baomidou.mybatisplus.core.conditions.Wrapper<JkRegion>) args[0], regions);
            }
            if ("selectOne".equals(name)) {
                return findOne((com.baomidou.mybatisplus.core.conditions.Wrapper<JkRegion>) args[0], regions);
            }
            if ("selectById".equals(name)) {
                Long id = (Long) args[0];
                for (JkRegion region : regions) {
                    if (id.equals(region.getId())) {
                        return region;
                    }
                }
                return null;
            }
            if ("updateById".equals(name) || "insert".equals(name)) {
                return 1;
            }
            return null;
        }));
        wireZeroUsageDaos(service);
        return service;
    }

    private void wireZeroUsageDaos(JkRegionServiceImpl service) {
        ReflectionTestUtils.setField(service, "regionAgentDao", countDao(JkRegionAgentDao.class, 0L));
        ReflectionTestUtils.setField(service, "userBusinessRoleDao", countDao(JkUserBusinessRoleDao.class, 0L));
        ReflectionTestUtils.setField(service, "userDataScopeDao", countDao(JkUserDataScopeDao.class, 0L));
        ReflectionTestUtils.setField(service, "priceRuleDao", countDao(JkProductPriceRuleDao.class, 0L));
        ReflectionTestUtils.setField(service, "stockAccountDao", countDao(JkStockAccountDao.class, 0L));
        ReflectionTestUtils.setField(service, "retailOrderAttributionDao", countDao(JkRetailOrderAttributionDao.class, 0L));
        ReflectionTestUtils.setField(service, "identityApplyDao", countDao(JkIdentityApplyDao.class, 0L));
        ReflectionTestUtils.setField(service, "platformOrderDao", countDao(JkPlatformOrderDao.class, 0L));
        ReflectionTestUtils.setField(service, "stockTransferDao", countDao(JkStockTransferDao.class, 0L));
        ReflectionTestUtils.setField(service, "stockTransferReturnDao", countDao(JkStockTransferReturnDao.class, 0L));
        ReflectionTestUtils.setField(service, "commissionAccountDao", countDao(JkCommissionAccountDao.class, 0L));
        ReflectionTestUtils.setField(service, "fundAccountDao", countDao(JkFundAccountDao.class, 0L));
        ReflectionTestUtils.setField(service, "retailRefundAdjustmentDao", countDao(JkRetailRefundAdjustmentDao.class, 0L));
        ReflectionTestUtils.setField(service, "commissionRuleDao", countDao(JkCommissionRuleDao.class, 0L));
    }

    private JkRegion region(Long id, String code, String name, String parentCode, int level, boolean status) {
        return new JkRegion()
                .setId(id)
                .setRegionCode(code)
                .setRegionName(name)
                .setParentRegionCode(parentCode)
                .setRegionLevel(level)
                .setStatus(status)
                .setIsDeleted(false)
                .setOccupied(false)
                .setCreateTime(new Date())
                .setUpdateTime(new Date());
    }

    private List<JkRegion> findList(com.baomidou.mybatisplus.core.conditions.Wrapper<JkRegion> wrapper, List<JkRegion> source) {
        if (wrapper == null) {
            return source;
        }
        String sql = String.valueOf(wrapper.getSqlSegment());
        List<JkRegion> result = new ArrayList<JkRegion>();
        for (JkRegion region : source) {
            if (matches(region, sql)) {
                result.add(region);
            }
        }
        return result;
    }

    private JkRegion findOne(com.baomidou.mybatisplus.core.conditions.Wrapper<JkRegion> wrapper, List<JkRegion> source) {
        List<JkRegion> rows = findList(wrapper, source);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean matches(JkRegion region, String sql) {
        if (sql == null) {
            return true;
        }
        if (sql.contains("is_deleted")) {
            if (sql.contains("is_deleted = ?") && Boolean.TRUE.equals(region.getIsDeleted())) {
                return false;
            }
        }
        if (sql.contains("parent_region_code IS NULL")) {
            if (region.getParentRegionCode() != null && region.getParentRegionCode().trim().length() > 0) {
                return false;
            }
        }
        if (sql.contains("status = ?") && !Boolean.TRUE.equals(region.getStatus())) {
            return false;
        }
        if (sql.contains("region_level = ?") && !sql.contains(String.valueOf(region.getRegionLevel()))) {
            return false;
        }
        List<Object> params = wrapperParams(sql);
        if (sql.contains("parent_region_code = ?")) {
            String parentCode = String.valueOf(params.get(0));
            if (!parentCode.equals(region.getParentRegionCode())) {
                return false;
            }
        }
        return true;
    }

    private List<Object> wrapperParams(String sql) {
        List<Object> params = new ArrayList<Object>();
        int idx = sql.indexOf('{');
        while (idx >= 0) {
            int end = sql.indexOf('}', idx);
            if (end < 0) {
                break;
            }
            String token = sql.substring(idx + 1, end);
            int eq = token.indexOf('=');
            if (eq > -1) {
                params.add(token.substring(eq + 1));
            }
            idx = sql.indexOf('{', end + 1);
        }
        return params;
    }

    private <T> T countDao(Class<T> type, Long count) {
        return proxy(type, (method, args) -> {
            if ("selectCount".equals(method.getName())) {
                return count;
            }
            if ("selectList".equals(method.getName())) {
                return Collections.emptyList();
            }
            return null;
        });
    }

    private <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) {
                    return type.getSimpleName() + "Proxy";
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
            }
            return invocation.apply(method, args);
        }));
    }

    private interface Invocation {
        Object apply(Method method, Object[] args) throws Throwable;
    }
}
