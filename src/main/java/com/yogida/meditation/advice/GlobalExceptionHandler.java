package com.yogida.meditation.advice;

import com.yogida.meditation.exception.BreathingNotFoundException;
import com.yogida.meditation.exception.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.warn("Validation failed on [{} {}]: {}", request.getMethod(), request.getRequestURI(), errors);
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Validation Failed",
                "errors", errors,
                "path", request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<Map<String, Object>> handleS3Exception(S3Exception ex, HttpServletRequest request) {
        log.error("S3 error on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_GATEWAY, "Storage service error", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex,
                                                                   HttpServletRequest request) {
        log.warn("Missing request parameter on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                   HttpServletRequest request) {
        log.warn("Type mismatch on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex,
                                                               HttpServletRequest request) {
        log.warn("Resource not found [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex,
                                                                      HttpServletRequest request) {
        log.warn("Illegal argument on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncTimeout(AsyncRequestTimeoutException ex, HttpServletRequest request) {
        log.debug("SSE stream timed out [{} {}]", request.getMethod(), request.getRequestURI());
    }

    /**
     * Raised by the servlet container when an async (SSE) response is written to after the
     * client disconnected — routine on mobile networks (app suspension, network switch).
     * The emitter registry already evicts dead connections; nothing to report.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncNotUsable(AsyncRequestNotUsableException ex, HttpServletRequest request) {
        log.debug("SSE client disconnected [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex,
                                                                     HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (status.is5xxServerError()) {
            log.error("Response status error on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("Response status error on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return buildResponse(status, message, request);
    }

    /**
     * A valid token whose subject has no {@code app_user} row.
     *
     * <p>{@code CurrentUserService.getCurrentUserOrThrow()} raises this, and nothing handled
     * it — so the catch-all below turned it into a 500. That is the wrong answer and the wrong
     * signal: the caller is authenticated but not provisioned, which is a 401 telling the
     * client to complete sign-in, not a server fault. Reproduced with a real token during the
     * infrastructure work: every endpoint returned 500 for a user Keycloak knew and the
     * database did not.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex,
                                                                   HttpServletRequest request) {
        log.warn("Unresolvable caller on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authenticated user is not provisioned", request);
    }

    /**
     * A database constraint refused the write.
     *
     * <p>Unhandled, this surfaced as a 500 for entirely ordinary situations: deleting a media
     * item that still has reviews (media_review's foreign keys have no cascade), or racing two
     * identical favourites past the check-then-insert into the unique constraint. 409 says what
     * actually happened.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                             HttpServletRequest request) {
        log.warn("Constraint violation on [{} {}]: {}", request.getMethod(), request.getRequestURI(),
            ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT,
            "The request conflicts with existing data or a related record still references it", request);
    }

    /**
     * An authenticated caller without the required role. Spring Security raises this from
     * {@code @PreAuthorize} and the filter chain; letting it reach the catch-all would report
     * a refusal as a server error.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
                                                                    HttpServletRequest request) {
        log.warn("Access denied on [{} {}]", request.getMethod(), request.getRequestURI());
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex,
                                                                     HttpServletRequest request) {
        log.warn("Entity not found [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoSuchBucketException.class)
    public ResponseEntity<Map<String, Object>> handleNoSuchBucketException(NoSuchBucketException ex, HttpServletRequest request) {
        log.error("Bucket not found on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.NOT_FOUND, "Bucket not found ", request);
    }

    /**
     * Returns an RFC 9457 {@link ProblemDetail} response for breathing exercise / phase not found.
     */
    @ExceptionHandler(BreathingNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleBreathingNotFound(BreathingNotFoundException ex,
                                                                  HttpServletRequest request) {
        log.warn("Breathing not found [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Breathing Exercise Not Found");
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message,
                                                               HttpServletRequest request) {
        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
