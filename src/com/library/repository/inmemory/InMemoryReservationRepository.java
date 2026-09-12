package com.library.repository.inmemory;

import com.library.domain.model.Reservation;
import com.library.repository.ReservationRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory reservation book.
 *
 * <p>The waiting queue is derived rather than stored as a separate structure:
 * sorting waiting reservations by the date they were placed gives first come,
 * first served ordering with no risk of the queue drifting out of step with the
 * reservations themselves.</p>
 */
public class InMemoryReservationRepository extends InMemoryRepository<Reservation, String>
        implements ReservationRepository {

    public InMemoryReservationRepository() {
        super(Reservation::getReservationId);
    }

    @Override
    public List<Reservation> findWaitingQueue(String itemId) {
        return filter(reservation -> reservation.getItemId().equals(itemId)
                && reservation.isWaiting()).stream()
                .sorted(Comparator.comparing(Reservation::getPlacedOn)
                        .thenComparing(Reservation::getReservationId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByPatron(String patronId) {
        return filter(reservation -> reservation.getPatronId().equals(patronId));
    }

    @Override
    public Optional<Reservation> peekQueue(String itemId) {
        List<Reservation> queue = findWaitingQueue(itemId);
        return queue.isEmpty() ? Optional.empty() : Optional.of(queue.get(0));
    }

    @Override
    public boolean hasWaitingReservation(String itemId, String patronId) {
        return firstMatching(reservation -> reservation.getItemId().equals(itemId)
                && reservation.getPatronId().equals(patronId)
                && reservation.isWaiting()).isPresent();
    }
}
