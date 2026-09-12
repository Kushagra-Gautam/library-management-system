package com.library.event;

import com.library.domain.enums.EventType;
import com.library.notification.NotificationService;
import com.library.repository.PatronRepository;

import java.util.EnumSet;
import java.util.Set;

/**
 * Observer that turns domain events into patron notifications.
 *
 * <p>It subscribes only to the events a patron should hear about, so lending and
 * cataloguing never need to know that notifications exist. This is the piece
 * that satisfies the brief's requirement to alert a patron when a reserved book
 * becomes available.</p>
 */
public class NotificationListener implements LibraryEventListener {

    private static final Set<EventType> INTERESTING = EnumSet.of(
            EventType.RESERVATION_READY,
            EventType.RESERVATION_PLACED,
            EventType.BOOK_OVERDUE);

    private final NotificationService notificationService;
    private final PatronRepository patronRepository;

    public NotificationListener(NotificationService notificationService,
                                PatronRepository patronRepository) {
        this.notificationService = notificationService;
        this.patronRepository = patronRepository;
    }

    @Override
    public boolean isInterestedIn(EventType type) {
        return INTERESTING.contains(type);
    }

    @Override
    public void onEvent(LibraryEvent event) {
        if (event.getActorId() == null) {
            return;
        }
        patronRepository.findById(event.getActorId()).ifPresent(patron -> {
            String subject = subjectFor(event);
            notificationService.notify(patron, subject, event.getMessage());
        });
    }

    private String subjectFor(LibraryEvent event) {
        switch (event.getType()) {
            case RESERVATION_READY:
                return "Your reserved title is ready";
            case RESERVATION_PLACED:
                return "Reservation confirmed";
            case BOOK_OVERDUE:
                return "Overdue loan";
            default:
                return "Library update";
        }
    }

    @Override
    public String getListenerName() {
        return "Patron notifier";
    }
}
