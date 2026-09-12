package com.library.event;

import com.library.domain.enums.EventType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An immutable record of something that happened in the domain.
 *
 * <p>Events carry identifiers and a small payload rather than live entity
 * references, so a listener can never mutate domain state through the event it
 * was handed.</p>
 */
public final class LibraryEvent {

    private final EventType type;
    private final String subjectId;
    private final String actorId;
    private final String message;
    private final LocalDateTime occurredAt;
    private final Map<String, String> payload;

    public LibraryEvent(EventType type, String subjectId, String actorId, String message) {
        this(type, subjectId, actorId, message, new LinkedHashMap<>());
    }

    public LibraryEvent(EventType type, String subjectId, String actorId, String message,
                        Map<String, String> payload) {
        this.type = type;
        this.subjectId = subjectId;
        this.actorId = actorId;
        this.message = message;
        this.occurredAt = LocalDateTime.now();
        this.payload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
    }

    public EventType getType() {
        return type;
    }

    /**
     * @return what the event is about: an item, a copy or a reservation
     */
    public String getSubjectId() {
        return subjectId;
    }

    /**
     * @return who caused it, usually a patron identifier
     */
    public String getActorId() {
        return actorId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    /**
     * @return extra detail as a read only map
     */
    public Map<String, String> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    public String get(String key) {
        return payload.get(key);
    }

    @Override
    public String toString() {
        return String.format("%s | %-20s | %s", occurredAt.toLocalTime().withNano(0), type, message);
    }
}
