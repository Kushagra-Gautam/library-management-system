package com.library.search;

import com.library.domain.model.LibraryItem;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Matches on genre label.
 */
public class GenreSearchStrategy implements SearchStrategy {

    @Override
    public List<LibraryItem> search(Collection<LibraryItem> items, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        if (needle.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item.getGenre() != null
                        && item.getGenre().getLabel().toLowerCase().contains(needle))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "Genre";
    }
}
