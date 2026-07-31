package com.zbkj.admin.controller;

import org.junit.Test;

public class MinioEndpointValidatorTest {

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDnsHostnameToPreventRebinding() {
        new MinioEndpointValidator().validate("https://minio.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLoopbackLiteralEndpoint() {
        new MinioEndpointValidator().validate("http://127.0.0.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCarrierGradeNatAddress() {
        new MinioEndpointValidator().validate("http://100.64.0.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBenchmarkingAddress() {
        new MinioEndpointValidator().validate("http://198.18.0.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIpv6UniqueLocalAddress() throws Exception {
        new MinioEndpointValidator().validate("https://[fc00::1]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIpv6LinkLocalAddress() throws Exception {
        new MinioEndpointValidator().validate("https://[fe80::1]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonLiteralHostWithoutResolvingDns() {
        new MinioEndpointValidator().validate("https://not-an-ip.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonHttpSchemeWithoutResolvingDns() {
        new MinioEndpointValidator().validate("file:///tmp/minio");
    }

    @Test
    public void permitsPublicLiteralEndpoint() {
        new MinioEndpointValidator().validate("https://8.8.8.8");
    }
}
