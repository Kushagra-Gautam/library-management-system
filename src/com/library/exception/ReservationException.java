package com.library.exception;

/**
 * Raised when a reservation cannot be placed, cancelled or fulfilled.
 */
public class ReservationException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public ReservationException(String message) {
        super(message);
    }
}
