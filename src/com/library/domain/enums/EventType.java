package com.library.domain.enums;

/**
 * Kinds of domain event published on the event bus.
 */
public enum EventType {

    PATRON_REGISTERED,
    ITEM_CATALOGUED,
    COPY_ADDED,
    BOOK_BORROWED,
    BOOK_RETURNED,
    BOOK_OVERDUE,
    RESERVATION_PLACED,
    RESERVATION_READY,
    RESERVATION_CANCELLED,
    COPY_TRANSFERRED
}
