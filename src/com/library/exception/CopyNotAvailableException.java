package com.library.exception;

/**
 * Raised when every copy of an item is out on loan, on hold or in transit.
 */
public class CopyNotAvailableException extends LibraryException {

    private static final long serialVersionUID = 1L;

    private final String itemId;

    public CopyNotAvailableException(String itemId) {
        super("No copy of '" + itemId + "' is available to lend right now.");
        this.itemId = itemId;
    }

    public CopyNotAvailableException(String itemId, String message) {
        super(message);
        this.itemId = itemId;
    }

    public String getItemId() {
        return itemId;
    }
}
