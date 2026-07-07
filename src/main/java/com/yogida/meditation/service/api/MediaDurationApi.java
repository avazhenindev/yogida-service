package com.yogida.meditation.service.api;

import org.springframework.web.multipart.MultipartFile;

public interface MediaDurationApi {

    /**
     * Extracts the duration in whole seconds from the provided media file.
     * <p>
     * Uses {@code ffprobe} when available on the deployment host.
     * Throws {@link IllegalArgumentException} when the file is unreadable or has no valid duration.
     *
     * @param file media file to inspect
     * @return duration in seconds (always &gt; 0)
     */
    int extractDurationSeconds(MultipartFile file);
}
