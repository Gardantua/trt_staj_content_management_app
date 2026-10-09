package com.trt.contentengagement.content.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class OfficialWatchUrl {

    private static final Set<String> ALLOWED_HOSTS = Set.of("tabii.com", "www.tabii.com");
    private static final Pattern DETAIL_PATH = Pattern.compile(
            "^/(?:[a-zA-Z]{2}(?:-[a-zA-Z]{2})?/)?detail/[1-9][0-9]*(?:/[^/%]+)?/?$"
    );

    private OfficialWatchUrl() {
    }

    static String normalizeNullable(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String trimmed = candidate.trim();
        if (trimmed.length() > 500) {
            throw invalid();
        }

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException exception) {
            throw invalid();
        }
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || host == null
                || !ALLOWED_HOSTS.contains(host.toLowerCase(Locale.ROOT))
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !DETAIL_PATH.matcher(uri.getRawPath()).matches()) {
            throw invalid();
        }

        try {
            return new URI(
                    "https",
                    null,
                    host.toLowerCase(Locale.ROOT),
                    -1,
                    uri.getPath(),
                    null,
                    null
            ).toASCIIString();
        } catch (URISyntaxException exception) {
            throw invalid();
        }
    }

    private static ContentRuleViolationException invalid() {
        return new ContentRuleViolationException(
                "CONTENT_WATCH_URL_INVALID",
                "Watch URL must be an official HTTPS tabii content detail URL."
        );
    }
}
