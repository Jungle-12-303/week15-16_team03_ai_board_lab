package com.jungle_choi.namanmu.api;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map((fieldError) -> new FieldErrorResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .toList();
        String message = fieldErrors.isEmpty()
                ? "Request validation failed."
                : fieldErrors.getFirst().message();

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        message,
                        fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage() {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "BAD_REQUEST",
                        "Request body is missing or malformed."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception) {
        HttpStatusCode statusCode = exception.getStatusCode();

        return ResponseEntity.status(statusCode)
                .body(ApiErrorResponse.of(
                        statusCode,
                        statusCodeName(statusCode),
                        statusMessage(statusCode, exception.getReason())));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNoSuchElement() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        "Requested resource was not found."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "Unexpected server error."));
    }

    private static String statusCodeName(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());

        if (status == null) {
            return "HTTP_" + statusCode.value();
        }

        return status.name();
    }

    private static String statusMessage(HttpStatusCode statusCode, String reason) {
        if (reason != null && !reason.isBlank()) {
            return reason;
        }

        HttpStatus status = HttpStatus.resolve(statusCode.value());

        if (status == null) {
            return "HTTP " + statusCode.value();
        }

        return status.getReasonPhrase();
    }

    public record ApiErrorResponse(
            int status,
            String code,
            String message,
            List<FieldErrorResponse> errors) {

        public static ApiErrorResponse of(
                HttpStatusCode statusCode,
                String code,
                String message) {
            return of(statusCode, code, message, List.of());
        }

        public static ApiErrorResponse of(
                HttpStatusCode statusCode,
                String code,
                String message,
                List<FieldErrorResponse> errors) {
            return new ApiErrorResponse(statusCode.value(), code, message, errors);
        }
    }

    public record FieldErrorResponse(String field, String message) {
    }
}
