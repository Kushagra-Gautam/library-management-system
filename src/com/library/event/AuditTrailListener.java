package com.library.event;

import com.library.util.LoggingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Observer that writes every event to the log and keeps an in-memory trail.
 * Registered for all event types, since the point of an audit trail is that
 * nothing is filtered out.
 */
public class AuditTrailListener implements LibraryEventListener {

    private static final Logger LOGGER = LoggingConfig.getLogger(AuditTrailListener.class);

    private final List<String> entries = new ArrayList<>();

    @Override
    public void onEvent(LibraryEvent event) {
        String entry = String.format("%s | %s | subject=%s | actor=%s | %s",
                event.getOccurredAt(), event.getType(), event.getSubjectId(),
                event.getActorId() == null ? "-" : event.getActorId(), event.getMessage());
        entries.add(entry);
        LOGGER.info(() -> "AUDIT " + event.getType() + ": " + event.getMessage());
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    @Override
    public String getListenerName() {
        return "Audit trail";
    }
}
