package com.example.bookmark;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BookmarkMetadataFetcher {
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(5);
    private static final String USER_AGENT = "bookmark-demo-java/0.0.1";

    public BookmarkMetadata fetch(String url) throws IOException {
        NormalizedUrl normalizedUrl = normalizeUrl(url);
        Document document = fetchDocument(normalizedUrl);

        String title = StringUtils.hasText(document.title()) ? document.title().trim() : normalizedUrl.url();
        Element ogpImage = document.selectFirst("meta[property=og:image], meta[name=og:image]");
        String ogpImageUrl = ogpImage != null ? ogpImage.attr("abs:content") : null;

        return new BookmarkMetadata(title, normalizedUrl.url(), optionalText(ogpImageUrl));
    }

    private static Document fetchDocument(NormalizedUrl normalizedUrl) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .dns(pinnedDns(normalizedUrl))
                .connectTimeout(FETCH_TIMEOUT)
                .readTimeout(FETCH_TIMEOUT)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();

        Request request = new Request.Builder()
                .url(normalizedUrl.url())
                .header("User-Agent", USER_AGENT)
                .header("Host", normalizedUrl.hostHeader())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed with status " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("HTTP response body is empty");
            }
            return Jsoup.parse(body.byteStream(), null, normalizedUrl.url());
        }
    }

    private static Dns pinnedDns(NormalizedUrl normalizedUrl) {
        return hostname -> {
            if (hostname.equalsIgnoreCase(normalizedUrl.host())) {
                return List.of(normalizedUrl.address());
            }
            return Dns.SYSTEM.lookup(hostname);
        };
    }

    private static NormalizedUrl normalizeUrl(String url) throws UnknownHostException {
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
            if (StringUtils.hasText(uri.getUserInfo())) {
                throw new IllegalArgumentException("URL must not include user info");
            }

            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            rejectDisallowedHost(addresses);
            return new NormalizedUrl(uri.toString(), uri.getHost(), addresses[0], hostHeader(uri));
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URL is invalid", ex);
        }
    }

    private static void rejectDisallowedHost(InetAddress[] addresses) {
        for (InetAddress address : addresses) {
            if (isDisallowedAddress(address)) {
                throw new IllegalArgumentException("URL host is not allowed");
            }
        }
    }

    private static String hostHeader(URI uri) {
        if (uri.getPort() < 0) {
            return uri.getHost();
        }
        return uri.getHost() + ":" + uri.getPort();
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
     * - IPv6 ULA           : fc00::/7
     */
    private static boolean isDisallowedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()
                || isIpv6UniqueLocalAddress(address);
    }

    private static boolean isIpv6UniqueLocalAddress(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return (bytes[0] & 0xfe) == 0xfc;
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

    private record NormalizedUrl(String url, String host, InetAddress address, String hostHeader) {
    }
}
