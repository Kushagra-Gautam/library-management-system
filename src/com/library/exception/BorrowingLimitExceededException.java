package com.library.exception;

/**
 * Raised when a patron already holds as many loans as their membership allows.
 */
public class BorrowingLimitExceededException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public BorrowingLimitExceededException(String patronId, int limit) {
        super("Patron " + patronId + " already holds the maximum of " + limit + " loans.");
    }
}
