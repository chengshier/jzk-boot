package com.zbkj.service.service.impl.jiuzhoukang.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.Assert;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class JkS3CompatibleClientMinioTest {

    @Test
    public void objectRequestDisablesRedirectsBeforeA302Response() throws Exception {
        RedirectConnection connection = new RedirectConnection();
        JkS3CompatibleClient client = new JkS3CompatibleClient() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                return connection;
            }
        };

        try {
            client.get("http://198.51.100.10", "bucket", "object", "access", "secret", "us-east-1");
            Assert.fail("expected redirect response to fail");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("HTTP 302"));
        }

        Assert.assertTrue("redirect-to-loopback must not be followed", connection.redirectsDisabled);
    }

    @Test(expected = IllegalArgumentException.class)
    public void connectionTestRejectsHostnameBeforeAnyWriteCanResolveItAgain() {
        JkS3CompatibleClient client = new JkS3CompatibleClient() {
            @Override
            public void put(String endpoint, String bucket, String objectKey, byte[] bytes, String contentType,
                            String accessKey, String secretKey, String region) {
                Assert.fail("hostname must be rejected before opening a connection");
            }
        };

        client.testWriteDeletePublicIp("http://rebinding.example.com", "bucket", "access", "secret", "us-east-1");
    }

    @Test
    public void administrativeConnectionTestKeepsTheValidatedLiteralAddress() {
        AtomicBoolean receivedLiteralEndpoint = new AtomicBoolean(false);
        JkS3CompatibleClient client = new JkS3CompatibleClient() {
            @Override
            public void put(String endpoint, String bucket, String objectKey, byte[] bytes, String contentType,
                            String accessKey, String secretKey, String region) {
                receivedLiteralEndpoint.set("https://8.8.8.8:9443".equals(endpoint));
            }

            @Override
            public void delete(String endpoint, String bucket, String objectKey, String accessKey, String secretKey, String region) { }
        };

        client.testWriteDeletePublicIp("https://8.8.8.8:9443", "bucket", "access", "secret", "us-east-1");

        Assert.assertTrue(receivedLiteralEndpoint.get());
    }

    @Test
    public void connectionTestReportsCleanupFailureAfterSuccessfulWrite() {
        JkS3CompatibleClient client = new JkS3CompatibleClient() {
            @Override
            public void put(String endpoint, String bucket, String objectKey, byte[] bytes, String contentType,
                            String accessKey, String secretKey, String region) { }

            @Override
            public void delete(String endpoint, String bucket, String objectKey, String accessKey, String secretKey, String region) {
                throw new IllegalStateException("cleanup failed");
            }
        };

        try {
            client.testWriteDelete("http://minio.example.com", "bucket", "access", "secret", "us-east-1");
            Assert.fail("expected cleanup failure");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("对象清理失败"));
            Assert.assertEquals("cleanup failed", expected.getCause().getMessage());
            Assert.assertFalse(expected.getMessage().contains("secret"));
        }
    }

    @Test
    public void connectionTestPreservesCleanupFailureWhenWriteAndCleanupBothFail() {
        JkS3CompatibleClient client = new JkS3CompatibleClient() {
            @Override
            public void put(String endpoint, String bucket, String objectKey, byte[] bytes, String contentType,
                            String accessKey, String secretKey, String region) {
                throw new IllegalStateException("write failed");
            }

            @Override
            public void delete(String endpoint, String bucket, String objectKey, String accessKey, String secretKey, String region) {
                throw new IllegalStateException("cleanup failed");
            }
        };

        try {
            client.testWriteDelete("http://minio.example.com", "bucket", "access", "secret", "us-east-1");
            Assert.fail("expected failed connection test");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("连接测试失败"));
            Assert.assertEquals(1, expected.getSuppressed().length);
            Assert.assertEquals("cleanup failed", expected.getSuppressed()[0].getMessage());
            Assert.assertFalse(expected.getMessage().contains("secret"));
        }
    }

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

    private static class RedirectConnection extends HttpURLConnection {
        private boolean redirectsDisabled;

        private RedirectConnection() throws Exception {
            super(new URL("http://198.51.100.10"));
        }

        @Override
        public void setInstanceFollowRedirects(boolean followRedirects) {
            redirectsDisabled = !followRedirects;
        }

        @Override
        public int getResponseCode() {
            return 302;
        }

        @Override
        public void disconnect() { }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() { }
    }
}
