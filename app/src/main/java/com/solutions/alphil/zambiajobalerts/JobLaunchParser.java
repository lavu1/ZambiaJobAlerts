package com.solutions.alphil.zambiajobalerts;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public final class JobLaunchParser {
    private JobLaunchParser() {}

    public static String extractIdentifier(String scheme, String host, String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String normalizedScheme = normalizePart(scheme);
        String normalizedHost = normalizePart(host);

        if ("zambiajobalerts".equals(normalizedScheme)) {
            if (!"job".equals(normalizedHost) && !"jobs".equals(normalizedHost)) {
                return null;
            }
            return normalizeIdentifier(path);
        }

        if (!isWebScheme(normalizedScheme) || !isSupportedWebHost(normalizedHost)) {
            return null;
        }

        if (path.startsWith("/job/")) {
            return normalizeIdentifier(path.substring("/job/".length()));
        }

        if (path.startsWith("/jobs/")) {
            return normalizeIdentifier(path.substring("/jobs/".length()));
        }

        return null;
    }

    public static boolean isHomeUri(String scheme, String host, String path) {
        String normalizedScheme = normalizePart(scheme);
        String normalizedHost = normalizePart(host);
        String normalizedPath = path == null ? "" : path.trim();

        if ("zambiajobalerts".equals(normalizedScheme)) {
            return "home".equals(normalizedHost);
        }

        return isWebScheme(normalizedScheme)
                && isSupportedWebHost(normalizedHost)
                && (normalizedPath.isEmpty() || "/".equals(normalizedPath));
    }

    public static String buildJobDetailsBySlugUrl(String baseUrl, String slug) {
        return baseUrl + "?slug=" + encodeQueryValue(slug) + "&_embed=1";
    }

    private static String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is unavailable", e);
        }
    }

    private static String normalizeIdentifier(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();

        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        int nextSlash = value.indexOf('/');
        if (nextSlash >= 0) {
            value = value.substring(0, nextSlash);
        }

        return value.isEmpty() ? null : value;
    }

    private static String normalizePart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isWebScheme(String scheme) {
        return "http".equals(scheme) || "https".equals(scheme);
    }

    private static boolean isSupportedWebHost(String host) {
        return "zambiajobalerts.com".equals(host) || "www.zambiajobalerts.com".equals(host);
    }
}
