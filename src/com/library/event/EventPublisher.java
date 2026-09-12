package com.library.event;

import com.library.util.LoggingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The subject half of the observer pattern.
 *
 * <p>Services publish events here without knowing who is listening, which is
 * what keeps lending independent of notifications, auditing and reservation
 * fulfilment. A listener that throws is logged and skipped, so one faulty
 * observer cannot abort the transaction that produced the event.</p>
 */
public class EventPublisher {

    private static final Logger LOGGER = LoggingConfig.getLogger(EventPublisher.class);

    private final List<LibraryEventListener> listeners = new ArrayList<>();
    private final List<LibraryEvent> history = new ArrayList<>();

    /**
     * @param listener an observer to notify from now on
     */
    public void subscribe(LibraryEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            LOGGER.fine(() -> "Subscribed listener: " + listener.getListenerName());
        }
    }

    public void unsubscribe(LibraryEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * @param event the event to broadcast to interested listeners
     */
    public void publish(LibraryEvent event) {
        history.add(event);
        for (LibraryEventListener listener : new ArrayList<>(listeners)) {
            if (!listener.isInterestedIn(event.getType())) {
                continue;
            }
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING,
                        "Listener " + listener.getListenerName() + " failed handling "
                                + event.getType(), e);
            }
        }
    }

    public List<LibraryEventListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * @return every event published, oldest first
     */
    public List<LibraryEvent> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
