package com.zbkj.service.service.impl.jiuzhoukang.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class JkS3CompatibleClientMinioTest {

    @Test
    public void connectionTestDeletesTestObjectAfterFailedWrite() throws Exception {
        AtomicBoolean deleted = new AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/bucket", exchange -> {
            if ("PUT".equals(exchange.getRequestMethod())) exchange.sendResponseHeaders(500, -1);
            else if ("DELETE".equals(exchange.getRequestMethod())) {
                deleted.set(exchange.getRequestURI().getPath().contains("/__crmeb_connection_test__/"));
                exchange.sendResponseHeaders(204, -1);
            }
            exchange.close();
        });
        server.start();
        try {
            try {
                new JkS3CompatibleClient().testWriteDelete("http://localhost:" + server.getAddress().getPort(), "bucket", "access", "secret", "us-east-1");
                Assert.fail("expected failed connection test");
            } catch (IllegalStateException expected) {
                Assert.assertEquals("MinIO 连接测试失败", expected.getMessage());
            }
            Assert.assertTrue(deleted.get());
        } finally {
            server.stop(0);
        }
    }
}
