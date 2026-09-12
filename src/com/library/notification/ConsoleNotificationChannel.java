package com.library.notification;

import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.Patron;

/**
 * Prints the message to the console. Also the fallback when a patron's preferred
 * channel cannot reach them.
 */
public class ConsoleNotificationChannel implements NotificationChannel {

    @Override
    public void send(Patron patron, String subject, String body) {
        System.out.printf("  [NOTICE to %s] %s - %s%n", patron.getName(), subject, body);
    }

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.CONSOLE;
    }
}
