package com.library.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Produces the human readable identifiers used for patrons, loans, reservations
 * and copies. Counters are {@link AtomicInteger}s so identifiers stay unique
 * even if generation ever happens off more than one thread.
 */
public final class IdGenerator {

    private static final AtomicInteger PATRON_SEQUENCE = new AtomicInteger(0);
    private static final AtomicInteger LOAN_SEQUENCE = new AtomicInteger(0);
    private static final AtomicInteger RESERVATION_SEQUENCE = new AtomicInteger(0);
    private static final AtomicInteger COPY_SEQUENCE = new AtomicInteger(0);

    private IdGenerator() {
    }

    public static String nextPatronId() {
        return String.format("P-%03d", PATRON_SEQUENCE.incrementAndGet());
    }

    public static String nextLoanId() {
        return String.format("L-%03d", LOAN_SEQUENCE.incrementAndGet());
    }

    public static String nextReservationId() {
        return String.format("R-%03d", RESERVATION_SEQUENCE.incrementAndGet());
    }

    public static String nextBarcode() {
        return String.format("BC-%05d", COPY_SEQUENCE.incrementAndGet());
    }

    /**
     * Resets every counter. Used by the test harness so each run starts clean.
     */
    public static void reset() {
        PATRON_SEQUENCE.set(0);
        LOAN_SEQUENCE.set(0);
        RESERVATION_SEQUENCE.set(0);
        COPY_SEQUENCE.set(0);
    }
}
