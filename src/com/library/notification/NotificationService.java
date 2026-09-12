package com.library.notification;

import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.Patron;
import com.library.util.LoggingConfig;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Routes a message to the channel a patron prefers.
 *
 * <p>Holds channels behind the {@link NotificationChannel} interface and keys
 * them by type, so registering a new route is one map entry. When the preferred
 * channel cannot reach the patron — no phone number on file, for instance — the
 * console channel is used instead rather than the message being lost.</p>
 */
public class NotificationService {

    private static final Logger LOGGER = LoggingConfig.getLogger(NotificationService.class);

    private final Map<NotificationChannelType, NotificationChannel> channels =
            new EnumMap<>(NotificationChannelType.class);
    private final NotificationChannel fallback = new ConsoleNotificationChannel();
    private final List<String> sentLog = new ArrayList<>();

    public NotificationService() {
        register(new ConsoleNotificationChannel());
        register(new EmailNotificationChannel());
        register(new SmsNotificationChannel());
    }

    /**
     * @param channel a delivery route to make available
     */
    public final void register(NotificationChannel channel) {
        channels.put(channel.getType(), channel);
    }

    /**
     * @param patron  the recipient
     * @param subject a short heading
     * @param body    the message
     */
    public void notify(Patron patron, String subject, String body) {
        if (patron == null) {
            return;
        }
        NotificationChannel channel = channels.getOrDefault(patron.getPreferredChannel(), fallback);
        if (!channel.canReach(patron)) {
            LOGGER.fine(() -> "Falling back to console for " + patron.getPatronId()
                    + "; preferred channel cannot reach them");
            channel = fallback;
        }
        channel.send(patron, subject, body);
        sentLog.add(String.format("%s -> %s | %s", patron.getPatronId(), channel.getType(), subject));
    }

    /**
     * @return a record of every message sent this session
     */
    public List<String> getSentLog() {
        return new ArrayList<>(sentLog);
    }

    public int getSentCount() {
        return sentLog.size();
    }
}
