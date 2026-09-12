package com.library.exception;

/**
 * Raised when a catalogue lookup finds nothing.
 */
public class ItemNotFoundException extends LibraryException {

    private static final long serialVersionUID = 1L;

    public ItemNotFoundException(String itemId) {
        super("No catalogue entry exists for '" + itemId + "'.");
    }

    public static ItemNotFoundException forBarcode(String barcode) {
        return new ItemNotFoundException("copy barcode " + barcode);
    }
}
