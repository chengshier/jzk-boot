package com.zbkj.service.service.impl.jiuzhoukang.audit;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkAuditLogSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkAuditLogResponse;
import com.zbkj.service.dao.jiuzhoukang.JkAuditLogDao;
import com.zbkj.service.service.jiuzhoukang.audit.JkAuditLogService;
import com.zbkj.service.service.jiuzhoukang.support.JkDictLabelHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JkAuditLogServiceImpl extends ServiceImpl<JkAuditLogDao, JkAuditLog> implements JkAuditLogService {

    @Override
    public void saveAuditLog(JkAuditLog auditLog) {
        save(auditLog);
    }

    @Override
    public List<JkAuditLogResponse> getAdminList(JkAuditLogSearchRequest request, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<JkAuditLog> lqw = new LambdaQueryWrapper<>();
        lqw.eq(JkAuditLog::getIsDeleted, false);
        if (request != null && StrUtil.isNotBlank(request.getBusinessType())) {
            lqw.eq(JkAuditLog::getBusinessType, request.getBusinessType());
        }
        if (request != null && request.getBusinessId() != null) {
            lqw.eq(JkAuditLog::getBusinessId, request.getBusinessId());
        }
        if (request != null && StrUtil.isNotBlank(request.getAuditAction())) {
            lqw.eq(JkAuditLog::getAuditAction, request.getAuditAction());
        }
        lqw.orderByDesc(JkAuditLog::getId);
        return toResponses(list(lqw));
    }

    @Override
    public List<JkAuditLogResponse> toResponses(List<JkAuditLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }
        return logs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private JkAuditLogResponse toResponse(JkAuditLog item) {
        JkAuditLogResponse response = new JkAuditLogResponse();
        BeanUtils.copyProperties(item, response);
        response.setBusinessTypeText(labelBusinessType(item.getBusinessType()));
        response.setAuditActionText(labelAuditAction(item.getAuditAction()));
        response.setBeforeStatusText(labelStatus(item.getBusinessType(), item.getBeforeStatus()));
        response.setAfterStatusText(labelStatus(item.getBusinessType(), item.getAfterStatus()));
        return response;
    }

    private String labelBusinessType(String businessType) {
        if (StrUtil.isBlank(businessType)) {
            return "--";
        }
        if ("IDENTITY_APPLY".equals(businessType)) {
            return "身份申请";
        }
        if ("PLATFORM_ORDER".equals(businessType)) {
            return "平台订货";
        }
        if ("PLATFORM_ORDER_STOCK_IN".equals(businessType)) {
            return "平台订货入库";
        }
        if ("STOCK_TRANSFER".equals(businessType)) {
            return "库存调拨";
        }
        if ("STOCK_TRANSFER_COMPLETED".equals(businessType)) {
            return "库存调拨完成";
        }
        if ("WITHDRAW".equals(businessType)) {
            return "提现申请";
        }
        return businessType;
    }

    private String labelAuditAction(String auditAction) {
        if (StrUtil.isBlank(auditAction)) {
            return "--";
        }
        if ("SUBMIT_VOUCHER".equals(auditAction)) {
            return "提交付款凭证";
        }
        if ("DISPATCH".equals(auditAction)) {
            return "拨货";
        }
        if ("EVENT_RECORDED".equals(auditAction)) {
            return "业绩事件记录";
        }
        if ("CONFIRM_PAID".equals(auditAction)) {
            return "确认打款";
        }
        return JkDictLabelHelper.label("audit_action", auditAction);
    }

    private String labelStatus(String businessType, String status) {
        if (StrUtil.isBlank(status)) {
            return "--";
        }
        return JkDictLabelHelper.label(resolveStatusDictType(businessType), status);
    }

    private String resolveStatusDictType(String businessType) {
        if ("PLATFORM_ORDER".equals(businessType) || "PLATFORM_ORDER_STOCK_IN".equals(businessType)) {
            return "platform_order_status";
        }
        if ("STOCK_TRANSFER".equals(businessType)) {
            return "stock_transfer_status";
        }
        if ("WITHDRAW".equals(businessType)) {
            return "withdraw_status";
        }
        return "audit_status";
    }
}
