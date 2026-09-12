package com.library.exception;

/**
 * Base type for every domain failure raised by the library.
 *
 * <p>A single root means a caller can catch all library failures with one clause
 * where that is appropriate, and still catch a specific subtype where it is not.
 * All subtypes are unchecked, so business methods are not forced to declare
 * conditions their callers usually cannot repair locally.</p>
 */
public class LibraryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
