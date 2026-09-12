package com.library.exception;

/**
 * Raised when supplied data fails validation.
 */
public class InvalidInputException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String field, String reason) {
        super("Invalid " + field + ": " + reason);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
