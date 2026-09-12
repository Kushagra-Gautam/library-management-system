package com.library.service.impl;

import com.library.domain.enums.EventType;
import com.library.domain.enums.MembershipType;
import com.library.domain.enums.NotificationChannelType;
import com.library.domain.model.Patron;
import com.library.event.EventPublisher;
import com.library.event.LibraryEvent;
import com.library.exception.DuplicateItemException;
import com.library.exception.PatronNotFoundException;
import com.library.repository.PatronRepository;
import com.library.service.PatronService;
import com.library.util.IdGenerator;
import com.library.util.LoggingConfig;
import com.library.util.Validator;

import java.util.List;
import java.util.logging.Logger;

/**
 * Default member register implementation.
 */
public class DefaultPatronService implements PatronService {

    private static final Logger LOGGER = LoggingConfig.getLogger(DefaultPatronService.class);

    private final PatronRepository patronRepository;
    private final EventPublisher eventPublisher;

    public DefaultPatronService(PatronRepository patronRepository, EventPublisher eventPublisher) {
        this.patronRepository = patronRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Patron registerPatron(String name, String email) {
        return registerPatron(name, email, "", MembershipType.BASIC, NotificationChannelType.CONSOLE);
    }

    @Override
    public Patron registerPatron(String name, String email, String phone,
                                 MembershipType membershipType, NotificationChannelType channel) {
        String validName = Validator.requireName(name, "patron name");
        String validEmail = Validator.requireEmail(email);
        if (patronRepository.findByEmail(validEmail).isPresent()) {
            LOGGER.warning(() -> "Rejected duplicate registration for " + validEmail);
            throw new DuplicateItemException(validEmail);
        }
        Patron patron = new Patron(IdGenerator.nextPatronId(), validName, validEmail,
                phone == null ? "" : phone.trim(),
                membershipType == null ? MembershipType.BASIC : membershipType,
                channel == null ? NotificationChannelType.CONSOLE : channel);
        patronRepository.save(patron);
        LOGGER.info(() -> "Registered patron " + patron.getPatronId() + " (" + validName + ")");
        eventPublisher.publish(new LibraryEvent(EventType.PATRON_REGISTERED,
                patron.getPatronId(), patron.getPatronId(), "Welcome to the library, " + validName));
        return patron;
    }

    @Override
    public Patron updatePatron(String patronId, String name, String email, String phone) {
        Patron patron = findById(patronId);
        if (!Validator.isBlank(name)) {
            patron.setName(Validator.requireName(name, "patron name"));
        }
        if (!Validator.isBlank(email)) {
            patron.setEmail(Validator.requireEmail(email));
        }
        if (!Validator.isBlank(phone)) {
            patron.setPhone(phone.trim());
        }
        patronRepository.save(patron);
        LOGGER.info(() -> "Updated patron " + patronId);
        return patron;
    }

    @Override
    public Patron findById(String patronId) {
        return patronRepository.findById(Validator.requireText(patronId, "patron id"))
                .orElseThrow(() -> new PatronNotFoundException(patronId));
    }

    @Override
    public List<String> getBorrowingHistory(String patronId) {
        return findById(patronId).getBorrowingHistory();
    }

    @Override
    public Patron setActive(String patronId, boolean active) {
        Patron patron = findById(patronId);
        patron.setActive(active);
        patronRepository.save(patron);
        LOGGER.info(() -> "Patron " + patronId + " is now " + (active ? "active" : "blocked"));
        return patron;
    }

    @Override
    public List<Patron> listAll() {
        return patronRepository.findAll();
    }

    @Override
    public int getPatronCount() {
        return patronRepository.count();
    }
}
