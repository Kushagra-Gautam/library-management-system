package com.library.event;

import com.library.domain.enums.EventType;

/**
 * Observer pattern: something that reacts to domain events.
 *
 * <p>Listeners declare which event types interest them through
 * {@link #isInterestedIn(EventType)}, so the publisher does no filtering and a
 * listener never receives events it would only discard.</p>
 */
public interface LibraryEventListener {

    /**
     * @param event the event that just occurred
     */
    void onEvent(LibraryEvent event);

    /**
     * @param type the event type being offered
     * @return {@code true} when this listener wants that type
     */
    default boolean isInterestedIn(EventType type) {
        return true;
    }

    /**
     * @return a label used when listing registered listeners
     */
    default String getListenerName() {
        return getClass().getSimpleName();
    }
}
