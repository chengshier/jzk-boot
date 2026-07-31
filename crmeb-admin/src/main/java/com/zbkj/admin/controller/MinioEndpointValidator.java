package com.zbkj.admin.controller;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

/** Validates MinIO endpoints before an administrative connection test can write to them. */
public class MinioEndpointValidator {

    interface AddressResolver {
        InetAddress[] resolve(String host) throws Exception;
    }

    private final AddressResolver addressResolver;

    public MinioEndpointValidator() {
        this(InetAddress::getAllByName);
    }

    MinioEndpointValidator(AddressResolver addressResolver) {
        this.addressResolver = addressResolver;
    }

    public void validate(String endpoint) {
        URI uri;
        try {
            uri = new URI(endpoint);
        } catch (URISyntaxException exception) {
            throw rejected();
        }
        if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw rejected();
        }
        try {
            InetAddress[] addresses = addressResolver.resolve(uri.getHost());
            if (addresses == null || addresses.length == 0) {
                throw rejected();
            }
            for (InetAddress address : addresses) {
                if (isDisallowed(address)) throw rejected();
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw rejected();
        }
    }

    private boolean isDisallowed(InetAddress address) {
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isMulticastAddress() || address.isSiteLocalAddress()) {
            return true;
        }
        if (address instanceof Inet6Address) {
            byte first = address.getAddress()[0];
            return (first & 0xfe) == 0xfc;
        }
        return false;
    }

    private IllegalArgumentException rejected() {
        return new IllegalArgumentException("MinIO Endpoint 必须是可访问的公共 HTTP/HTTPS 地址");
    }
}
