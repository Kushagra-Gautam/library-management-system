package com.library.repository.inmemory;

import com.library.domain.model.Patron;
import com.library.repository.PatronRepository;

import java.util.List;
import java.util.Optional;

/**
 * In-memory patron register.
 */
public class InMemoryPatronRepository extends InMemoryRepository<Patron, String>
        implements PatronRepository {

    public InMemoryPatronRepository() {
        super(Patron::getPatronId);
    }

    @Override
    public Optional<Patron> findByEmail(String email) {
        return firstMatching(patron -> patron.getEmail() != null
                && patron.getEmail().equalsIgnoreCase(email));
    }

    @Override
    public List<Patron> findActive() {
        return filter(Patron::isActive);
    }
}
