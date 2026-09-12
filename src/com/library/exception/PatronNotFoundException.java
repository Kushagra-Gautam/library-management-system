package com.library.exception;

/**
 * Raised when a patron lookup finds nothing.
 */
public class PatronNotFoundException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public PatronNotFoundException(String patronId) {
        super("No patron is registered with ID '" + patronId + "'.");
    }
}
