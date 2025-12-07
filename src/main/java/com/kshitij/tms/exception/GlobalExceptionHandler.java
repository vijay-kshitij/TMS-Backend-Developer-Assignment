package com.kshitij.tms.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire application.
 * Catches exceptions thrown by controllers and services, and returns
 * consistent error responses with appropriate HTTP status codes.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Creates a standard error response map
     */
    private Map<String, Object> createErrorResponse(String message, HttpStatus status, WebRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("path", request.getDescription(false).replace("uri=", ""));
        return error;
    }

    /**
     * Handle ResourceNotFoundException - 404 Not Found
     * Thrown when Load, Bid, Transporter, or Booking is not found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request));
    }

    /**
     * Handle InvalidStatusTransitionException - 400 Bad Request
     * Thrown when trying invalid status changes (e.g., bid on cancelled load)
     */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatusTransition(
            InvalidStatusTransitionException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request));
    }

    /**
     * Handle InsufficientCapacityException - 400 Bad Request
     * Thrown when transporter doesn't have enough trucks
     */
    @ExceptionHandler(InsufficientCapacityException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientCapacity(
            InsufficientCapacityException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request));
    }

    /**
     * Handle LoadAlreadyBookedException - 409 Conflict
     * Thrown when load is already booked or concurrent modification occurs
     */
    @ExceptionHandler(LoadAlreadyBookedException.class)
    public ResponseEntity<Map<String, Object>> handleLoadAlreadyBooked(
            LoadAlreadyBookedException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(createErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, request));
    }

    /**
     * Handle ObjectOptimisticLockingFailureException - 409 Conflict
     * Thrown by JPA when two transactions try to update the same entity simultaneously
     * This is the concurrency control mechanism
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLocking(
            ObjectOptimisticLockingFailureException ex, WebRequest request) {
        String message = "Concurrent modification detected. Another transaction has modified this resource. Please retry your request.";
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(createErrorResponse(message, HttpStatus.CONFLICT, request));
    }

    /**
     * Handle validation errors - 400 Bad Request
     * Thrown when @Valid fails on DTOs (e.g., @NotNull, @Min, @Max violations)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", "Input validation failed. Please check your request.");
        response.put("validationErrors", validationErrors);
        response.put("path", request.getDescription(false).replace("uri=", ""));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle constraint violation - 400 Bad Request
     * Database constraint violations (e.g., unique constraint on accepted bids)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse("Constraint violation: " + ex.getMessage(),
                        HttpStatus.BAD_REQUEST, request));
    }

    /**
     * Handle data integrity violation - 409 Conflict
     * Foreign key violations, unique constraints, etc.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        String message = "Database constraint violation. This might be due to duplicate data or referential integrity issues.";
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(createErrorResponse(message, HttpStatus.CONFLICT, request));
    }

    /**
     * Handle all other exceptions - 500 Internal Server Error
     * Fallback for any unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        String message = "An unexpected error occurred: " + ex.getMessage();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR, request));
    }
}