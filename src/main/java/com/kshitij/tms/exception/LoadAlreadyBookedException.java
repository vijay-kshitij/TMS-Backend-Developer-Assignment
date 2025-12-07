package com.kshitij.tms.exception;

/**
 * Thrown when attempting to book a load that's already fully booked.
 * Also used for optimistic locking failures (concurrent modifications).
 * Returns HTTP 409 Conflict status.
 */
public class LoadAlreadyBookedException extends RuntimeException {

    public LoadAlreadyBookedException(String message) {
        super(message);
    }
}