package ar.ss.betting.rework;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Instant.now(clock).toString(), 400, ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableJson(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Instant.now(clock).toString(), 400, "Malformed JSON request body"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Instant.now(clock).toString(), 400, "Database constraint violated"));
    }

    @ExceptionHandler(java.time.format.DateTimeParseException.class)
    public ResponseEntity<ApiError> handleDateTimeParse(java.time.format.DateTimeParseException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Instant.now(clock).toString(), 400, "Invalid date-time format"));
    }

    /**
     * IMPORTANT: SSE endpoints (text/event-stream) cannot reliably serialize our ApiError as JSON once the
     * response is committed as event-stream. If an exception occurs in that context, return plain text.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex, HttpServletRequest request) {
        if (isSseRequest(request)) {
            // Returning String avoids "no converter for ApiError with text/event-stream"
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("SSE error: " + ex.getClass().getSimpleName() + ": " + safeMsg(ex));
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(Instant.now(clock).toString(), 500,
                        "Internal error: " + ex.getClass().getSimpleName()));
    }

    private boolean isSseRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/event-stream")) return true;

        String uri = request.getRequestURI();
        // Your SSE endpoint is /public/rounds/{id}/live
        return uri != null && uri.startsWith("/public/rounds/") && uri.endsWith("/live");
    }

    private String safeMsg(Exception ex) {
        String m = ex.getMessage();
        return (m == null) ? "" : m;
    }

    public record ApiError(String timestamp, int status, String message) { }
}