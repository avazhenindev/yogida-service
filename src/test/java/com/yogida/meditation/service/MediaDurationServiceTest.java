package com.yogida.meditation.service;

import com.yogida.meditation.config.MediaDurationProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the failure modes of shelling out to ffprobe, which is the one place this service
 * hands control to an external process it does not control.
 */
@DisabledOnOs(OS.WINDOWS)
class MediaDurationServiceTest {

    private Path fakeBinary;

    @AfterEach
    void cleanUp() throws IOException {
        if (fakeBinary != null) {
            Files.deleteIfExists(fakeBinary);
        }
    }

    /**
     * The defect this guards: the old implementation read the child's stdout with
     * {@code readAllBytes()} BEFORE waiting on it, so a child that never exited blocked the
     * request thread forever. There was no timeout, and adding one to {@code waitFor} would not
     * have helped — the thread never got that far. Enough malformed uploads would exhaust the
     * servlet thread pool.
     *
     * <p>Deliberately fails after a bounded wait rather than hanging: if this test ever regresses
     * it times out the build instead of passing.
     */
    @Test
    @DisplayName("a probe that never exits is killed at the timeout, not waited on forever")
    void hangingProbeIsKilledAtTheTimeout() throws Exception {
        // Ignores its arguments, produces nothing, and never exits on its own.
        fakeBinary = executableScript("#!/bin/sh\nsleep 120\n");
        MediaDurationService service = new MediaDurationService(
                new MediaDurationProperties(fakeBinary.toString(), Duration.ofMillis(500)));

        long start = System.nanoTime();
        assertThatThrownBy(() -> service.extractDurationSeconds(audioFile()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("did not finish within");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs)
                .as("must return promptly after the timeout rather than waiting on the child")
                .isLessThan(30_000);
    }

    @Test
    @DisplayName("a probe writing a duration is parsed and rounded up")
    void durationIsParsedAndRoundedUp() throws Exception {
        fakeBinary = executableScript("#!/bin/sh\necho 12.3\n");
        MediaDurationService service = new MediaDurationService(
                new MediaDurationProperties(fakeBinary.toString(), Duration.ofSeconds(10)));

        assertThat(service.extractDurationSeconds(audioFile())).isEqualTo(13);
    }

    @Test
    @DisplayName("a non-zero exit is reported with the probe's own output")
    void nonZeroExitIsReported() throws Exception {
        fakeBinary = executableScript("#!/bin/sh\necho 'moov atom not found' >&2\nexit 1\n");
        MediaDurationService service = new MediaDurationService(
                new MediaDurationProperties(fakeBinary.toString(), Duration.ofSeconds(10)));

        assertThatThrownBy(() -> service.extractDurationSeconds(audioFile()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moov atom not found");
    }

    @Test
    @DisplayName("a missing binary names the property that configures it")
    void missingBinaryIsReportedClearly() {
        MediaDurationService service = new MediaDurationService(
                new MediaDurationProperties("/nonexistent/ffprobe", Duration.ofSeconds(10)));

        assertThatThrownBy(() -> service.extractDurationSeconds(audioFile()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.media.duration.ffprobe-path");
    }

    private static MockMultipartFile audioFile() {
        return new MockMultipartFile("file", "track.mp3", "audio/mpeg", "not really audio".getBytes());
    }

    private static Path executableScript(String body) throws IOException {
        Path script = Files.createTempFile("fake-ffprobe-", ".sh");
        Files.writeString(script, body);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        return script;
    }
}
