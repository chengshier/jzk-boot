package com.zbkj.admin.controller;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates MinIO endpoints before an administrative connection test can write to them.
 *
 * The connection test intentionally accepts only public IP literals.  HttpURLConnection resolves
 * host names when opening a connection, so accepting a DNS name after a one-time validation would
 * allow a DNS-rebinding response to select a different destination.
 */
public class MinioEndpointValidator {

    public String validate(String endpoint) {
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
        InetAddress address = literalAddress(uri.getHost());
        if (isDisallowed(address)) throw rejected();
        return endpoint;
    }

    private InetAddress literalAddress(String host) {
        String value = host;
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        if (!isIpv4Literal(value) && value.indexOf(':') < 0) throw rejected();
        try {
            return InetAddress.getByName(value);
        } catch (Exception exception) {
            throw rejected();
        }
    }

    private boolean isIpv4Literal(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) return false;
            for (int index = 0; index < part.length(); index++) {
                if (!Character.isDigit(part.charAt(index))) return false;
            }
            if (Integer.parseInt(part) > 255) return false;
        }
        return true;
    }

    private boolean isDisallowed(InetAddress address) {
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isMulticastAddress() || address.isSiteLocalAddress()) {
            return true;
        }
        if (address.getAddress().length == 4) return isNonGlobalIpv4(address.getAddress());
        return address.getAddress().length == 16 && (address.getAddress()[0] & 0xfe) == 0xfc;
    }

    /** RFC 6890 special-use IPv4 ranges that must never be treated as public egress targets. */
    private boolean isNonGlobalIpv4(byte[] address) {
        int first = address[0] & 0xff;
        int second = address[1] & 0xff;
        int third = address[2] & 0xff;
        return first == 0
                || first == 10
                || (first == 100 && second >= 64 && second <= 127)
                || first == 127
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 0 && (third == 0 || third == 2))
                || (first == 192 && second == 88 && third == 99)
                || (first == 192 && second == 168)
                || (first == 198 && (second == 18 || second == 19))
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113)
                || first >= 224;
    }

    private IllegalArgumentException rejected() {
        return new IllegalArgumentException("MinIO Endpoint 仅支持可访问的公共 HTTP/HTTPS IP 地址（不支持域名）");
    }
}
