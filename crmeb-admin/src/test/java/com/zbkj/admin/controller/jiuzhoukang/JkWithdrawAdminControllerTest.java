package com.zbkj.admin.controller.jiuzhoukang;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class JkWithdrawAdminControllerTest {

    @Test
    public void resolvesReverseDeductFilterToLegacyAndCurrentCodes() {
        List<String> current = JkWithdrawAdminController.resolveFundFlowTypes("REVERSE_DEDUCT");
        List<String> legacy = JkWithdrawAdminController.resolveFundFlowTypes("COMMISSION_REVERSE_OUT");
        List<String> expected = Arrays.asList("REVERSE_DEDUCT", "COMMISSION_REVERSE_OUT");

        Assert.assertEquals(expected, current);
        Assert.assertEquals(expected, legacy);
    }

    @Test
    public void keepsNormalFundFlowTypeAsSingleValue() {
        Assert.assertEquals(Arrays.asList("WITHDRAW_FREEZE"), JkWithdrawAdminController.resolveFundFlowTypes("WITHDRAW_FREEZE"));
    }
}
