package com.zbkj.service.service.jiuzhoukang.audit;

import com.zbkj.common.model.jiuzhoukang.JkAuditLog;
import com.zbkj.common.response.jiuzhoukang.JkAuditLogResponse;
import com.zbkj.service.service.impl.jiuzhoukang.audit.JkAuditLogServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class JkAuditLogServiceImplTest {

    @Test
    public void mapsAuditActionAndStatusDisplayTexts() {
        JkAuditLogServiceImpl service = new JkAuditLogServiceImpl();
        JkAuditLog log = new JkAuditLog();
        log.setBusinessType("PLATFORM_ORDER");
        log.setAuditAction("SHIP");
        log.setBeforeStatus("PAYMENT_APPROVED");
        log.setAfterStatus("SHIPPED");

        List<JkAuditLogResponse> responses = service.toResponses(Collections.singletonList(log));

        Assert.assertEquals(1, responses.size());
        JkAuditLogResponse response = responses.get(0);
        Assert.assertEquals("平台订货", response.getBusinessTypeText());
        Assert.assertEquals("SHIP", response.getAuditAction());
        Assert.assertEquals("PAYMENT_APPROVED", response.getBeforeStatus());
        Assert.assertEquals("SHIPPED", response.getAfterStatus());
        Assert.assertEquals("发货", response.getAuditActionText());
        Assert.assertEquals("付款审核通过", response.getBeforeStatusText());
        Assert.assertEquals("已发货", response.getAfterStatusText());
    }
}
