package com.library.notification;

import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.Patron;

/**
 * One route by which a patron can be reached.
 *
 * <p>Segregated from the event listener interface on purpose: a channel knows
 * how to deliver a message and nothing about the domain, while a listener knows
 * about the domain and nothing about delivery. Adding a push channel touches
 * neither the events nor the services.</p>
 */
public interface NotificationChannel {

    /**
     * @param patron  who to reach
     * @param subject a short heading
     * @param body    the message
     */
    void send(Patron patron, String subject, String body);

    /**
     * @return the channel this implementation serves
     */
    NotificationChannelType getType();

    /**
     * @param patron the intended recipient
     * @return {@code true} when this patron has the details this channel needs
     */
    default boolean canReach(Patron patron) {
        return patron != null;
    }
}
