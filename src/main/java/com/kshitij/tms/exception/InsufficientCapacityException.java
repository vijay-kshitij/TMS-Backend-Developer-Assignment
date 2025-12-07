package com.kshitij.tms.exception;

/**
 * Thrown when a transporter doesn't have enough trucks available.
 * Used during:
 * - Bid submission (transporter must have trucks to offer)
 * - Booking creation (verify capacity before deducting)
 * Returns HTTP 400 Bad Request status.
 */
public class InsufficientCapacityException extends RuntimeException {

    public InsufficientCapacityException(String message) {
        super(message);
    }
}