package com.zbkj.admin.controller;

import org.junit.Test;

import java.net.InetAddress;

public class MinioEndpointValidatorTest {

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLoopbackResolvedEndpoint() throws Exception {
        new MinioEndpointValidator(host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")})
                .validate("http://minio.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEndpointWhenAnyResolvedAddressIsPrivate() throws Exception {
        new MinioEndpointValidator(host -> new InetAddress[]{
                InetAddress.getByName("8.8.8.8"), InetAddress.getByName("192.168.1.10")
        }).validate("https://minio.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIpv6UniqueLocalAddress() throws Exception {
        new MinioEndpointValidator(host -> new InetAddress[]{InetAddress.getByName("fc00::1")})
                .validate("https://minio.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIpv6LinkLocalAddress() throws Exception {
        new MinioEndpointValidator(host -> new InetAddress[]{InetAddress.getByName("fe80::1")})
                .validate("https://minio.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyDnsResolution() {
        new MinioEndpointValidator(host -> new InetAddress[0])
                .validate("https://minio.example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonHttpSchemeWithoutResolvingDns() {
        new MinioEndpointValidator(host -> { throw new AssertionError("DNS should not be called"); })
                .validate("file:///tmp/minio");
    }

    @Test
    public void permitsPublicHttpEndpoint() throws Exception {
        new MinioEndpointValidator(host -> new InetAddress[]{InetAddress.getByName("8.8.8.8")})
                .validate("https://minio.example.com");
    }
}
