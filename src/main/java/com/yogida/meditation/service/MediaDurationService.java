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
import java.util.concurrent.TimeUnit;

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

    /**
     * Runs ffprobe against a file, bounded by {@link MediaDurationProperties#timeout()}.
     *
     * <p>The output goes to a FILE rather than a pipe, and that is the whole point. The previous
     * implementation read the pipe with {@code readAllBytes()} before calling {@code waitFor()},
     * which blocks until the child closes stdout — so a child that never exits hung the request
     * thread forever, and adding a timeout to {@code waitFor} would have changed nothing because
     * the thread never reached it. Redirecting to a file means nothing blocks on the child, and
     * {@code waitFor(timeout)} is genuinely the only wait.
     *
     * <p>The child's stdin is closed immediately after start, so ffprobe cannot block reading
     * input that will never arrive.
     */
    private int runFfprobe(Path filePath) throws IOException {
        Path outputFile = Files.createTempFile("media-duration-out-", ".txt");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    properties.ffprobePath(),
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    filePath.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(outputFile.toFile());

            Process process;
            try {
                process = pb.start();
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "ffprobe not found at '" + properties.ffprobePath() + "'. Install ffprobe or provide its path via app.media.duration.ffprobe-path.", e);
            }

            String output;
            try {
                // Give the child EOF on stdin so it can never block waiting for input.
                process.getOutputStream().close();

                if (!process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    // Reap it, so a killed child is not left as a zombie.
                    process.waitFor(5, TimeUnit.SECONDS);
                    throw new IllegalArgumentException(
                            "ffprobe did not finish within " + properties.timeout()
                                    + "; the file is probably malformed");
                }
                output = Files.readString(outputFile).trim();
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new IllegalArgumentException("ffprobe exited with code " + exitCode + ": " + output);
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
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
        } finally {
            deleteSilently(outputFile);
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
