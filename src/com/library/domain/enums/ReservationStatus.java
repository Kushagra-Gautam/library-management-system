package com.library.domain.enums;

/**
 * Lifecycle states of a reservation.
 */
public enum ReservationStatus {

    WAITING("Waiting in queue"),
    READY("Ready for collection"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");

    private final String label;

    ReservationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isClosed() {
        return this == FULFILLED || this == CANCELLED || this == EXPIRED;
    }
}
