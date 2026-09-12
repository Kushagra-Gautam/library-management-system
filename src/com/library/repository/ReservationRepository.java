package com.library.repository;

import com.library.domain.model.Reservation;

import java.util.List;
import java.util.Optional;

/**
 * Storage for reservations, including the waiting queue per item.
 */
public interface ReservationRepository extends Repository<Reservation, String> {

    /**
     * @param itemId the catalogue key
     * @return reservations still waiting for that item, oldest first
     */
    List<Reservation> findWaitingQueue(String itemId);

    /**
     * @param patronId the member
     * @return every reservation that patron has placed
     */
    List<Reservation> findByPatron(String patronId);

    /**
     * @param itemId the catalogue key
     * @return the reservation at the head of the queue, if any
     */
    Optional<Reservation> peekQueue(String itemId);

    /**
     * @param itemId   the catalogue key
     * @param patronId the member
     * @return {@code true} when that patron is already waiting for that item
     */
    boolean hasWaitingReservation(String itemId, String patronId);
}
