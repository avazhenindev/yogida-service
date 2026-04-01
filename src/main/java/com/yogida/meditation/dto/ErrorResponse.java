package com.yogida.meditation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Standard error response body")
public class ErrorResponse {

    @Schema(description = "ISO-8601 timestamp of the error", example = "2026-03-18T14:30:00.123456")
    private String timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP status reason phrase", example = "Bad Request")
    private String error;

    @Schema(description = "Human-readable error message", example = "Required request parameter 'bucketName' is not present")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/meditation/stream-link")
    private String path;
}

