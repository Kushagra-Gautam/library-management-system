package com.library.repository;

import com.library.domain.model.Patron;

import java.util.List;
import java.util.Optional;

/**
 * Storage for library members.
 */
public interface PatronRepository extends Repository<Patron, String> {

    /**
     * @param email the address to look for
     * @return the patron holding that address, if any
     */
    Optional<Patron> findByEmail(String email);

    /**
     * @return only patrons whose membership is active
     */
    List<Patron> findActive();
}
