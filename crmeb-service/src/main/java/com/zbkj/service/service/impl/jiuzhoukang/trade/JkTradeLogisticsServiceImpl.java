package com.zbkj.service.service.impl.jiuzhoukang.trade;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.service.dao.jiuzhoukang.JkPlatformOrderDao;
import com.zbkj.service.dao.jiuzhoukang.JkStockTransferDao;
import com.zbkj.service.service.jiuzhoukang.trade.JkTradeLogisticsService;
import com.zbkj.service.service.jiuzhoukang.trade.PlatformOrderService;
import com.zbkj.service.service.jiuzhoukang.trade.StockTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 物流信息与库存出库必须在同一事务内完成，避免“已出库但没有物流单”或“有物流单但出库失败”。
 */
@Service
public class JkTradeLogisticsServiceImpl implements JkTradeLogisticsService {
    @Autowired private JkPlatformOrderDao platformOrderDao;
    @Autowired private JkStockTransferDao stockTransferDao;
    @Autowired private PlatformOrderService platformOrderService;
    @Autowired private StockTransferService stockTransferService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPlatformOrder shipPlatformOrder(Long operatorId, JkBusinessActionRequest request) {
        validate(request, "发货");
        JkPlatformOrder order = platformOrderDao.selectById(request.getBusinessId());
        if (order == null || Boolean.TRUE.equals(order.getIsDeleted())) throw new CrmebException("订货单不存在");
        order.setLogisticsCompany(normalizeCompany(request.getLogisticsCompany()))
                .setLogisticsNo(normalizeNo(request.getLogisticsCompany(), request.getLogisticsNo()))
                .setShippingTime(request.getShippingTime() == null ? new Date() : request.getShippingTime())
                .setUpdateTime(new Date())
                .setUpdateUserId(operatorId);
        platformOrderDao.updateById(order);
        return platformOrderService.ship(operatorId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkStockTransfer dispatchStockTransfer(Long operatorId, JkBusinessActionRequest request) {
        validate(request, "拨货");
        JkStockTransfer transfer = stockTransferDao.selectById(request.getBusinessId());
        if (transfer == null || Boolean.TRUE.equals(transfer.getIsDeleted())) throw new CrmebException("调拨单不存在");
        transfer.setLogisticsCompany(normalizeCompany(request.getLogisticsCompany()))
                .setLogisticsNo(normalizeNo(request.getLogisticsCompany(), request.getLogisticsNo()))
                .setShippingTime(request.getShippingTime() == null ? new Date() : request.getShippingTime())
                .setUpdateTime(new Date())
                .setUpdateUserId(operatorId);
        stockTransferDao.updateById(transfer);
        return stockTransferService.dispatch(operatorId, request);
    }

    private void validate(JkBusinessActionRequest request, String action) {
        if (request == null || request.getBusinessId() == null) throw new CrmebException(action + "单据不能为空");
        if (StrUtil.isBlank(request.getLogisticsCompany())) throw new CrmebException("请填写物流公司或选择自提");
        if (!isSelfPickup(request.getLogisticsCompany()) && StrUtil.isBlank(request.getLogisticsNo())) {
            throw new CrmebException("请填写物流单号");
        }
    }

    private String normalizeCompany(String company) {
        return isSelfPickup(company) ? "SELF_PICKUP" : company.trim();
    }

    private String normalizeNo(String company, String logisticsNo) {
        return isSelfPickup(company) ? "SELF_PICKUP" : logisticsNo.trim();
    }

    private boolean isSelfPickup(String company) {
        if (StrUtil.isBlank(company)) return false;
        String value = company.trim();
        return "SELF_PICKUP".equalsIgnoreCase(value) || "自提".equals(value) || "无需物流".equals(value);
    }
}
