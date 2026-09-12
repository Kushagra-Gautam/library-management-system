package com.library.notification;

import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.Patron;
import com.library.util.LoggingConfig;
import com.library.util.Validator;

import java.util.logging.Logger;

/**
 * Simulated SMS delivery, truncated to the length a single message allows.
 */
public class SmsNotificationChannel implements NotificationChannel {

    private static final Logger LOGGER = LoggingConfig.getLogger(SmsNotificationChannel.class);
    private static final int MAX_LENGTH = 140;

    @Override
    public void send(Patron patron, String subject, String body) {
        String text = subject + ": " + body;
        String truncated = text.length() <= MAX_LENGTH ? text : text.substring(0, MAX_LENGTH - 3) + "...";
        LOGGER.info(() -> String.format("SMS to %s | %s", patron.getPhone(), truncated));
        System.out.printf("  [SMS to %s] %s%n", patron.getPhone(), truncated);
    }

    @Override
    public NotificationChannelType getType() {
        return NotificationChannelType.SMS;
    }

    @Override
    public boolean canReach(Patron patron) {
        return patron != null && !Validator.isBlank(patron.getPhone());
    }
}
