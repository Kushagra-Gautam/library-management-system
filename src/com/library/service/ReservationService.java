package com.library.service;

import com.library.domain.model.Reservation;

import java.util.List;
import java.util.Optional;

/**
 * The hold queue for items whose copies are all out.
 */
public interface ReservationService {

    /**
     * @param patronId who wants the item
     * @param itemId   the work wanted
     * @param branchId where they would collect it
     * @return the new reservation
     */
    Reservation reserve(String patronId, String itemId, String branchId);

    /**
     * @param reservationId the reservation to withdraw
     * @return the cancelled reservation
     */
    Reservation cancel(String reservationId);

    /**
     * Allocates a just-returned copy to the patron at the head of the queue and
     * publishes the event that notifies them.
     *
     * @param itemId  the work that came back
     * @param barcode the copy that came back
     * @return the reservation now ready, if anyone was waiting
     */
    Optional<Reservation> fulfilNext(String itemId, String barcode);

    /**
     * @param itemId the work
     * @return everyone waiting for it, oldest first
     */
    List<Reservation> getQueue(String itemId);

    /**
     * @param patronId the member
     * @return every reservation that member has placed
     */
    List<Reservation> getReservationsFor(String patronId);

    /**
     * @param itemId   the work
     * @param patronId the member
     * @return that member's place in the queue, counting from one, or -1
     */
    int getQueuePosition(String itemId, String patronId);
}
