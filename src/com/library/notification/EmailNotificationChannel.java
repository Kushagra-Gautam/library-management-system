package com.library.notification;

import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.Patron;
import com.library.util.LoggingConfig;
import com.library.util.Validator;

import java.util.logging.Logger;

/**
 * Simulated email delivery. No SMTP is involved — the brief excludes external
 * systems — so the message is logged in the shape an email would take.
 */
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger LOGGER = LoggingConfig.getLogger(EmailNotificationChannel.class);

    @Override
    public void send(Patron patron, String subject, String body) {
        LOGGER.info(() -> String.format("EMAIL to <%s> | subject: %s | %s",
                patron.getEmail(), subject, body));
        System.out.printf("  [EMAIL to %s] %s - %s%n", patron.getEmail(), subject, body);
    }

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public boolean canReach(Patron patron) {
        return patron != null && !Validator.isBlank(patron.getEmail());
    }
}
