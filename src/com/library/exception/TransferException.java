package com.library.exception;

/**
 * Raised when a copy cannot be moved between branches.
 */
public class TransferException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public TransferException(String message) {
        super(message);
    }
}
