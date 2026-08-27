package com.zbkj.service.service.jiuzhoukang.promotion;

import org.junit.Assert;
import org.junit.Test;

public class JkPromotionSceneSupportTest {

    @Test
    public void entryAccessSceneNeverCarriesAnInviter() {
        JkPromotionSceneSupport.SceneEntry entry = JkPromotionSceneSupport.parse("entry:open");

        Assert.assertEquals(JkPromotionSceneSupport.SceneType.ENTRY_ACCESS, entry.getType());
        Assert.assertNull(entry.getInviterUserId());
    }

    @Test
    public void promotionSceneOnlyCarriesInviterUntilIdentityApproval() {
        JkPromotionSceneSupport.SceneEntry entry = JkPromotionSceneSupport.parse("bind:42");

        Assert.assertEquals(JkPromotionSceneSupport.SceneType.IDENTITY_PROMOTION, entry.getType());
        Assert.assertEquals(Long.valueOf(42L), entry.getInviterUserId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownSceneIsRejectedInsteadOfFallingBackToCrmebSpread() {
        JkPromotionSceneSupport.parse("42");
    }
}
