package com.yogida.meditation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for duration extraction.
 *
 * @param ffprobePath path or name of the {@code ffprobe} binary
 *                    (defaults to {@code ffprobe}, assuming it is on PATH)
 * @param timeout     how long to wait for ffprobe before killing it. Bounds the damage a
 *                    malformed or adversarial upload can do: without a limit, one request
 *                    thread is lost permanently per bad file, and enough of them exhaust the
 *                    container's thread pool.
 */
@ConfigurationProperties(prefix = "app.media.duration")
public record MediaDurationProperties(String ffprobePath, Duration timeout) {
    public MediaDurationProperties {
        if (ffprobePath == null || ffprobePath.isBlank()) {
            ffprobePath = "ffprobe";
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            timeout = Duration.ofSeconds(30);
        }
    }
}
