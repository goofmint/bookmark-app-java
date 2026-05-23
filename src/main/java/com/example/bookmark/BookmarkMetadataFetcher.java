package com.example.bookmark;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BookmarkMetadataFetcher {
    public BookmarkMetadata fetch(String url) {
        String normalizedUrl = normalizeUrl(url);
        try {
            Document document = Jsoup.connect(normalizedUrl)
                    .userAgent("bookmark-demo-java/0.0.1")
                    .timeout(5000)
                    .followRedirects(false)
                    .get();

            String title = StringUtils.hasText(document.title()) ? document.title().trim() : normalizedUrl;
            Element ogpImage = document.selectFirst("meta[property=og:image], meta[name=og:image]");
            String ogpImageUrl = ogpImage != null ? ogpImage.attr("abs:content") : null;

            return new BookmarkMetadata(title, normalizedUrl, optionalText(ogpImageUrl));
        } catch (IOException ex) {
            return new BookmarkMetadata(normalizedUrl, normalizedUrl, null);
        }
    }

    private static String normalizeUrl(String url) {
        String trimmedUrl = requireText(url);
        String withScheme = trimmedUrl.matches("(?i)^[a-z][a-z0-9+.-]*://.*") ? trimmedUrl : "https://" + trimmedUrl;
        try {
            URI uri = new URI(withScheme);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("URL scheme must be http or https");
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("URL must include a host");
            }
            rejectDisallowedHost(uri.getHost());
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URL is invalid", ex);
        }
    }

    private static void rejectDisallowedHost(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isDisallowedAddress(address)) {
                    throw new IllegalArgumentException("URL host is not allowed");
                }
            }
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("URL host cannot be resolved", ex);
        }
    }

    private static boolean isDisallowedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isDisallowedIpv4(bytes, 0);
        }
        return bytes.length == 16 && isDisallowedIpv6(bytes);
    }

    private static boolean isDisallowedIpv4(byte[] bytes, int offset) {
        int first = Byte.toUnsignedInt(bytes[offset]);
        int second = Byte.toUnsignedInt(bytes[offset + 1]);
        int third = Byte.toUnsignedInt(bytes[offset + 2]);

        return first == 0
                || first == 100 && second >= 64 && second <= 127
                || first == 192 && second == 0
                || first == 198 && (second == 18 || second == 19)
                || first == 198 && second == 51 && third == 100
                || first == 203 && second == 0 && third == 113
                || first >= 240;
    }

    private static boolean isDisallowedIpv6(byte[] bytes) {
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);

        return (first & 0xfe) == 0xfc
                || (first == 0x20
                && second == 0x01
                && Byte.toUnsignedInt(bytes[2]) == 0x0d
                && Byte.toUnsignedInt(bytes[3]) == 0xb8)
                || (isIpv4MappedIpv6(bytes) && isDisallowedIpv4(bytes, 12));
    }

    private static boolean isIpv4MappedIpv6(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    private static String requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("url is required");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
