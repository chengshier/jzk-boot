package com.zbkj.service.service.impl.jiuzhoukang.health;

import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.Test;

public class SinocarePayloadStructureLoggerTest {
    @Test
    public void describesOnlyFieldNamesTypesAndArraySize() {
        String actual = SinocarePayloadStructureLogger.describe(JSON.parseObject(
                "{\"uniqueId\":\"a-real-user-id\",\"deviceSn\":\"DEVICE-SECRET\",\"data\":[{\"value\":5.6,\"time\":1710000000000}]}"));

        Assert.assertTrue(actual.contains("uniqueId:string"));
        Assert.assertTrue(actual.contains("deviceSn:string"));
        Assert.assertTrue(actual.contains("data:array(size=1"));
        Assert.assertFalse(actual.contains("a-real-user-id"));
        Assert.assertFalse(actual.contains("DEVICE-SECRET"));
        Assert.assertFalse(actual.contains("5.6"));
    }
}
