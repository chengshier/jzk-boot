package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;

import java.util.Collections;
import java.util.List;

public class JkTradeViewSupport {

    public static String resolveTradeIdentity(JkUserContext context) {
        if (context == null || context.getPrimaryRoleCode() == null || JkBizConstants.ROLE_PLATFORM_ADMIN.equals(context.getPrimaryRoleCode())) {
            return JkBizConstants.ROLE_NORMAL_USER;
        }
        return context.getPrimaryRoleCode();
    }

    public static ActionFlags resolveActionFlags(String tradeIdentity, List<String> permissions) {
        List<String> safePermissions = permissions == null ? Collections.<String>emptyList() : permissions;
        ActionFlags flags = new ActionFlags();
        if (JkBizConstants.ROLE_NORMAL_USER.equals(tradeIdentity)) {
            flags.setCanRetailBuy(true);
            return flags;
        }
        if (JkBizConstants.ROLE_MAKER.equals(tradeIdentity) || JkBizConstants.ROLE_PARTNER.equals(tradeIdentity)) {
            flags.setCanApplyTransfer(safePermissions.contains(JkBizConstants.PERMISSION_STOCK_APPLY));
            return flags;
        }
        if (JkBizConstants.ROLE_COUNTY_AGENT.equals(tradeIdentity)) {
            flags.setCanOrderFromPlatform(safePermissions.contains(JkBizConstants.PERMISSION_STOCK_PLATFORM_ORDER));
            flags.setCanTransferToDownline(safePermissions.contains(JkBizConstants.PERMISSION_STOCK_TRANSFER_CONFIRM));
            flags.setCanViewStockDetail(true);
            return flags;
        }
        flags.setCanRetailBuy(true);
        return flags;
    }

    public static class ActionFlags {
        private boolean canRetailBuy;
        private boolean canApplyTransfer;
        private boolean canOrderFromPlatform;
        private boolean canTransferToDownline;
        private boolean canViewStockDetail;
        private String transferDisabledReason;
        private String platformOrderDisabledReason;
        private String downlineTransferDisabledReason;

        public boolean getCanRetailBuy() {
            return canRetailBuy;
        }

        public ActionFlags setCanRetailBuy(boolean canRetailBuy) {
            this.canRetailBuy = canRetailBuy;
            return this;
        }

        public boolean getCanApplyTransfer() {
            return canApplyTransfer;
        }

        public ActionFlags setCanApplyTransfer(boolean canApplyTransfer) {
            this.canApplyTransfer = canApplyTransfer;
            return this;
        }

        public boolean getCanOrderFromPlatform() {
            return canOrderFromPlatform;
        }

        public ActionFlags setCanOrderFromPlatform(boolean canOrderFromPlatform) {
            this.canOrderFromPlatform = canOrderFromPlatform;
            return this;
        }

        public boolean getCanTransferToDownline() {
            return canTransferToDownline;
        }

        public ActionFlags setCanTransferToDownline(boolean canTransferToDownline) {
            this.canTransferToDownline = canTransferToDownline;
            return this;
        }

        public boolean getCanViewStockDetail() {
            return canViewStockDetail;
        }

        public ActionFlags setCanViewStockDetail(boolean canViewStockDetail) {
            this.canViewStockDetail = canViewStockDetail;
            return this;
        }

        public String getTransferDisabledReason() {
            return transferDisabledReason;
        }

        public ActionFlags setTransferDisabledReason(String transferDisabledReason) {
            this.transferDisabledReason = transferDisabledReason;
            return this;
        }

        public String getPlatformOrderDisabledReason() {
            return platformOrderDisabledReason;
        }

        public ActionFlags setPlatformOrderDisabledReason(String platformOrderDisabledReason) {
            this.platformOrderDisabledReason = platformOrderDisabledReason;
            return this;
        }

        public String getDownlineTransferDisabledReason() {
            return downlineTransferDisabledReason;
        }

        public ActionFlags setDownlineTransferDisabledReason(String downlineTransferDisabledReason) {
            this.downlineTransferDisabledReason = downlineTransferDisabledReason;
            return this;
        }
    }
}
