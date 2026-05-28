package com.yogida.meditation.service;

import com.yogida.meditation.config.MediaDurationProperties;
import com.yogida.meditation.service.api.MediaDurationApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extracts media duration using {@code ffprobe}.
 * <p>
 * The uploaded file is written to a temp path, inspected, and immediately deleted.
 * If {@code ffprobe} is not available or the file is unreadable the method throws
 * {@link IllegalArgumentException} so the caller can surface a clear validation error.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class MediaDurationService implements MediaDurationApi {

    private final MediaDurationProperties properties;

    @Override
    public int extractDurationSeconds(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot extract duration: media file is empty");
        }

        Path tempFile = null;
        try {
            String suffix = resolveExtension(file.getOriginalFilename());
            tempFile = Files.createTempFile("media-duration-", suffix);
            file.transferTo(tempFile.toFile());
            return runFfprobe(tempFile);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to extract duration from media file: " + e.getMessage(), e);
        } finally {
            deleteSilently(tempFile);
        }
    }

    private int runFfprobe(Path filePath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                properties.ffprobePath(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "ffprobe not found at '" + properties.ffprobePath() + "'. Install ffprobe or provide its path via app.media.duration.ffprobe-path.", e);
        }

        String output;
        try {
            output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalArgumentException("ffprobe exited with code " + exitCode + ": " + output);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Duration extraction was interrupted", e);
        }

        if (output.isEmpty() || output.equalsIgnoreCase("N/A")) {
            throw new IllegalArgumentException("ffprobe returned no duration for the provided file");
        }

        try {
            double seconds = Double.parseDouble(output);
            int result = (int) Math.ceil(seconds);
            if (result <= 0) {
                throw new IllegalArgumentException("Extracted duration must be greater than zero, got: " + seconds);
            }
            log.debug("MediaDurationService > extracted duration={} seconds from file={}", result, filePath.getFileName());
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Could not parse duration from ffprobe output: '" + output + "'", e);
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return ".tmp";
    }

    private void deleteSilently(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("MediaDurationService > failed to delete temp file {}: {}", path, e.getMessage());
            }
        }
    }
}
