package com.zbkj.service.service.impl.jiuzhoukang.region;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import com.zbkj.common.model.jiuzhoukang.JkCommissionRule;
import com.zbkj.common.model.jiuzhoukang.JkFundAccount;
import com.zbkj.common.model.jiuzhoukang.JkIdentityApply;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkProductPriceRule;
import com.zbkj.common.model.jiuzhoukang.JkRegion;
import com.zbkj.common.model.jiuzhoukang.JkRegionAgent;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkRetailRefundAdjustment;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturn;
import com.zbkj.common.model.jiuzhoukang.JkUserBusinessRole;
import com.zbkj.common.model.jiuzhoukang.JkUserDataScope;
import com.zbkj.common.request.jiuzhoukang.JkRegionSaveRequest;
import com.zbkj.common.response.jiuzhoukang.JkRegionOptionResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionPathNodeResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionPathResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionSearchResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionTreeNodeResponse;
import com.zbkj.common.response.jiuzhoukang.JkRegionUsageResponse;
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
import com.zbkj.service.service.jiuzhoukang.region.JkRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class JkRegionServiceImpl implements JkRegionService {
    private static final int MAX_REGION_LEVEL = 3;
    private static final int DEFAULT_SEARCH_LIMIT = 50;
    private static final int MAX_SEARCH_LIMIT = 100;

    @Autowired private JkRegionDao regionDao;
    @Autowired private JkRegionAgentDao regionAgentDao;
    @Autowired private JkUserBusinessRoleDao userBusinessRoleDao;
    @Autowired private JkUserDataScopeDao userDataScopeDao;
    @Autowired private JkProductPriceRuleDao priceRuleDao;
    @Autowired private JkStockAccountDao stockAccountDao;
    @Autowired private JkRetailOrderAttributionDao retailOrderAttributionDao;
    @Autowired private JkIdentityApplyDao identityApplyDao;
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkStockTransferDao stockTransferDao;
    @Autowired private JkStockTransferReturnDao stockTransferReturnDao;
    @Autowired private JkCommissionAccountDao commissionAccountDao;
    @Autowired private JkFundAccountDao fundAccountDao;
    @Autowired private JkRetailRefundAdjustmentDao retailRefundAdjustmentDao;
    @Autowired private JkCommissionRuleDao commissionRuleDao;

    @Override
    public List<JkRegion> list(String keywords, Boolean status) {
        LambdaQueryWrapper<JkRegion> query = baseRegionQuery();
        if (StrUtil.isNotBlank(keywords)) {
            String key = keywords.trim();
            query.and(wrapper -> wrapper.like(JkRegion::getRegionCode, key).or().like(JkRegion::getRegionName, key));
        }
        if (status != null) {
            query.eq(JkRegion::getStatus, status);
        }
        return regionDao.selectList(query);
    }

    @Override
    public List<JkRegionTreeNodeResponse> listChildren(String parentRegionCode, Boolean enabled) {
        List<JkRegion> children = listDirectChildren(parentRegionCode, enabled);
        if (children.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> childCounts = countDirectChildren(toCodeSet(children), enabled);
        Set<String> occupiedCodes = resolveOccupiedCodes(toCodeSet(children));
        List<JkRegionTreeNodeResponse> responses = new ArrayList<JkRegionTreeNodeResponse>();
        for (JkRegion child : children) {
            Integer childCount = childCounts.containsKey(child.getRegionCode()) ? childCounts.get(child.getRegionCode()) : Integer.valueOf(0);
            responses.add(new JkRegionTreeNodeResponse()
                    .setId(child.getId())
                    .setRegionCode(child.getRegionCode())
                    .setRegionName(child.getRegionName())
                    .setParentRegionCode(normalizeCode(child.getParentRegionCode()))
                    .setRegionLevel(child.getRegionLevel())
                    .setOccupied(occupiedCodes.contains(child.getRegionCode()) || Boolean.TRUE.equals(child.getOccupied()))
                    .setStatus(child.getStatus())
                    .setHasChildren(childCount.intValue() > 0)
                    .setChildCount(childCount));
        }
        return responses;
    }

    @Override
    public List<JkRegionSearchResponse> searchRegions(String keyword, Integer regionLevel, Boolean status, Integer limit) {
        String key = normalizeText(keyword);
        if (StrUtil.isBlank(key)) {
            return Collections.emptyList();
        }
        int safeLimit = limit == null ? DEFAULT_SEARCH_LIMIT : Math.max(1, Math.min(limit.intValue(), MAX_SEARCH_LIMIT));
        LambdaQueryWrapper<JkRegion> query = baseRegionQuery();
        if (regionLevel != null) {
            query.eq(JkRegion::getRegionLevel, regionLevel);
        }
        if (status != null) {
            query.eq(JkRegion::getStatus, status);
        }
        query.and(wrapper -> wrapper.like(JkRegion::getRegionName, key)
                .or().eq(JkRegion::getRegionCode, key)
                .or().likeRight(JkRegion::getRegionCode, key));
        query.last("limit " + safeLimit);
        List<JkRegion> rows = regionDao.selectList(query);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, JkRegion> hierarchy = loadHierarchyMap(toCodeSet(rows), true);
        Set<String> occupiedCodes = resolveOccupiedCodes(toCodeSet(rows));
        List<JkRegionSearchResponse> responses = new ArrayList<JkRegionSearchResponse>();
        for (JkRegion row : rows) {
            PathData path = buildPathData(row, hierarchy);
            responses.add(new JkRegionSearchResponse()
                    .setRegionCode(row.getRegionCode())
                    .setRegionName(row.getRegionName())
                    .setRegionLevel(row.getRegionLevel())
                    .setParentRegionCode(normalizeCode(row.getParentRegionCode()))
                    .setFullPathName(path.fullPathName)
                    .setFullPathCodes(path.fullPathCodes)
                    .setStatus(row.getStatus())
                    .setOccupied(occupiedCodes.contains(row.getRegionCode()) || Boolean.TRUE.equals(row.getOccupied())));
        }
        return responses;
    }

    @Override
    public JkRegionPathResponse getRegionPath(String regionCode) {
        String code = normalizeCode(regionCode);
        if (StrUtil.isBlank(code)) {
            throw new IllegalArgumentException("区域编码不能为空");
        }
        JkRegion current = getRequiredRegionByCode(code);
        Map<String, JkRegion> hierarchy = loadHierarchyMap(Collections.singleton(code), true);
        PathData path = buildPathData(current, hierarchy);
        return new JkRegionPathResponse()
                .setCurrent(toPathNode(current))
                .setNodes(path.nodes)
                .setFullPathName(path.fullPathName)
                .setFullPathCodes(path.fullPathCodes);
    }

    @Override
    public List<JkRegionOptionResponse> listRegionOptions(String parentRegionCode, Integer targetLevel, Boolean enabled, String keyword) {
        if (StrUtil.isNotBlank(keyword)) {
            List<JkRegionSearchResponse> rows = searchRegions(keyword, targetLevel, enabled, DEFAULT_SEARCH_LIMIT);
            List<JkRegionOptionResponse> options = new ArrayList<JkRegionOptionResponse>();
            for (JkRegionSearchResponse row : rows) {
                options.add(new JkRegionOptionResponse()
                        .setLabel(row.getFullPathName())
                        .setValue(row.getRegionCode())
                        .setRegionLevel(row.getRegionLevel())
                        .setLeaf(row.getRegionLevel() != null && row.getRegionLevel().intValue() >= MAX_REGION_LEVEL)
                        .setDisabled(!Boolean.TRUE.equals(row.getStatus())));
            }
            return options;
        }
        List<JkRegion> rows = listDirectChildren(parentRegionCode, enabled);
        if (targetLevel != null) {
            List<JkRegion> filtered = new ArrayList<JkRegion>();
            for (JkRegion row : rows) {
                if (Objects.equals(targetLevel, row.getRegionLevel())) {
                    filtered.add(row);
                }
            }
            rows = filtered;
        }
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Integer> childCounts = countDirectChildren(toCodeSet(rows), enabled);
        List<JkRegionOptionResponse> options = new ArrayList<JkRegionOptionResponse>();
        for (JkRegion row : rows) {
            Integer childCount = childCounts.containsKey(row.getRegionCode()) ? childCounts.get(row.getRegionCode()) : Integer.valueOf(0);
            options.add(new JkRegionOptionResponse()
                    .setLabel(row.getRegionName())
                    .setValue(row.getRegionCode())
                    .setRegionLevel(row.getRegionLevel())
                    .setLeaf(row.getRegionLevel() != null && row.getRegionLevel().intValue() >= MAX_REGION_LEVEL || childCount.intValue() == 0)
                    .setDisabled(!Boolean.TRUE.equals(row.getStatus())));
        }
        return options;
    }

    @Override
    public JkRegionUsageResponse getRegionUsage(String regionCode) {
        String code = normalizeCode(regionCode);
        if (StrUtil.isBlank(code)) {
            throw new IllegalArgumentException("区域编码不能为空");
        }
        JkRegion region = getRequiredRegionByCode(code);
        Set<String> subtreeCodes = loadSubtreeCodes(code);
        long activeChildCount = countActiveChildren(code);
        long regionAgentCount = countRegionAgents(subtreeCodes);
        long userRoleCount = countUserRoles(subtreeCodes);
        long dataScopeCount = countDataScopes(subtreeCodes);
        long priceRuleCount = countPriceRules(subtreeCodes);
        long stockAccountCount = countStockAccounts(subtreeCodes);
        long attributionCount = countAttributions(subtreeCodes);
        long identityApplyCount = countIdentityApplies(subtreeCodes);
        long platformOrderCount = countPlatformOrders(subtreeCodes);
        long stockTransferCount = countStockTransfers(subtreeCodes);
        long stockTransferReturnCount = countStockTransferReturns(subtreeCodes);
        long commissionAccountCount = countCommissionAccounts(subtreeCodes);
        long fundAccountCount = countFundAccounts(subtreeCodes);
        long refundAdjustmentCount = countRefundAdjustments(subtreeCodes);
        long commissionRuleCount = countCommissionRules(subtreeCodes);
        long businessRecordCount = regionAgentCount + userRoleCount + dataScopeCount + priceRuleCount + stockAccountCount
                + attributionCount + identityApplyCount + platformOrderCount + stockTransferCount + stockTransferReturnCount
                + commissionAccountCount + fundAccountCount + refundAdjustmentCount + commissionRuleCount;
        List<String> reasons = new ArrayList<String>();
        if (activeChildCount > 0L) {
            reasons.add("存在启用子区域，不能停用父区域");
        }
        if (regionAgentCount > 0L) {
            reasons.add("已绑定区域代理，不能删除或错误变更");
        }
        if (userRoleCount > 0L) {
            reasons.add("已关联用户业务身份，不能删除或错误变更");
        }
        if (dataScopeCount > 0L) {
            reasons.add("已关联用户数据范围，不能删除或错误变更");
        }
        if (priceRuleCount > 0L) {
            reasons.add("已关联价格规则，不能删除或错误变更");
        }
        if (stockAccountCount > 0L) {
            reasons.add("已生成库存账户，不能删除或错误变更");
        }
        if (attributionCount > 0L) {
            reasons.add("已生成零售订单归属快照，不能删除或错误变更");
        }
        if (businessRecordCount > 0L) {
            reasons.add("区域已参与业务历史，原则上不允许物理删除");
        }
        return new JkRegionUsageResponse()
                .setCanDisable(activeChildCount == 0L && businessRecordCount == 0L)
                .setCanDelete(activeChildCount == 0L && businessRecordCount == 0L)
                .setActiveChildCount(activeChildCount)
                .setRegionAgentCount(regionAgentCount)
                .setUserRoleCount(userRoleCount)
                .setDataScopeCount(dataScopeCount)
                .setPriceRuleCount(priceRuleCount)
                .setStockAccountCount(stockAccountCount)
                .setAttributionCount(attributionCount)
                .setBusinessRecordCount(businessRecordCount)
                .setReasons(reasons);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkRegion save(JkRegionSaveRequest request, Long operatorId) {
        String regionCode = normalizeRequired(request.getRegionCode(), "区域编码不能为空");
        String regionName = normalizeRequired(request.getRegionName(), "区域名称不能为空");
        String parentRegionCode = normalizeCode(request.getParentRegionCode());
        String remark = normalizeText(request.getRemark());
        boolean status = request.getStatus() == null || request.getStatus().booleanValue();

        JkRegion entity = request.getId() == null ? new JkRegion() : regionDao.selectById(request.getId());
        if (request.getId() != null && (entity == null || Boolean.TRUE.equals(entity.getIsDeleted()))) {
            throw new IllegalArgumentException("区域不存在");
        }
        JkRegion duplicate = regionDao.selectOne(new LambdaQueryWrapper<JkRegion>()
                .eq(JkRegion::getRegionCode, regionCode)
                .eq(JkRegion::getIsDeleted, false)
                .ne(request.getId() != null, JkRegion::getId, request.getId())
                .last("limit 1"));
        if (duplicate != null) {
            throw new IllegalArgumentException("区域编码已存在");
        }

        JkRegion parent = null;
        if (StrUtil.isNotBlank(parentRegionCode)) {
            parent = getRegionByCode(parentRegionCode);
            if (parent == null) {
                throw new IllegalArgumentException("上级区域不存在");
            }
            if (!Boolean.TRUE.equals(parent.getStatus())) {
                throw new IllegalArgumentException("上级区域未启用，不能挂载到该节点");
            }
            if (parent.getRegionLevel() == null || parent.getRegionLevel().intValue() < 1 || parent.getRegionLevel().intValue() >= MAX_REGION_LEVEL) {
                throw new IllegalArgumentException("上级区域层级不正确");
            }
        }

        int regionLevel = parent == null ? 1 : parent.getRegionLevel().intValue() + 1;
        if (regionLevel > MAX_REGION_LEVEL) {
            throw new IllegalArgumentException("区域最大只允许三级");
        }

        if (entity.getId() != null) {
            String currentCode = normalizeCode(entity.getRegionCode());
            String currentParentCode = normalizeCode(entity.getParentRegionCode());
            if (Objects.equals(currentCode, parentRegionCode)) {
                throw new IllegalArgumentException("当前区域不能作为自己的父级");
            }
            if (StrUtil.isNotBlank(parentRegionCode)) {
                assertNoMoveToDescendant(currentCode, parentRegionCode);
            }
            if (!Objects.equals(currentCode, regionCode)) {
                if (countAllChildren(currentCode) > 0L) {
                    throw new IllegalArgumentException("存在下级区域，不能修改区域编码");
                }
                if (getRegionUsage(currentCode).getBusinessRecordCount().longValue() > 0L) {
                    throw new IllegalArgumentException("区域已产生业务数据，禁止修改区域编码");
                }
            }
            if (!Objects.equals(currentParentCode, parentRegionCode)) {
                if (getRegionUsage(currentCode).getBusinessRecordCount().longValue() > 0L) {
                    throw new IllegalArgumentException("区域已产生业务数据，禁止修改上级区域");
                }
            }
        }

        JkRegionUsageResponse existingUsage = entity.getId() == null ? null : getRegionUsage(entity.getRegionCode());
        if (entity.getId() != null && !status) {
            assertCanDisable(entity.getRegionCode());
        }

        Date now = new Date();
        entity.setRegionCode(regionCode)
                .setRegionName(regionName)
                .setParentRegionCode(parentRegionCode)
                .setRegionLevel(Integer.valueOf(regionLevel))
                .setRemark(remark)
                .setStatus(Boolean.valueOf(status))
                .setIsDeleted(false)
                .setOccupied(Boolean.valueOf(existingUsage != null && existingUsage.getBusinessRecordCount().longValue() > 0L))
                .setUpdateUserId(operatorId)
                .setUpdateTime(now);
        if (entity.getId() == null) {
            entity.setOccupied(false);
            entity.setCreateUserId(operatorId);
            entity.setCreateTime(now);
            entity.setTenantId(StrUtil.blankToDefault(entity.getTenantId(), "000000"));
            regionDao.insert(entity);
        } else {
            regionDao.updateById(entity);
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, boolean status, Long operatorId) {
        JkRegion entity = regionDao.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            return false;
        }
        if (status) {
            if (StrUtil.isNotBlank(entity.getParentRegionCode())) {
                JkRegion parent = getRegionByCode(entity.getParentRegionCode());
                if (parent == null) {
                    throw new IllegalArgumentException("上级区域不存在");
                }
                if (!Boolean.TRUE.equals(parent.getStatus())) {
                    throw new IllegalArgumentException("上级区域未启用，不能启用当前区域");
                }
            }
        } else {
            assertCanDisable(entity.getRegionCode());
        }
        entity.setStatus(Boolean.valueOf(status))
                .setOccupied(Boolean.valueOf(getRegionUsage(entity.getRegionCode()).getBusinessRecordCount().longValue() > 0L))
                .setUpdateUserId(operatorId)
                .setUpdateTime(new Date());
        return regionDao.updateById(entity) == 1;
    }

    private LambdaQueryWrapper<JkRegion> baseRegionQuery() {
        return new LambdaQueryWrapper<JkRegion>()
                .eq(JkRegion::getIsDeleted, false)
                .orderByAsc(JkRegion::getRegionLevel)
                .orderByAsc(JkRegion::getRegionCode);
    }

    private List<JkRegion> listDirectChildren(String parentRegionCode, Boolean enabled) {
        LambdaQueryWrapper<JkRegion> query = baseRegionQuery();
        String code = normalizeCode(parentRegionCode);
        if (StrUtil.isBlank(code)) {
            query.and(wrapper -> wrapper.isNull(JkRegion::getParentRegionCode).or().eq(JkRegion::getParentRegionCode, ""));
        } else {
            query.eq(JkRegion::getParentRegionCode, code);
        }
        if (enabled != null) {
            query.eq(JkRegion::getStatus, enabled);
        }
        return regionDao.selectList(query);
    }

    private Map<String, Integer> countDirectChildren(Set<String> parentCodes, Boolean enabled) {
        if (parentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<JkRegion> query = new LambdaQueryWrapper<JkRegion>()
                .eq(JkRegion::getIsDeleted, false)
                .in(JkRegion::getParentRegionCode, parentCodes)
                .orderByAsc(JkRegion::getRegionCode);
        if (enabled != null) {
            query.eq(JkRegion::getStatus, enabled);
        }
        List<JkRegion> rows = regionDao.selectList(query);
        Map<String, Integer> result = new HashMap<String, Integer>();
        for (JkRegion row : rows) {
            String parentCode = normalizeCode(row.getParentRegionCode());
            if (StrUtil.isBlank(parentCode)) {
                continue;
            }
            Integer count = result.get(parentCode);
            result.put(parentCode, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        return result;
    }

    private Set<String> resolveOccupiedCodes(Set<String> regionCodes) {
        if (regionCodes.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> occupied = new HashSet<String>();
        occupied.addAll(extractRegionCodes(regionAgentDao.selectList(new LambdaQueryWrapper<JkRegionAgent>()
                .eq(JkRegionAgent::getIsDeleted, false).in(JkRegionAgent::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(userBusinessRoleDao.selectList(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getIsDeleted, false).in(JkUserBusinessRole::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(userDataScopeDao.selectList(new LambdaQueryWrapper<JkUserDataScope>()
                .eq(JkUserDataScope::getIsDeleted, false).in(JkUserDataScope::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(priceRuleDao.selectList(new LambdaQueryWrapper<JkProductPriceRule>()
                .eq(JkProductPriceRule::getIsDeleted, false).in(JkProductPriceRule::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(stockAccountDao.selectList(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getIsDeleted, false).in(JkStockAccount::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(retailOrderAttributionDao.selectList(new LambdaQueryWrapper<JkRetailOrderAttribution>()
                .eq(JkRetailOrderAttribution::getIsDeleted, false).in(JkRetailOrderAttribution::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(identityApplyDao.selectList(new LambdaQueryWrapper<JkIdentityApply>()
                .eq(JkIdentityApply::getIsDeleted, false).in(JkIdentityApply::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(platformOrderDao.selectList(new LambdaQueryWrapper<JkPlatformOrder>()
                .eq(JkPlatformOrder::getIsDeleted, false).in(JkPlatformOrder::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(stockTransferDao.selectList(new LambdaQueryWrapper<JkStockTransfer>()
                .eq(JkStockTransfer::getIsDeleted, false).in(JkStockTransfer::getRegionCode, regionCodes))));
        occupied.addAll(extractRegionCodes(stockTransferReturnDao.selectList(new LambdaQueryWrapper<JkStockTransferReturn>()
                .eq(JkStockTransferReturn::getIsDeleted, false).in(JkStockTransferReturn::getRegionCode, regionCodes))));
        return occupied;
    }

    private Set<String> extractRegionCodes(Collection<?> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> codes = new HashSet<String>();
        for (Object row : rows) {
            String code = null;
            if (row instanceof JkRegionAgent) {
                code = ((JkRegionAgent) row).getRegionCode();
            } else if (row instanceof JkUserBusinessRole) {
                code = ((JkUserBusinessRole) row).getRegionCode();
            } else if (row instanceof JkUserDataScope) {
                code = ((JkUserDataScope) row).getRegionCode();
            } else if (row instanceof JkProductPriceRule) {
                code = ((JkProductPriceRule) row).getRegionCode();
            } else if (row instanceof JkStockAccount) {
                code = ((JkStockAccount) row).getRegionCode();
            } else if (row instanceof JkRetailOrderAttribution) {
                code = ((JkRetailOrderAttribution) row).getRegionCode();
            } else if (row instanceof JkIdentityApply) {
                code = ((JkIdentityApply) row).getRegionCode();
            } else if (row instanceof JkPlatformOrder) {
                code = ((JkPlatformOrder) row).getRegionCode();
            } else if (row instanceof JkStockTransfer) {
                code = ((JkStockTransfer) row).getRegionCode();
            } else if (row instanceof JkStockTransferReturn) {
                code = ((JkStockTransferReturn) row).getRegionCode();
            } else if (row instanceof JkCommissionAccount) {
                code = ((JkCommissionAccount) row).getRegionCode();
            } else if (row instanceof JkFundAccount) {
                code = ((JkFundAccount) row).getRegionCode();
            } else if (row instanceof JkRetailRefundAdjustment) {
                code = ((JkRetailRefundAdjustment) row).getRegionCode();
            } else if (row instanceof JkCommissionRule) {
                code = ((JkCommissionRule) row).getRegionCode();
            }
            code = normalizeCode(code);
            if (StrUtil.isNotBlank(code)) {
                codes.add(code);
            }
        }
        return codes;
    }

    private Map<String, JkRegion> loadHierarchyMap(Set<String> codes, boolean includeParents) {
        if (codes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, JkRegion> result = new LinkedHashMap<String, JkRegion>();
        Set<String> pending = new LinkedHashSet<String>(codes);
        for (int i = 0; i < MAX_REGION_LEVEL && !pending.isEmpty(); i++) {
            List<JkRegion> rows = regionDao.selectList(new LambdaQueryWrapper<JkRegion>()
                    .eq(JkRegion::getIsDeleted, false)
                    .in(JkRegion::getRegionCode, pending));
            pending = new LinkedHashSet<String>();
            for (JkRegion row : rows) {
                result.put(row.getRegionCode(), row);
            }
            if (!includeParents) {
                break;
            }
            for (JkRegion row : rows) {
                String parentCode = normalizeCode(row.getParentRegionCode());
                if (StrUtil.isNotBlank(parentCode) && !result.containsKey(parentCode)) {
                    pending.add(parentCode);
                }
            }
        }
        return result;
    }

    private PathData buildPathData(JkRegion current, Map<String, JkRegion> hierarchy) {
        List<JkRegionPathNodeResponse> reversedNodes = new ArrayList<JkRegionPathNodeResponse>();
        List<String> reversedCodes = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        JkRegion cursor = current;
        for (int depth = 0; depth < MAX_REGION_LEVEL; depth++) {
            if (cursor == null) {
                break;
            }
            String code = normalizeCode(cursor.getRegionCode());
            if (!seen.add(code)) {
                throw new IllegalArgumentException("区域父子关系存在循环");
            }
            reversedNodes.add(toPathNode(cursor));
            reversedCodes.add(code);
            String parentCode = normalizeCode(cursor.getParentRegionCode());
            if (StrUtil.isBlank(parentCode)) {
                break;
            }
            JkRegion parent = hierarchy.get(parentCode);
            if (parent == null) {
                throw new IllegalArgumentException("区域上级不存在");
            }
            if (cursor.getRegionLevel() == null || parent.getRegionLevel() == null
                    || parent.getRegionLevel().intValue() != cursor.getRegionLevel().intValue() - 1) {
                throw new IllegalArgumentException("区域层级异常");
            }
            cursor = parent;
            if (depth == MAX_REGION_LEVEL - 1 && StrUtil.isNotBlank(parent.getParentRegionCode())) {
                throw new IllegalArgumentException("区域层级异常，最多仅支持三级");
            }
        }
        Collections.reverse(reversedNodes);
        Collections.reverse(reversedCodes);
        if (reversedNodes.isEmpty()) {
            throw new IllegalArgumentException("区域路径不存在");
        }
        if (reversedNodes.get(0).getRegionLevel() == null || reversedNodes.get(0).getRegionLevel().intValue() != 1) {
            throw new IllegalArgumentException("区域层级异常");
        }
        List<String> names = new ArrayList<String>();
        for (JkRegionPathNodeResponse node : reversedNodes) {
            names.add(node.getRegionName());
        }
        return new PathData(reversedNodes, reversedCodes, String.join(" / ", names));
    }

    private JkRegionPathNodeResponse toPathNode(JkRegion region) {
        return new JkRegionPathNodeResponse()
                .setId(region.getId())
                .setRegionCode(region.getRegionCode())
                .setRegionName(region.getRegionName())
                .setParentRegionCode(normalizeCode(region.getParentRegionCode()))
                .setRegionLevel(region.getRegionLevel())
                .setStatus(region.getStatus());
    }

    private JkRegion getRequiredRegionByCode(String regionCode) {
        JkRegion region = getRegionByCode(regionCode);
        if (region == null) {
            throw new IllegalArgumentException("区域不存在");
        }
        return region;
    }

    private JkRegion getRegionByCode(String regionCode) {
        String code = normalizeCode(regionCode);
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return regionDao.selectOne(new LambdaQueryWrapper<JkRegion>()
                .eq(JkRegion::getRegionCode, code)
                .eq(JkRegion::getIsDeleted, false)
                .last("limit 1"));
    }

    private void assertNoMoveToDescendant(String regionCode, String targetParentCode) {
        String currentCode = normalizeCode(regionCode);
        String parentCode = normalizeCode(targetParentCode);
        if (StrUtil.isBlank(currentCode) || StrUtil.isBlank(parentCode)) {
            return;
        }
        if (currentCode.equals(parentCode)) {
            throw new IllegalArgumentException("当前区域不能作为自己的父级");
        }
        Set<String> seen = new HashSet<String>();
        String cursorCode = parentCode;
        for (int depth = 0; depth < MAX_REGION_LEVEL; depth++) {
            if (!seen.add(cursorCode)) {
                throw new IllegalArgumentException("区域父子关系存在循环");
            }
            if (currentCode.equals(cursorCode)) {
                throw new IllegalArgumentException("不能移动到自己的子孙节点下面");
            }
            JkRegion cursor = getRegionByCode(cursorCode);
            if (cursor == null) {
                throw new IllegalArgumentException("上级区域不存在");
            }
            cursorCode = normalizeCode(cursor.getParentRegionCode());
            if (StrUtil.isBlank(cursorCode)) {
                return;
            }
        }
        if (StrUtil.isNotBlank(cursorCode)) {
            throw new IllegalArgumentException("区域层级异常，最多仅支持三级");
        }
    }

    private void assertCanDisable(String regionCode) {
        JkRegionUsageResponse usage = getRegionUsage(regionCode);
        if (!Boolean.TRUE.equals(usage.getCanDisable())) {
            if (usage.getReasons() != null && !usage.getReasons().isEmpty()) {
                throw new IllegalArgumentException(String.join("；", usage.getReasons()));
            }
            throw new IllegalArgumentException("当前区域不允许停用");
        }
    }

    private Set<String> loadSubtreeCodes(String regionCode) {
        Set<String> subtree = new LinkedHashSet<String>();
        String rootCode = normalizeCode(regionCode);
        subtree.add(rootCode);
        Set<String> currentParents = Collections.singleton(rootCode);
        for (int depth = 0; depth < MAX_REGION_LEVEL - 1 && !currentParents.isEmpty(); depth++) {
            List<JkRegion> rows = regionDao.selectList(new LambdaQueryWrapper<JkRegion>()
                    .eq(JkRegion::getIsDeleted, false)
                    .in(JkRegion::getParentRegionCode, currentParents));
            Set<String> nextParents = new LinkedHashSet<String>();
            for (JkRegion row : rows) {
                if (subtree.add(row.getRegionCode())) {
                    nextParents.add(row.getRegionCode());
                }
            }
            currentParents = nextParents;
        }
        return subtree;
    }

    private long countActiveChildren(String regionCode) {
        return regionDao.selectCount(new LambdaQueryWrapper<JkRegion>()
                .eq(JkRegion::getIsDeleted, false)
                .eq(JkRegion::getStatus, true)
                .eq(JkRegion::getParentRegionCode, normalizeCode(regionCode)));
    }

    private long countAllChildren(String regionCode) {
        return regionDao.selectCount(new LambdaQueryWrapper<JkRegion>()
                .eq(JkRegion::getIsDeleted, false)
                .eq(JkRegion::getParentRegionCode, normalizeCode(regionCode)));
    }

    private long countRegionAgents(Set<String> regionCodes) {
        return regionAgentDao.selectCount(new LambdaQueryWrapper<JkRegionAgent>()
                .eq(JkRegionAgent::getIsDeleted, false)
                .in(JkRegionAgent::getRegionCode, regionCodes));
    }

    private long countUserRoles(Set<String> regionCodes) {
        return userBusinessRoleDao.selectCount(new LambdaQueryWrapper<JkUserBusinessRole>()
                .eq(JkUserBusinessRole::getIsDeleted, false)
                .in(JkUserBusinessRole::getRegionCode, regionCodes));
    }

    private long countDataScopes(Set<String> regionCodes) {
        return userDataScopeDao.selectCount(new LambdaQueryWrapper<JkUserDataScope>()
                .eq(JkUserDataScope::getIsDeleted, false)
                .in(JkUserDataScope::getRegionCode, regionCodes));
    }

    private long countPriceRules(Set<String> regionCodes) {
        return priceRuleDao.selectCount(new LambdaQueryWrapper<JkProductPriceRule>()
                .eq(JkProductPriceRule::getIsDeleted, false)
                .in(JkProductPriceRule::getRegionCode, regionCodes));
    }

    private long countStockAccounts(Set<String> regionCodes) {
        return stockAccountDao.selectCount(new LambdaQueryWrapper<JkStockAccount>()
                .eq(JkStockAccount::getIsDeleted, false)
                .in(JkStockAccount::getRegionCode, regionCodes));
    }

    private long countAttributions(Set<String> regionCodes) {
        return retailOrderAttributionDao.selectCount(new LambdaQueryWrapper<JkRetailOrderAttribution>()
                .eq(JkRetailOrderAttribution::getIsDeleted, false)
                .in(JkRetailOrderAttribution::getRegionCode, regionCodes));
    }

    private long countIdentityApplies(Set<String> regionCodes) {
        return identityApplyDao.selectCount(new LambdaQueryWrapper<JkIdentityApply>()
                .eq(JkIdentityApply::getIsDeleted, false)
                .in(JkIdentityApply::getRegionCode, regionCodes));
    }

    private long countPlatformOrders(Set<String> regionCodes) {
        return platformOrderDao.selectCount(new LambdaQueryWrapper<JkPlatformOrder>()
                .eq(JkPlatformOrder::getIsDeleted, false)
                .in(JkPlatformOrder::getRegionCode, regionCodes));
    }

    private long countStockTransfers(Set<String> regionCodes) {
        return stockTransferDao.selectCount(new LambdaQueryWrapper<JkStockTransfer>()
                .eq(JkStockTransfer::getIsDeleted, false)
                .in(JkStockTransfer::getRegionCode, regionCodes));
    }

    private long countStockTransferReturns(Set<String> regionCodes) {
        return stockTransferReturnDao.selectCount(new LambdaQueryWrapper<JkStockTransferReturn>()
                .eq(JkStockTransferReturn::getIsDeleted, false)
                .in(JkStockTransferReturn::getRegionCode, regionCodes));
    }

    private long countCommissionAccounts(Set<String> regionCodes) {
        return commissionAccountDao.selectCount(new LambdaQueryWrapper<JkCommissionAccount>()
                .eq(JkCommissionAccount::getIsDeleted, false)
                .in(JkCommissionAccount::getRegionCode, regionCodes));
    }

    private long countFundAccounts(Set<String> regionCodes) {
        return fundAccountDao.selectCount(new LambdaQueryWrapper<JkFundAccount>()
                .eq(JkFundAccount::getIsDeleted, false)
                .in(JkFundAccount::getRegionCode, regionCodes));
    }

    private long countRefundAdjustments(Set<String> regionCodes) {
        return retailRefundAdjustmentDao.selectCount(new LambdaQueryWrapper<JkRetailRefundAdjustment>()
                .eq(JkRetailRefundAdjustment::getIsDeleted, false)
                .in(JkRetailRefundAdjustment::getRegionCode, regionCodes));
    }

    private long countCommissionRules(Set<String> regionCodes) {
        return commissionRuleDao.selectCount(new LambdaQueryWrapper<JkCommissionRule>()
                .eq(JkCommissionRule::getIsDeleted, false)
                .in(JkCommissionRule::getRegionCode, regionCodes));
    }

    private Set<String> toCodeSet(List<JkRegion> rows) {
        Set<String> codes = new LinkedHashSet<String>();
        for (JkRegion row : rows) {
            String code = normalizeCode(row.getRegionCode());
            if (StrUtil.isNotBlank(code)) {
                codes.add(code);
            }
        }
        return codes;
    }

    private String normalizeCode(String value) {
        return StrUtil.blankToDefault(normalizeText(value), null);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeText(value);
        if (StrUtil.isBlank(normalized)) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private static class PathData {
        private final List<JkRegionPathNodeResponse> nodes;
        private final List<String> fullPathCodes;
        private final String fullPathName;

        private PathData(List<JkRegionPathNodeResponse> nodes, List<String> fullPathCodes, String fullPathName) {
            this.nodes = nodes;
            this.fullPathCodes = fullPathCodes;
            this.fullPathName = fullPathName;
        }
    }
}
