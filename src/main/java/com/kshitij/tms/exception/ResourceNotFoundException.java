package com.kshitij.tms.exception;

/**
 * Thrown when a requested resource (Load, Bid, Transporter, Booking) is not found in the database.
 * Returns HTTP 404 Not Found status.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}