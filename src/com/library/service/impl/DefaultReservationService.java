package com.library.service.impl;

import com.library.domain.enums.CopyStatus;
import com.library.domain.enums.EventType;
import com.library.domain.enums.ReservationStatus;
import com.library.domain.model.ItemCopy;
import com.library.domain.model.LibraryItem;
import com.library.domain.model.Patron;
import com.library.domain.model.Reservation;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.ItemNotFoundException;
import com.library.exception.ReservationException;
import com.library.repository.CopyRepository;
import com.library.repository.ItemRepository;
import com.library.repository.ReservationRepository;
import com.library.service.InventoryService;
import com.library.service.PatronService;
import com.library.service.ReservationService;
import com.library.util.IdGenerator;
import com.library.util.LoggingConfig;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Default hold queue implementation.
 *
 * <p>When a copy comes back, {@link #fulfilNext(String, String)} takes the
 * patron at the head of the queue, puts the copy on hold for them so nobody else
 * can borrow it, and publishes {@code RESERVATION_READY}. The notification
 * itself is sent by a listener, which is why this class never imports anything
 * from the notification package.</p>
 */
public class DefaultReservationService implements ReservationService {

    private static final Logger LOGGER = LoggingConfig.getLogger(DefaultReservationService.class);

    private final ReservationRepository reservationRepository;
    private final CopyRepository copyRepository;
    private final ItemRepository itemRepository;
    private final PatronService patronService;
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;

    public DefaultReservationService(ReservationRepository reservationRepository,
                                     CopyRepository copyRepository, ItemRepository itemRepository,
                                     PatronService patronService, InventoryService inventoryService,
                                     EventPublisher eventPublisher) {
        this.reservationRepository = reservationRepository;
        this.copyRepository = copyRepository;
        this.itemRepository = itemRepository;
        this.patronService = patronService;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Reservation reserve(String patronId, String itemId, String branchId) {
        Patron patron = patronService.findById(patronId);
        LibraryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (inventoryService.isAvailable(itemId)) {
            throw new ReservationException("A copy of '" + item.getTitle()
                    + "' is on the shelf; borrow it rather than reserving it.");
        }
        if (reservationRepository.hasWaitingReservation(itemId, patronId)) {
            throw new ReservationException("You are already in the queue for '"
                    + item.getTitle() + "'.");
        }

        Reservation reservation = new Reservation(IdGenerator.nextReservationId(), itemId,
                patronId, branchId);
        reservationRepository.save(reservation);

        int position = getQueuePosition(itemId, patronId);
        LOGGER.info(() -> String.format("Reservation %s: %s queued at position %d for '%s'",
                reservation.getReservationId(), patronId, position, item.getTitle()));

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("position", String.valueOf(position));
        eventPublisher.publish(new LibraryEvent(EventType.RESERVATION_PLACED, itemId,
                patron.getPatronId(),
                String.format("You are number %d in the queue for '%s'", position, item.getTitle()),
                payload));
        return reservation;
    }

    @Override
    public Reservation cancel(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(
                        "No reservation exists with ID " + reservationId));
        if (reservation.getStatus().isClosed()) {
            throw new ReservationException("Reservation " + reservationId + " is already "
                    + reservation.getStatus().getLabel().toLowerCase() + ".");
        }

        if (reservation.getStatus() == ReservationStatus.READY
                && reservation.getHeldBarcode() != null) {
            copyRepository.findById(reservation.getHeldBarcode()).ifPresent(copy -> {
                copy.setStatus(CopyStatus.AVAILABLE);
                copyRepository.save(copy);
            });
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        LOGGER.info(() -> "Cancelled reservation " + reservationId);
        eventPublisher.publish(new LibraryEvent(EventType.RESERVATION_CANCELLED,
                reservation.getItemId(), reservation.getPatronId(), "Reservation cancelled"));
        return reservation;
    }

    @Override
    public Optional<Reservation> fulfilNext(String itemId, String barcode) {
        Optional<Reservation> next = reservationRepository.peekQueue(itemId);
        if (next.isEmpty()) {
            return Optional.empty();
        }
        Reservation reservation = next.get();
        Optional<ItemCopy> copyHolder = copyRepository.findById(barcode);
        if (copyHolder.isEmpty()) {
            return Optional.empty();
        }

        ItemCopy copy = copyHolder.get();
        copy.setStatus(CopyStatus.ON_HOLD);
        copyRepository.save(copy);

        reservation.setStatus(ReservationStatus.READY);
        reservation.setReadyOn(LocalDate.now());
        reservation.setHeldBarcode(barcode);
        reservationRepository.save(reservation);

        String title = itemRepository.findById(itemId).map(LibraryItem::getTitle).orElse(itemId);
        LOGGER.info(() -> String.format("Reservation %s is ready: copy %s of '%s' held for %s",
                reservation.getReservationId(), barcode, title, reservation.getPatronId()));

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("barcode", barcode);
        payload.put("reservationId", reservation.getReservationId());
        eventPublisher.publish(new LibraryEvent(EventType.RESERVATION_READY, itemId,
                reservation.getPatronId(),
                "'" + title + "' is now ready for you to collect", payload));
        return Optional.of(reservation);
    }

    /**
     * Completes a hold by lending the held copy to the patron it was kept for.
     *
     * @param reservationId the reservation being collected
     * @return the barcode of the copy to lend
     */
    public String collect(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(
                        "No reservation exists with ID " + reservationId));
        if (reservation.getStatus() != ReservationStatus.READY) {
            throw new ReservationException("Reservation " + reservationId + " is not ready yet.");
        }
        String barcode = reservation.getHeldBarcode();
        copyRepository.findById(barcode).ifPresent(copy -> {
            copy.setStatus(CopyStatus.AVAILABLE);
            copyRepository.save(copy);
        });
        reservation.setStatus(ReservationStatus.FULFILLED);
        reservationRepository.save(reservation);
        LOGGER.info(() -> "Reservation " + reservationId + " collected");
        return barcode;
    }

    @Override
    public List<Reservation> getQueue(String itemId) {
        return reservationRepository.findWaitingQueue(itemId);
    }

    @Override
    public List<Reservation> getReservationsFor(String patronId) {
        patronService.findById(patronId);
        return reservationRepository.findByPatron(patronId);
    }

    @Override
    public int getQueuePosition(String itemId, String patronId) {
        List<Reservation> queue = reservationRepository.findWaitingQueue(itemId);
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getPatronId().equals(patronId)) {
                return i + 1;
            }
        }
        return -1;
    }
}
