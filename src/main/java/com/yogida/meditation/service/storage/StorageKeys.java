package com.yogida.meditation.service.storage;

import java.util.UUID;

/**
 * Generates object keys for uploads.
 *
 * <p>Keys are produced here rather than accepted from the client, and that is a correctness
 * requirement rather than tidiness. The admin UI used to send the raw upload filename as the key,
 * so two different media items whose files were both called {@code intro.mp3} resolved to one
 * object: the second upload silently overwrote the first item's audio, and then failed the
 * {@code unique(bucket, base_url, uri)} constraint when the row was written. One media item ended
 * up playing another's track.
 *
 * <p>The UUID prefix also removes a second defect by construction. The update path used to skip
 * the upload when the submitted key matched the stored one, so replacing a file without renaming
 * it kept the old audio. A freshly generated key is never equal to the previous one, so there is
 * nothing left to skip.
 */
public final class StorageKeys {

    /** Key prefix for media audio objects. */
    public static final String MEDIA_PREFIX = "media/";

    private StorageKeys() {
    }

    /**
     * Builds a collision-free key for a media object, preserving a recognisable filename for
     * whoever has to read a bucket listing later.
     */
    public static String mediaKey(String originalFilename) {
        return key(MEDIA_PREFIX, originalFilename, "audio");
    }

    /**
     * Builds a collision-free key under {@code prefix}, preserving a recognisable filename.
     *
     * <p>Three of the four upload paths spelled this concatenation out themselves and only reached
     * in here for {@code sanitise}, which left the part that must not vary — the UUID that makes
     * the key collision-free — restated at each site.
     *
     * @param prefix           key prefix, including its trailing slash
     * @param originalFilename the client-supplied name, never trusted verbatim
     * @param fallback         name to use when the client supplied nothing usable
     */
    public static String key(String prefix, String originalFilename, String fallback) {
        return prefix + UUID.randomUUID() + "-" + sanitise(originalFilename, fallback);
    }

    /**
     * Strips any path and reduces the name to characters that are safe in an object key and in
     * the URLs derived from it. A client controls this string, so it is never trusted verbatim —
     * a name containing {@code ../} or a slash would otherwise reshape the key's namespace.
     */
    public static String sanitise(String originalFilename, String fallback) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallback;
        }
        String name = originalFilename.substring(
                Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? fallback : name;
    }
}
