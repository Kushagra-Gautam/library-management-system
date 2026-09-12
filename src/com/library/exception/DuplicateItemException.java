package com.library.exception;

/**
 * Raised when an identifier that must be unique is reused.
 */
public class DuplicateItemException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public DuplicateItemException(String identifier) {
        super("'" + identifier + "' already exists in the catalogue.");
    }

    public static DuplicateItemException forPatron(String patronId) {
        return new DuplicateItemException("Patron " + patronId);
    }
}
