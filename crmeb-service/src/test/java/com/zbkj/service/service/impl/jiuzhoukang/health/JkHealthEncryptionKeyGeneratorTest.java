package com.zbkj.service.service.impl.jiuzhoukang.health;

import org.junit.Assert;
import org.junit.Test;

public class JkHealthEncryptionKeyGeneratorTest {
    @Test
    public void generatesAUrlSafe256BitSecret() {
        String key = JkHealthEncryptionKeyGenerator.generate();

        Assert.assertTrue(key.matches("[A-Za-z0-9_-]{43}"));
    }
}
