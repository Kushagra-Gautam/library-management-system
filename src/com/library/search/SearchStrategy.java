package com.library.search;

import com.library.domain.model.LibraryItem;

import java.util.Collection;
import java.util.List;

/**
 * Strategy pattern: one way of matching catalogue records against a query.
 *
 * <p>This is where the open/closed principle earns its keep. Adding a
 * publisher search, a fuzzy search or a year-range search means writing one new
 * class — no existing class is edited, and the catalogue service is unaware that
 * anything changed.</p>
 */
@FunctionalInterface
public interface SearchStrategy {

    /**
     * @param items the catalogue to search
     * @param query the text the user typed
     * @return matching records, never {@code null}
     */
    List<LibraryItem> search(Collection<LibraryItem> items, String query);

    /**
     * @return a label for menus and logs
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}
