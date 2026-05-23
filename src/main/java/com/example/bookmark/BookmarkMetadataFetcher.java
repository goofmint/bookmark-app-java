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

    /**
     * ループバック・プライベートアドレス・リンクローカル等、
     * 内部ネットワーク向けアドレスへのアクセスを禁止する。
     * 本アプリはローカル実行のシングルユーザーデモのため、
     * JVM 組み込みチェックのみで十分とする。
     * 将来的に本番用途に転用する場合は、クラウドメタデータエンドポイント
     * (例: 169.254.169.254) などへの追加ブロックも検討すること。
     *
     * - isAnyLocalAddress : 0.0.0.0 / ::
     * - isLoopbackAddress  : 127.0.0.0/8 / ::1
     * - isSiteLocalAddress : 10/8, 172.16/12, 192.168/16
     * - isLinkLocalAddress : 169.254/16 / fe80::/10
     * - isMulticastAddress : 224.0.0.0/4 / ff00::/8
     */
    private static boolean isDisallowedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress();
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
