package com.library.repository;

import java.util.List;
import java.util.Optional;

/**
 * The storage contract every repository shares.
 *
 * <p>This is the dependency inversion boundary of the system: services depend on
 * this abstraction, never on a concrete store. Replacing the in-memory maps with
 * JDBC means writing new implementations and changing one line of wiring in
 * {@code LibraryApplication} — no service is touched.</p>
 *
 * @param <T> the entity type held
 * @param <I> the type of that entity's identifier
 */
public interface Repository<T, I> {

    /**
     * @param entity the record to insert or replace
     * @return the stored record
     */
    T save(T entity);

    /**
     * @param id the identifier to look for
     * @return the record, or an empty optional when absent
     */
    Optional<T> findById(I id);

    /**
     * @return every record held, as a copy
     */
    List<T> findAll();

    /**
     * @param id the identifier to remove
     * @return {@code true} when a record was removed
     */
    boolean deleteById(I id);

    /**
     * @param id the identifier to test
     * @return {@code true} when a record with that identifier exists
     */
    boolean existsById(I id);

    /**
     * @return how many records are held
     */
    int count();
}
