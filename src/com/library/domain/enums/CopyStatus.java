package com.library.domain.enums;

/**
 * Lifecycle states of a single physical copy.
 */
public enum CopyStatus {

    AVAILABLE("Available"),
    BORROWED("Borrowed"),
    ON_HOLD("On hold for a reservation"),
    IN_TRANSIT("In transit between branches"),
    LOST("Lost");

    private final String label;

    CopyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * @return {@code true} when a patron may borrow this copy right now
     */
    public boolean isLendable() {
        return this == AVAILABLE;
    }
}
