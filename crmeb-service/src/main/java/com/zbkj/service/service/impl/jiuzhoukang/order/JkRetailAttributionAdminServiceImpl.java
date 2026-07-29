package com.zbkj.service.service.impl.jiuzhoukang.order;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkRegionAgent;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttributionAdjustment;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkRetailAttributionResolveRequest;
import com.zbkj.common.response.jiuzhoukang.JkRegionPathResponse;
import com.zbkj.service.dao.jiuzhoukang.JkRegionAgentDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionAdjustmentDao;
import com.zbkj.service.dao.jiuzhoukang.JkRetailOrderAttributionDao;
import com.zbkj.service.service.jiuzhoukang.order.JkRetailAttributionAdminService;
import com.zbkj.service.service.jiuzhoukang.region.JkRegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JkRetailAttributionAdminServiceImpl implements JkRetailAttributionAdminService {
    @Autowired private JkRetailOrderAttributionDao attributionDao;
    @Autowired private JkRetailOrderAttributionAdjustmentDao adjustmentDao;
    @Autowired private JkRegionAgentDao regionAgentDao;
    @Autowired private JkRegionService regionService;

    @Override
    public PageInfo<JkRetailOrderAttribution> list(String orderNo, Long buyerUserId, String regionSourceType,
                                                   String attributionStatus, PageParamRequest page) {
        PageHelper.startPage(page.getPage(), page.getLimit());
        LambdaQueryWrapper<JkRetailOrderAttribution> query = new LambdaQueryWrapper<JkRetailOrderAttribution>()
                .eq(JkRetailOrderAttribution::getIsDeleted, false)
                .orderByDesc(JkRetailOrderAttribution::getId);
        if (hasText(orderNo)) query.like(JkRetailOrderAttribution::getOrderNo, orderNo.trim());
        if (buyerUserId != null) query.eq(JkRetailOrderAttribution::getBuyerUserId, buyerUserId);
        if (hasText(regionSourceType)) query.eq(JkRetailOrderAttribution::getRegionSourceType, regionSourceType.trim());
        if (hasText(attributionStatus)) query.eq(JkRetailOrderAttribution::getAttributionStatus, attributionStatus.trim());
        return new PageInfo<JkRetailOrderAttribution>(attributionDao.selectList(query));
    }

    @Override
    public Map<String, Object> detail(Long id) {
        JkRetailOrderAttribution row = require(id);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("attribution", row);
        result.put("adjustments", audit(id));
        result.put("locked", "LOCKED".equalsIgnoreCase(row.getLockStatus()));
        result.put("editable", isDirectlyEditable(row));
        result.put("explain", explain(row));
        return result;
    }

    @Override
    public Map<String, Object> overview(Long id) {
        JkRetailOrderAttribution row = require(id);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("orderNo", row.getOrderNo());
        result.put("orderInfoId", row.getOrderInfoId());
        result.put("buyerUserId", row.getBuyerUserId());
        result.put("profileRegionCode", row.getProfileRegionCode());
        result.put("shippingRegionCode", row.getShippingRegionCode());
        result.put("finalRegionCode", row.getFinalRegionCode());
        result.put("regionSourceType", row.getRegionSourceType());
        result.put("directParentUserId", row.getDirectParentUserId());
        result.put("countyAgentUserId", row.getCountyAgentUserId());
        result.put("commissionBaseAmount", row.getCommissionBaseAmount());
        result.put("refundedAmount", row.getRefundedAmount());
        result.put("lockStatus", row.getLockStatus());
        result.put("explain", explain(row));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkRetailOrderAttribution resolve(Long id, Long operatorId, JkRetailAttributionResolveRequest request) {
        JkRetailOrderAttribution row = require(id);
        if (!isDirectlyEditable(row)) {
            throw new CrmebException("只有未锁定的 PENDING_MANUAL 或 CONFLICT 归属可直接处理；已锁定记录必须走调整、冲正和补偿");
        }
        applyRegion(row, request);
        saveAdjustment(row, operatorId, request, "MANUAL_RESOLVE", "APPLIED", true);
        return require(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkRetailOrderAttributionAdjustment adjust(Long id, Long operatorId, JkRetailAttributionResolveRequest request) {
        JkRetailOrderAttribution row = require(id);
        if ("LOCKED".equalsIgnoreCase(row.getLockStatus())) {
            return saveAdjustment(row, operatorId, request, "LOCKED_COMPENSATION", "PENDING_COMPENSATION", false);
        }
        applyRegion(row, request);
        return saveAdjustment(row, operatorId, request, "UNLOCKED_ADJUST", "APPLIED", true);
    }

    @Override
    public List<JkRetailOrderAttributionAdjustment> audit(Long id) {
        return adjustmentDao.selectList(new LambdaQueryWrapper<JkRetailOrderAttributionAdjustment>()
                .eq(JkRetailOrderAttributionAdjustment::getAttributionId, id)
                .orderByDesc(JkRetailOrderAttributionAdjustment::getId));
    }

    private void applyRegion(JkRetailOrderAttribution row, JkRetailAttributionResolveRequest request) {
        String regionCode = request.getFinalRegionCode() == null ? null : request.getFinalRegionCode().trim();
        JkRegionPathResponse path = regionService.getRegionPath(regionCode);
        if (path == null || path.getCurrent() == null) throw new CrmebException("最终区域无效");
        if (request.getCountyAgentUserId() != null) {
            JkRegionAgent agent = regionAgentDao.selectOne(new LambdaQueryWrapper<JkRegionAgent>()
                    .eq(JkRegionAgent::getRegionCode, regionCode)
                    .eq(JkRegionAgent::getCountyAgentUserId, request.getCountyAgentUserId())
                    .eq(JkRegionAgent::getStatus, true).eq(JkRegionAgent::getIsDeleted, false)
                    .orderByDesc(JkRegionAgent::getId).last("limit 1"));
            if (agent == null) throw new CrmebException("所选区县代理不是该区域当前有效代理");
        }
        row.setFinalRegionCode(regionCode).setRegionCode(regionCode)
                .setFinalRegionNameSnapshot(path.getFullPathName()).setRegionSourceType("MANUAL_RESOLVED")
                .setCountyAgentUserId(request.getCountyAgentUserId()).setReceiverUserId(request.getCountyAgentUserId())
                .setReceiverRoleCode(request.getCountyAgentUserId() == null ? null : "county_agent")
                .setAttributionStatus("RESOLVED");
    }

    private JkRetailOrderAttributionAdjustment saveAdjustment(JkRetailOrderAttribution row, Long operatorId,
                                                               JkRetailAttributionResolveRequest request,
                                                               String type, String status, boolean updateAttribution) {
        if (adjustmentDao.selectCount(new LambdaQueryWrapper<JkRetailOrderAttributionAdjustment>()
                .eq(JkRetailOrderAttributionAdjustment::getRequestNo, request.getRequestNo())) > 0) {
            return adjustmentDao.selectOne(new LambdaQueryWrapper<JkRetailOrderAttributionAdjustment>()
                    .eq(JkRetailOrderAttributionAdjustment::getRequestNo, request.getRequestNo()).last("limit 1"));
        }
        String before = JSONUtil.toJsonStr(require(row.getId()));
        String after = JSONUtil.toJsonStr(row);
        Date now = new Date();
        JkRetailOrderAttributionAdjustment adjustment = new JkRetailOrderAttributionAdjustment()
                .setAttributionId(row.getId()).setBeforeSnapshotJson(before).setAfterSnapshotJson(after)
                .setAdjustReason(request.getReason()).setAdjustType(type).setOperatorUserId(operatorId)
                .setStatus(status).setRequestNo(request.getRequestNo()).setCreateTime(now).setUpdateTime(now);
        adjustmentDao.insert(adjustment);
        if (updateAttribution) {
            int version = row.getVersion() == null ? 0 : row.getVersion();
            int updated = attributionDao.update(null, new LambdaUpdateWrapper<JkRetailOrderAttribution>()
                    .eq(JkRetailOrderAttribution::getId, row.getId())
                    .eq(JkRetailOrderAttribution::getVersion, version)
                    .ne(JkRetailOrderAttribution::getLockStatus, "LOCKED")
                    .set(JkRetailOrderAttribution::getFinalRegionCode, row.getFinalRegionCode())
                    .set(JkRetailOrderAttribution::getRegionCode, row.getRegionCode())
                    .set(JkRetailOrderAttribution::getFinalRegionNameSnapshot, row.getFinalRegionNameSnapshot())
                    .set(JkRetailOrderAttribution::getRegionSourceType, row.getRegionSourceType())
                    .set(JkRetailOrderAttribution::getCountyAgentUserId, row.getCountyAgentUserId())
                    .set(JkRetailOrderAttribution::getReceiverUserId, row.getReceiverUserId())
                    .set(JkRetailOrderAttribution::getReceiverRoleCode, row.getReceiverRoleCode())
                    .set(JkRetailOrderAttribution::getAttributionStatus, row.getAttributionStatus())
                    .set(JkRetailOrderAttribution::getVersion, version + 1)
                    .set(JkRetailOrderAttribution::getUpdateTime, now));
            if (updated != 1) throw new CrmebException("归属记录已锁定或版本冲突，未直接修改，请重新加载后走补偿流程");
        }
        return adjustment;
    }

    private JkRetailOrderAttribution require(Long id) {
        JkRetailOrderAttribution row = attributionDao.selectById(id);
        if (row == null || Boolean.TRUE.equals(row.getIsDeleted())) throw new CrmebException("零售归属记录不存在");
        return row;
    }

    private boolean isDirectlyEditable(JkRetailOrderAttribution row) {
        if ("LOCKED".equalsIgnoreCase(row.getLockStatus())) return false;
        return "PENDING_MANUAL".equalsIgnoreCase(row.getAttributionStatus())
                || "CONFLICT".equalsIgnoreCase(row.getAttributionStatus());
    }

    private String explain(JkRetailOrderAttribution row) {
        if ("RELATION".equals(row.getRegionSourceType())) return "采用下单时有效直属关系快照；后续换绑不改变本单";
        if ("USER_PROFILE".equals(row.getRegionSourceType())) return "无有效直属关系，采用下单时个人资料标准区域";
        if ("ORDER_ADDRESS_FALLBACK".equals(row.getRegionSourceType())) return "无有效直属关系和个人资料区域，采用本单标准收货区域兜底";
        if ("MANUAL_RESOLVED".equals(row.getRegionSourceType())) return "归属冲突在未锁定阶段经人工处理；调整历史已保留";
        return "无法解析有效关系和标准区域，订单继续归平台默认且不产生区域代理佣金";
    }

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
