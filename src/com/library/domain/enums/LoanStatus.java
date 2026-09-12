package com.library.domain.enums;

/**
 * Lifecycle states of a loan.
 */
public enum LoanStatus {

    ACTIVE("Active"),
    RETURNED("Returned"),
    OVERDUE("Overdue");

    private final String label;

    LoanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
