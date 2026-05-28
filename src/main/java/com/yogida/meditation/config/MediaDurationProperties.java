package com.yogida.meditation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the media duration extraction service.
 *
 * @param ffprobePath path or name of the {@code ffprobe} binary
 *                   (defaults to {@code ffprobe}, assuming it is on PATH)
 */
@ConfigurationProperties(prefix = "app.media.duration")
public record MediaDurationProperties(String ffprobePath) {

    public MediaDurationProperties {
        if (ffprobePath == null || ffprobePath.isBlank()) {
            ffprobePath = "ffprobe";
        }
    }
}
