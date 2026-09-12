package com.library.repository;

import com.library.domain.model.LibraryItem;

import java.util.List;

/**
 * Storage for catalogue records.
 *
 * <p>Declares only the extra query the catalogue genuinely needs. Search itself
 * is deliberately absent: matching rules belong to the search strategies, not to
 * the store, so a new way of searching never changes this interface.</p>
 */
public interface ItemRepository extends Repository<LibraryItem, String> {

    /**
     * @param type the concrete item class wanted
     * @param <T>  that same type
     * @return every catalogue record of that type
     */
    <T extends LibraryItem> List<T> findByType(Class<T> type);
}
