package com.kshitij.tms.exception;

/**
 * Thrown when attempting an invalid status transition.
 * Examples:
 * - Bidding on a CANCELLED or BOOKED load
 * - Cancelling an already BOOKED load
 * Returns HTTP 400 Bad Request status.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}