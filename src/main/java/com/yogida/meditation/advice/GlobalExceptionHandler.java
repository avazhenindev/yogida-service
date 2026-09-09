package com.yogida.meditation.advice;

import com.yogida.meditation.exception.BreathingNotFoundException;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.exception.NotProvisionedException;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;

/**
 * One error contract for the whole API: RFC 9457 {@link ProblemDetail}.
 *
 * <p>There used to be three shapes on the wire — an ad-hoc {@code Map} from most handlers, a
 * differently-shaped {@code Map} for validation failures (no {@code message}, an extra
 * {@code errors} array), and a real {@code ProblemDetail} from exactly one handler. A client
 * could not parse errors uniformly because there was no uniform error.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} does more than tidy the shape: it picks up
 * twenty framework exception types that previously fell through to the catch-all and became 500s.
 * Malformed JSON in a request body now answers 400 instead of 500, an unsupported HTTP verb 405,
 * and an upload past the configured multipart cap 413.
 *
 * <p><b>Handlers here must never declare an {@code @ExceptionHandler} for a type the base class
 * already claims.</b> {@code ResponseEntityExceptionHandler.handleException} is {@code final} and
 * annotated with all twenty, so a duplicate registration is not an override — it is an ambiguous
 * mapping, and the application refuses to start. The correct way to change the behaviour of one
 * of those types is to override its {@code protected} hook, as done below for the two async ones.
 *
 * <p>Fields relative to the old shape: {@code message} is now {@code detail}, {@code error} is
 * {@code title}, {@code path} is {@code instance}, and {@code timestamp} is gone. {@code status}
 * is unchanged. {@code type} is absent unless a handler sets it — it is not defaulted to
 * {@code about:blank} on the wire. Every field except {@code status} may be absent, because
 * {@code ProblemDetail} serialises with {@code NON_EMPTY}.
 */
@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // --- overrides of base-class hooks -------------------------------------

    /**
     * Keeps the per-field validation messages, which the base class does not include.
     *
     * <p>They move from a top-level {@code errors} array to a {@code ProblemDetail} extension
     * property of the same name, so the information survives the change of shape.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.warn("Validation failed on [{}]: {}", request.getDescription(false), errors);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * An SSE stream reaching its timeout. Returning null writes nothing: the response is already
     * committed and the emitter registry evicts the connection on its own.
     *
     * <p>This has to be an override rather than an {@code @ExceptionHandler} — see the class
     * javadoc. As a duplicate registration it prevented the context from starting at all.
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleAsyncRequestTimeoutException(
            AsyncRequestTimeoutException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        log.debug("SSE stream timed out [{}]", request.getDescription(false));
        return null;
    }

    /**
     * Raised by the container when an async (SSE) response is written to after the client
     * disconnected — routine on mobile networks (app suspension, network switch). Nothing to
     * report and nowhere to report it to.
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleAsyncRequestNotUsableException(
            AsyncRequestNotUsableException ex, WebRequest request) {
        log.debug("SSE client disconnected [{}]: {}", request.getDescription(false), ex.getMessage());
        return null;
    }

    /**
     * The single funnel every base-class-handled exception passes through, which is why the
     * logging lives here.
     *
     * <p>Deleting the old {@code ResponseStatusException} handler would otherwise have silently
     * removed the only place a 5xx raised through {@code ResponseStatusException} was logged with
     * a stack trace — and the service raises several, including a misconfigured storage bucket.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Server error on [{}]: {}", request.getDescription(false), ex.getMessage(), ex);
        } else {
            log.warn("Client error {} on [{}]: {}", statusCode.value(),
                    request.getDescription(false), ex.getMessage());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // --- application exceptions --------------------------------------------
    // None of the types below appear in the base class's list; adding one that does
    // would stop the application from starting.

    @ExceptionHandler(S3Exception.class)
    public ProblemDetail handleS3Exception(S3Exception ex, WebRequest request) {
        log.error("S3 error on [{}]: {}", request.getDescription(false), ex.getMessage(), ex);
        return problem(HttpStatus.BAD_GATEWAY, "Storage service error", "Storage Unavailable");
    }

    @ExceptionHandler(NoSuchBucketException.class)
    public ProblemDetail handleNoSuchBucket(NoSuchBucketException ex, WebRequest request) {
        log.error("Bucket not found on [{}]: {}", request.getDescription(false), ex.getMessage(), ex);
        return problem(HttpStatus.NOT_FOUND, "Bucket not found", "Bucket Not Found");
    }

    /**
     * Kept deliberately. The base class claims the supertype {@code TypeMismatchException}, not
     * this subtype, so a handler here is legal and produces a more specific message.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Type mismatch on [{}]: {}", request.getDescription(false), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), "Invalid Parameter");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument on [{}]: {}", request.getDescription(false), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), "Bad Request");
    }

    /**
     * A valid token whose subject has no {@code app_user} row.
     *
     * <p>Nothing handled this, so the catch-all turned it into a 500 — the wrong answer and the
     * wrong signal. The caller is authenticated but not provisioned, which is a 401 telling the
     * client to complete sign-in, not a server fault.
     *
     * <p>Matched on a dedicated type rather than on {@code IllegalStateException}, which this
     * handler used to catch. That was too broad: a blank
     * {@code cloudflare.r2.public-picture-base-url} raises an IllegalStateException too, and an
     * operator hitting it was told the caller was not provisioned and went looking at Keycloak.
     * A configuration fault now falls through to the 500 it deserves.
     */
    @ExceptionHandler(NotProvisionedException.class)
    public ProblemDetail handleNotProvisioned(NotProvisionedException ex, WebRequest request) {
        log.warn("Unresolvable caller on [{}]: {}", request.getDescription(false), ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "Authenticated user is not provisioned", "Not Provisioned");
    }

    /**
     * A database constraint refused the write.
     *
     * <p>Unhandled, this surfaced as a 500 for entirely ordinary situations: deleting a media item
     * that still has reviews, or racing two identical favourites past a check-then-insert into the
     * unique constraint.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Constraint violation on [{}]: {}", request.getDescription(false),
                ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT,
                "The request conflicts with existing data or a related record still references it",
                "Conflict");
    }

    /**
     * An authenticated caller without the required role, raised by {@code @PreAuthorize}.
     *
     * <p>Only covers denials that occur once the dispatcher is running. A denial from the filter
     * chain itself is handled by {@code ExceptionTranslationFilter} and never reaches any
     * {@code @RestControllerAdvice}, so those still return the container's default body.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied on [{}]", request.getDescription(false));
        return problem(HttpStatus.FORBIDDEN, "Access denied", "Forbidden");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        log.warn("Entity not found [{}]: {}", request.getDescription(false), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "Not Found");
    }

    @ExceptionHandler(BreathingNotFoundException.class)
    public ProblemDetail handleBreathingNotFound(BreathingNotFoundException ex, WebRequest request) {
        log.warn("Breathing not found [{}]: {}", request.getDescription(false), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "Breathing Exercise Not Found");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error on [{}]: {}", request.getDescription(false), ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Internal Server Error");
    }

    /**
     * Builds a problem body. {@code instance} is deliberately not set here — Spring MVC fills it
     * with the request path when a {@code ProblemDetail} is returned from a handler.
     */
    private static ProblemDetail problem(HttpStatus status, String detail, String title) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
