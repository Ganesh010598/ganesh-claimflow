package com.chubb.claims.common;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> notFound(NotFoundException e) { return error(HttpStatus.NOT_FOUND, e.getMessage()); }

    @ExceptionHandler({IllegalStateException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<?> conflict(Exception e) { return error(HttpStatus.CONFLICT, e.getMessage()); }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
                       MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> badRequest(Exception e) { return error(HttpStatus.BAD_REQUEST, "Invalid request or field value"); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "status", status.value(), "error", message == null ? "Request failed" : message,
            "timestamp", Instant.now().toString()));
    }
}
