package com.yogida.meditation.service.storage;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validation and normalisation for the storage configuration values that reach object keys and
 * the URLs derived from them.
 *
 * <p>Both helpers here were previously private methods duplicated across storage services, which
 * is how they came to be applied inconsistently: a base URL normalised in one upload path and not
 * another produces object rows whose {@code base_url} differs only by a trailing slash, and the
 * {@code unique(bucket, base_url, uri)} constraint treats those as distinct objects.
 */
public final class StorageConfigs {

    private StorageConfigs() {
    }

    /**
     * Strips trailing slashes so a base URL concatenates with a key exactly once, whatever the
     * operator typed into configuration.
     */
    public static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Returns a configured bucket name, or fails naming the property that is wrong.
     *
     * <p>Deliberately a 500 rather than {@code IllegalStateException}, which
     * {@code GlobalExceptionHandler} maps to a 401 "Authenticated user is not provisioned" — a
     * misconfigured bucket would otherwise read to an administrator as a sign-in problem.
     *
     * @param value      the configured value, possibly null or blank
     * @param property   the property name, used verbatim in the failure message
     * @param describedAs what the bucket is for, used verbatim in the failure message
     */
    public static String requireBucketName(String value, String property, String describedAs) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    property + " (" + describedAs + ") is not configured");
        }
        if (value.contains("://") || value.contains("/")) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    property + " must be a bucket name, not a URL (got: " + value + ")");
        }
        return value;
    }
}
