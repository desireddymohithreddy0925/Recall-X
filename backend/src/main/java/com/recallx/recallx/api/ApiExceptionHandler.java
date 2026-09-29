package com.recallx.recallx.api;

import com.recallx.recallx.common.BadRequestException;
import com.recallx.recallx.common.ConflictException;
import com.recallx.recallx.common.NotFoundException;
import com.recallx.recallx.guard.DiffParseException;
import com.recallx.recallx.memory.hindsight.MemoryUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Clear JSON errors for the frontend: { "error": "...", "detail": "..." }. Runs before any other handler. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler({BadRequestException.class, DiffParseException.class})
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException e) {
        return error(HttpStatus.BAD_REQUEST, "bad_request", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "bad_request", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "bad_request", "The request body is not valid JSON for this endpoint");
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "not_found", e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException e) {
        return error(HttpStatus.CONFLICT, "conflict", e.getMessage());
    }

    @ExceptionHandler(MemoryUnavailableException.class)
    public ResponseEntity<Map<String, Object>> memoryUnavailable(MemoryUnavailableException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "memory_unavailable", e.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("detail", detail == null ? "" : detail);
        return ResponseEntity.status(status).body(body);
    }
}
