package com.example.bookmark;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

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
                    .followRedirects(true)
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
        String withScheme = trimmedUrl.matches("(?i)^https?://.*") ? trimmedUrl : "https://" + trimmedUrl;
        try {
            URI uri = new URI(withScheme);
            if (!StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("URL must include a host");
            }
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("URL is invalid", ex);
        }
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
